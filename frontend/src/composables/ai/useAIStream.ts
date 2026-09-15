import { ref, computed, nextTick, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import { API_BASE_URL } from '@/config';
import { storage } from '@/core/storage/local';
import { askGlobalAssistant } from '@/api/ai/assistant';
import {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation,
  deleteMessage,
  generateConversationTitle
} from '@/api/ai/chat';
import {
  collectPairedMessageIds,
  isPersistedMessageId,
  removeMessagesByIds
} from '@/utils/ai/chat-message-pair';
import {
  splitCopilotStream,
  cleanReasoningText,
  buildStoppedGenerationContent,
  STOPPED_GENERATION_MARKER
} from '@/utils/ai/copilot-stream-split';
import { useStreamingMarkdown } from '@/composables/ai/useStreamingMarkdown';
import { bindMarkdownCodeCopy } from '@/utils/ai/chat-markdown';
import type { CitationItem } from '@/types/ai/assistant';
import {
  getDefaultReasoningFolded,
  isThinkingPanelHidden
} from '@/utils/ai/thinking-display';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  reasoningContent?: string;
  reasoningFolded?: boolean;
  isReasoningActive?: boolean;
  streamPhaseMessage?: string;
  createdAt: string;
  isStreaming?: boolean;
  citations?: CitationItem[];
  followUpPrompts?: string[];
}

export interface ChatSession {
  id: string;
  title: string;
  updatedAt: string;
}

export interface StreamOptions {
  chapterId?: number;
  modelKey?: string;
  model?: string;
  useRag?: boolean;
  enableThinking?: boolean;
  activeSectionTitle?: string;
  isRegenerate?: boolean;
}

export interface LoadSessionsOptions {
  /** 是否恢复上次会话（localStorage 或列表首条），默认 true */
  restoreLastSession?: boolean;
  /** 仅刷新会话列表，不重载当前会话消息（用于手动停止后保留本地结算消息） */
  skipMessageReload?: boolean;
}

function resolveStreamModelKey(options?: StreamOptions): string | undefined {
  return options?.modelKey || options?.model;
}

function isDefaultSessionTitle(title?: string): boolean {
  if (!title?.trim()) return true;
  return /^(新会话|新问答会话)$/.test(title.trim());
}

const SESSION_STORAGE_PREFIX = 'edumind_course_ai_session:';
const MESSAGE_CACHE_PREFIX = 'edumind_course_ai_messages:';

function messageCacheKey(courseId?: number, conversationId?: string): string {
  if (!courseId || !conversationId) return '';
  return `${MESSAGE_CACHE_PREFIX}${courseId}:${conversationId}`;
}

function persistMessageCache(courseId: number | undefined, conversationId: string | undefined, msgs: ChatMessage[]) {
  const key = messageCacheKey(courseId, conversationId);
  if (!key || msgs.length === 0) return;
  try {
    sessionStorage.setItem(key, JSON.stringify(msgs));
  } catch {
    // ignore quota errors
  }
}

function readMessageCache(courseId?: number, conversationId?: string): ChatMessage[] {
  const key = messageCacheKey(courseId, conversationId);
  if (!key) return [];
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return [];
    return JSON.parse(raw) as ChatMessage[];
  } catch {
    return [];
  }
}

function clearMessageCache(courseId?: number, conversationId?: string) {
  const key = messageCacheKey(courseId, conversationId);
  if (key) sessionStorage.removeItem(key);
}

function isStoppedAssistantMessage(msg: ChatMessage): boolean {
  return msg.role === 'assistant' && !!msg.content?.includes(STOPPED_GENERATION_MARKER);
}

/** 服务端未持久化的「手动停止」回合，合并回消息列表 */
function mergeServerWithLocalDrafts(server: ChatMessage[], local: ChatMessage[]): ChatMessage[] {
  if (!local.length) return server;
  if (!server.length) return local;

  const result = [...server];

  const hasUserContent = (content: string) =>
    result.some((m) => m.role === 'user' && m.content === content);

  const hasAssistantContent = (content: string) =>
    result.some((m) => m.role === 'assistant' && m.content === content);

  for (let i = 0; i < local.length - 1; i++) {
    if (local[i].role !== 'user') continue;
    const draftAssistant = local[i + 1];
    if (!draftAssistant || draftAssistant.role !== 'assistant' || !isStoppedAssistantMessage(draftAssistant)) {
      continue;
    }

    const userContent = local[i].content;
    if (!hasUserContent(userContent)) {
      if (!result.some((m) => m.id === local[i].id)) result.push(local[i]);
      if (!result.some((m) => m.id === draftAssistant.id)) result.push(draftAssistant);
      continue;
    }

    const userIdx = result.findIndex((m) => m.role === 'user' && m.content === userContent);
    const next = result[userIdx + 1];
    if (!next || next.role !== 'assistant') {
      if (!hasAssistantContent(draftAssistant.content)) {
        result.splice(userIdx + 1, 0, draftAssistant);
      }
    }
  }

  // 仅追加服务端尚未收录的用户追问（按 content 去重，避免本地/服务端 id 不同导致重复气泡）
  for (const localMsg of local) {
    if (localMsg.role === 'user') {
      if (hasUserContent(localMsg.content)) continue;
      if (!result.some((m) => m.id === localMsg.id)) {
        result.push(localMsg);
      }
      continue;
    }
    if (
      isStoppedAssistantMessage(localMsg) &&
      !hasAssistantContent(localMsg.content) &&
      !result.some((m) => m.id === localMsg.id)
    ) {
      result.push(localMsg);
    }
  }

  return result;
}

function getStoredSessionId(courseId?: number): string {
  if (!courseId) return '';
  const stored = storage.get(`${SESSION_STORAGE_PREFIX}${courseId}`);
  return typeof stored === 'string' ? stored : '';
}

function storeSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  storage.set(`${SESSION_STORAGE_PREFIX}${courseId}`, sessionId);
}

function clearStoredSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  if (getStoredSessionId(courseId) === sessionId) {
    storage.remove(`${SESSION_STORAGE_PREFIX}${courseId}`);
  }
}

function buildSessionTitleFromPrompt(text: string): string {
  const cleaned = text
    .replace(/^\[当前章节:[^\]]+\]\s*/i, '')
    .replace(/\s+/g, ' ')
    .trim();
  return cleaned.slice(0, 20) || '新问答会话';
}

function formatSessionTime(raw: unknown): string {
  if (!raw) return '刚刚';
  const d = new Date(String(raw));
  if (Number.isNaN(d.getTime())) return String(raw);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return `${d.toLocaleDateString([], { month: '2-digit', day: '2-digit' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function mapCitation(raw: Record<string, unknown>): CitationItem {
  return {
    id: raw.chunkId as number | string | undefined,
    docTitle: (raw.documentName as string) || (raw.docTitle as string),
    documentName: raw.documentName as string,
    page: raw.pageNo as number | undefined,
    pageNo: raw.pageNo as number | undefined,
    score: raw.score as number | undefined,
    snippet: (raw.excerpt as string) || (raw.snippet as string),
    excerpt: raw.excerpt as string
  };
}

function generateSmartFollowUps(query: string): string[] {
  // 1. 去除系统注入的前缀，例如「[当前章节: 1.1 算法复杂度与渐进表示法]」或「[知识点: xxx]」
  const cleanQuery = query.replace(/^\[[^\]]+\]\s*/g, '').trim().toLowerCase();
  const q = cleanQuery || query.toLowerCase();

  // 2. 优先判定：出题、考试、做题、作业、考点、数学、数列等教学与试题场景
  if (/出题|题目|试题|试卷|考题|做题|作业|考点|数列|函数解析|数学|难题|随堂|真题|选择题|填空题|大题|解答题|难度系数/.test(q)) {
    return [
      '根据此题型衍生 3 道同等难度的变式训练题',
      '导出试题解析、踩分点与评分细则标准',
      '分析这道题目考察的底层核心知识点与易错陷阱'
    ];
  }

  // 3. 明确的编程开发与代码编写场景（避免被章节名中的宽泛“算法”误伤）
  if (/写代码|代码实现|写一个函数|写个类|报错|bug|debug|堆栈|异常处理|spring boot|vue|typescript|sql查询/.test(q) || (/代码|编程/.test(q) && /写|看|改|实现|重构/.test(q))) {
    return [
      '给出此逻辑的完整工程优化版代码实现',
      '分析这段代码可能存在的边界隐患与异常处理',
      '结合设计模式重构此核心业务逻辑'
    ];
  }

  // 4. 概念与原理辨析
  if (/概念|原理|是什么|区别|体系|架构|为什么/.test(q)) {
    return [
      '用通俗生动的比喻进一步解释底层原理',
      '用对比表格列出它与关联知识的关键异同',
      '结合当前课程知识大纲展开前驱依赖与后继拓展'
    ];
  }

  // 5. 算法与数据结构理论
  if (/算法|数据结构|复杂度|时间复杂度|空间复杂度|渐进表示|递归|动态规划|贪心/.test(q)) {
    return [
      '分析该算法在最好、最坏与平均情况下的时空复杂度',
      '用简明步骤图解该算法的执行推演过程',
      '对比该算法与其他替代算法的性能优缺点'
    ];
  }

  // 6. 默认智能推荐
  return [
    '用更通俗生动的教学案例进一步解释',
    '为该考点出 2 道课堂即兴互动提问',
    '结合实际工程案例列举具体应用场景'
  ];
}

export function useAIStream() {
  const sseClient = new SSEClient();
  const streaming = ref(false);
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<string>('');
  const messages = ref<ChatMessage[]>([]);
  let currentStreamId = '';

  // 滚动容器与物理底端锚点 (用于 rAF 平滑向上跟随贴底)
  const messagesScrollRef = ref<HTMLDivElement | null>(null);
  const streamAnchorRef = ref<HTMLDivElement | null>(null);

  const getInitialReasoningFolded = getDefaultReasoningFolded;

  // 流式过程中的临时状态 (对标全局副驾驶独立顶级 ref)
  const streamingReasoning = ref('');
  const streamingContent = ref('');
  const streamingCitations = ref<CitationItem[]>([]);
  const isReasoningFolded = ref(getInitialReasoningFolded());
  const isReasoningActive = ref(false);
  const streamPhaseMessage = ref('');
  const answerStreamStarted = ref(false);
  const followUpPrompts = ref<string[]>([]);
  /** 用户手动点击停止后置位，避免 fallback / 重载消息覆盖已结算内容 */
  const userStoppedGeneration = ref(false);
  const activeStreamCourseId = ref<number | undefined>();

  // 增量高效流式 Markdown 渲染引擎
  const {
    renderedHtml: streamingRenderedHtml,
    appendChunk: appendStreamingMarkdown,
    finish: finishStreamingMarkdown,
    reset: resetStreamingMarkdown
  } = useStreamingMarkdown();

  const streamingSplit = computed(() => splitCopilotStream(streamingContent.value));
  const streamingThinkingBody = computed(() => streamingSplit.value.thinking);
  const streamingAnswerBody = computed(() => streamingSplit.value.answer);
  const streamingThinkingDisplay = computed(() => {
    const raw = streamingReasoning.value || streamingThinkingBody.value;
    return cleanReasoningText(raw);
  });

  const showScrollToBottom = ref(false);
  const userScrolledUp = ref(false);
  let followAnimationFrameId: number | null = null;

  function pauseAutoScrollFollow() {
    userScrolledUp.value = true;
    showScrollToBottom.value = true;
  }

  /** 流式输出时自动平滑向上滚动贴底；用户手动上滑后暂停贴底（与侧边栏 AI 一致） */
  function scheduleFollowStreamOutput() {
    if (!streaming.value || userScrolledUp.value || showScrollToBottom.value) return;
    if (followAnimationFrameId) return;
    followAnimationFrameId = requestAnimationFrame(() => {
      followAnimationFrameId = null;
      nextTick(() => {
        if (!streaming.value || userScrolledUp.value || showScrollToBottom.value) return;
        if (messagesScrollRef.value) {
          messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
          bindMarkdownCodeCopy(messagesScrollRef.value);
        }
      });
    });
  }

  function handleViewportScroll(e: Event) {
    const el = e.target as HTMLElement;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    if (distanceToBottom > 160) {
      userScrolledUp.value = true;
      showScrollToBottom.value = true;
    } else if (distanceToBottom < 30) {
      userScrolledUp.value = false;
      showScrollToBottom.value = false;
    }
  }

  function scrollToBottomInstant() {
    nextTick(() => {
      if (messagesScrollRef.value) {
        messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
      }
    });
  }

  function scrollToBottomSmooth() {
    userScrolledUp.value = false;
    showScrollToBottom.value = false;
    nextTick(() => {
      if (streamAnchorRef.value) {
        streamAnchorRef.value.scrollIntoView({ behavior: 'smooth', block: 'end' });
      } else if (messagesScrollRef.value) {
        messagesScrollRef.value.scrollTo({
          top: messagesScrollRef.value.scrollHeight,
          behavior: 'smooth'
        });
      }
    });
  }

  watch(
    () => streamingRenderedHtml.value,
    () => {
      if (streaming.value) {
        scheduleFollowStreamOutput();
      }
    }
  );

  watch(
    () => streamingReasoning.value,
    () => {
      if (streaming.value) {
        scheduleFollowStreamOutput();
      }
    }
  );

  function resetStreamingState() {
    streamingReasoning.value = '';
    streamingContent.value = '';
    streamingCitations.value = [];
    isReasoningFolded.value = getInitialReasoningFolded();
    isReasoningActive.value = false;
    streamPhaseMessage.value = '';
    answerStreamStarted.value = false;
    resetStreamingMarkdown();
  }

  function findFirstUserPrompt(source: ChatMessage[] = messages.value): string {
    return source.find((m) => m.role === 'user' && m.content?.trim())?.content?.trim() || '';
  }

  /** 默认标题会话：用首条用户提问重命名，并持久化到后端 */
  async function syncSessionTitleIfDefault(
    conversationId: string,
    promptHint?: string,
    options?: { tryLlmTitle?: boolean }
  ): Promise<void> {
    if (!conversationId) return;
    const sess = sessions.value.find((s) => s.id === conversationId);
    if (!sess || !isDefaultSessionTitle(sess.title)) return;

    const prompt = promptHint || findFirstUserPrompt();
    if (!prompt) return;

    const fallbackTitle = buildSessionTitleFromPrompt(prompt);
    if (isDefaultSessionTitle(fallbackTitle)) return;

    sess.title = fallbackTitle;
    try {
      await renameConversation(conversationId, fallbackTitle);
    } catch {
      // 保留本地标题，避免列表闪回默认名
    }

    if (!options?.tryLlmTitle) return;
    void generateConversationTitle(conversationId)
      .then((res) => {
        const aiTitle = String(res?.data || '').trim();
        if (aiTitle && !isDefaultSessionTitle(aiTitle)) {
          sess.title = aiTitle;
        }
      })
      .catch(() => {});
  }

  function finalizeAssistantMessage(
    promptText: string,
    serverIds?: { userMessageId?: string; messageId?: string }
  ) {
    finishStreamingMarkdown();
    const finalReasoning = cleanReasoningText(
      streamingReasoning.value || streamingThinkingBody.value
    );
    const finalAnswer = streamingAnswerBody.value || streamingContent.value;

    if (serverIds?.userMessageId) {
      for (let i = messages.value.length - 1; i >= 0; i--) {
        if (messages.value[i]?.role === 'user') {
          messages.value[i].id = serverIds.userMessageId;
          break;
        }
      }
    }

    messages.value.push({
      id: serverIds?.messageId || `ai_${Date.now()}`,
      role: 'assistant',
      content: finalAnswer.trim() || '已处理你的课程学习咨询。',
      reasoningContent: finalReasoning,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations: [...streamingCitations.value],
      followUpPrompts: generateSmartFollowUps(promptText)
    });

    followUpPrompts.value = generateSmartFollowUps(promptText);
    resetStreamingState();
    persistMessageCache(activeStreamCourseId.value, currentSessionId.value, messages.value);
    window.dispatchEvent(new CustomEvent('edumind:ai-usage-changed'));
    scrollToBottomInstant();
    nextTick(() => {
      if (messagesScrollRef.value) {
        bindMarkdownCodeCopy(messagesScrollRef.value);
      }
    });
  }

  function mapSession(raw: any): ChatSession {
    return {
      id: String(raw?.id || `sess_${Date.now()}`),
      title: (raw?.title as string) || '新会话',
      updatedAt: formatSessionTime(raw?.updateTime || raw?.updatedAt || raw?.createTime)
    };
  }

  function mapMessage(raw: any): ChatMessage {
    const citations = Array.isArray(raw?.citations)
      ? (raw.citations as any[]).map(mapCitation)
      : undefined;
    return {
      id: (raw.id as string) || `msg_${Date.now()}`,
      role: (raw.role || 'assistant') as ChatMessage['role'],
      content: (raw.content as string) || '',
      reasoningContent: (raw.reasoningContent as string) || (raw.reasoning_content as string) || '',
      createdAt: raw.createTime
        ? new Date(raw.createTime as string).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations
    };
  }

  async function refreshSessionsMeta(courseId?: number) {
    const preservedTitles = new Map(
      sessions.value
        .filter((s) => !isDefaultSessionTitle(s.title))
        .map((s) => [s.id, s.title] as const)
    );
    try {
      const res = await getConversations(courseId);
      sessions.value = (res.data || []).map((item) => {
        const mapped = mapSession(item as unknown as Record<string, unknown>);
        const preserved = preservedTitles.get(mapped.id);
        if (preserved && isDefaultSessionTitle(mapped.title)) {
          mapped.title = preserved;
        }
        return mapped;
      });
      if (
        currentSessionId.value &&
        !sessions.value.some((s) => s.id === currentSessionId.value)
      ) {
        const firstUser = messages.value.find((m) => m.role === 'user');
        sessions.value.unshift({
          id: currentSessionId.value,
          title: firstUser?.content
            ? buildSessionTitleFromPrompt(firstUser.content)
            : '新问答会话',
          updatedAt: '刚刚'
        });
      }
    } catch {
      // 保留本地会话列表，避免手动停止后左侧列表闪空
    }
  }

  async function loadSessions(courseId?: number, options?: LoadSessionsOptions) {
    const restoreLastSession = options?.restoreLastSession !== false;
    const preservedTitles = new Map(
      sessions.value
        .filter((s) => !isDefaultSessionTitle(s.title))
        .map((s) => [s.id, s.title] as const)
    );
    try {
      const res = await getConversations(courseId);
      sessions.value = (res.data || []).map((item) => {
        const mapped = mapSession(item as unknown as Record<string, unknown>);
        const preserved = preservedTitles.get(mapped.id);
        if (preserved && isDefaultSessionTitle(mapped.title)) {
          mapped.title = preserved;
        }
        return mapped;
      });

      if (!restoreLastSession) {
        currentSessionId.value = '';
        messages.value = [];
        followUpPrompts.value = [];
        return;
      }

      const storedId = getStoredSessionId(courseId);
      const preferredId =
        (storedId && sessions.value.some((s) => s.id === storedId) ? storedId : '') ||
        (currentSessionId.value && sessions.value.some((s) => s.id === currentSessionId.value)
          ? currentSessionId.value
          : '') ||
        sessions.value[0]?.id ||
        '';

      if (preferredId) {
        currentSessionId.value = preferredId;
        storeSessionId(courseId, preferredId);
        if (!options?.skipMessageReload) {
          await loadMessages(preferredId, courseId);
        }
      } else if (!options?.skipMessageReload) {
        currentSessionId.value = '';
        messages.value = [];
      }
    } catch {
      if (sessions.value.length === 0) {
        currentSessionId.value = '';
        messages.value = [];
      }
    }
  }

  function restoreFollowUpsForLastTurn() {
    if (messages.value.length === 0) {
      followUpPrompts.value = [];
      return;
    }
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg && lastMsg.role === 'assistant') {
      if (lastMsg.followUpPrompts && lastMsg.followUpPrompts.length > 0) {
        followUpPrompts.value = [...lastMsg.followUpPrompts];
        return;
      }
      const lastUserMsg = [...messages.value].reverse().find((m) => m.role === 'user');
      if (lastUserMsg?.content) {
        const prompts = generateSmartFollowUps(lastUserMsg.content);
        followUpPrompts.value = prompts;
        lastMsg.followUpPrompts = prompts;
      }
    } else {
      followUpPrompts.value = [];
    }
  }

  async function loadMessages(conversationId: string, courseId?: number) {
    const localDraft =
      messages.value.length > 0 && currentSessionId.value === conversationId
        ? [...messages.value]
        : readMessageCache(courseId, conversationId);
    try {
      const res = await getMessages(conversationId);
      const serverMsgs = (res.data || []).map((item) => mapMessage(item));
      messages.value = mergeServerWithLocalDrafts(serverMsgs, localDraft);
      persistMessageCache(courseId, conversationId, messages.value);
      await syncSessionTitleIfDefault(conversationId);
      restoreFollowUpsForLastTurn();
    } catch {
      if (localDraft.length > 0) {
        messages.value = localDraft;
      } else {
        messages.value = [];
        followUpPrompts.value = [];
      }
    }
  }

  async function streamAssistantChat(query: string, courseId: number, options?: StreamOptions): Promise<boolean> {
    let receivedContent = false;
    let lastStreamedIndex = 0;

    await sseClient.streamEvents(
      `${API_BASE_URL}/ai/chat/stream`,
      {
        message: query,
        courseId,
        chapterId: options?.chapterId,
        conversationId: currentSessionId.value || undefined,
        modelKey: resolveStreamModelKey(options),
        useRag: options?.useRag ?? true,
        regenerate: options?.isRegenerate === true
      },
      (event, data) => {
        if (userStoppedGeneration.value) return;
        if (event === 'status') {
          const s = data as { phase?: string; message?: string };
          if (s?.message) streamPhaseMessage.value = s.message;
          if (s?.phase === 'reasoning') isReasoningActive.value = true;
          if (s?.phase === 'composing') isReasoningActive.value = false;
        } else if (event === 'reasoning') {
          const chunk = String((data as { content?: string })?.content || data || '');
          if (chunk) {
            streamingReasoning.value += chunk;
            isReasoningActive.value = true;
            streamPhaseMessage.value = '';
          }
        } else if (event === 'delta') {
          const chunk = String((data as { content?: string; text?: string })?.content || (data as { text?: string })?.text || data || '');
          if (chunk) {
            receivedContent = true;
            streamingContent.value += chunk;
            const split = splitCopilotStream(streamingContent.value);
            if (split.answer) {
              isReasoningActive.value = false;
              streamPhaseMessage.value = '';
              if (!answerStreamStarted.value) {
                answerStreamStarted.value = true;
                isReasoningFolded.value = getInitialReasoningFolded();
              }
              const answerDelta = split.answer.slice(lastStreamedIndex);
              lastStreamedIndex = split.answer.length;
              if (answerDelta) {
                appendStreamingMarkdown(answerDelta);
              }
            }
          }
        } else if (event === 'citation' || event === 'citations') {
          const cits = (Array.isArray(data) ? data : (data as { citations?: CitationItem[] })?.citations) || [];
          if (Array.isArray(cits) && cits.length > 0) {
            streamingCitations.value = cits as CitationItem[];
          }
        } else if (event === 'done') {
          const d = data as {
            conversationId?: string;
            messageId?: string;
            userMessageId?: string;
            citations?: CitationItem[];
            reasoningContent?: string;
          };
          if (d.conversationId) {
            const convId = String(d.conversationId);
            if (convId !== currentSessionId.value) {
              currentSessionId.value = convId;
            }
            storeSessionId(courseId, convId);
            if (!sessions.value.some((s) => s.id === convId)) {
              sessions.value.unshift({
                id: convId,
                title: query.slice(0, 20).replace(/\n/g, ' ').trim() || '新问答会话',
                updatedAt: '刚刚'
              });
            }
          }
          if (d.citations && d.citations.length > 0) {
            streamingCitations.value = d.citations;
          }
          if (d.reasoningContent && !streamingReasoning.value) {
            streamingReasoning.value = String(d.reasoningContent);
          }
          finalizeAssistantMessage(query, {
            userMessageId: d.userMessageId,
            messageId: d.messageId
          });
        } else if (event === 'error') {
          throw new Error(String((data as { message?: string })?.message || '流式输出服务异常'));
        }
        scheduleFollowStreamOutput();
      }
    );

    return receivedContent;
  }

  async function fallbackAsk(query: string, courseId: number) {
    const res = await askGlobalAssistant({
      message: query,
      courseId,
      conversationId: currentSessionId.value
    });
    const data = res?.data;
    if (!data) throw new Error('AI 服务暂未返回有效数据');
    if (data.reasoningContent) {
      streamingReasoning.value = data.reasoningContent;
    }
    if (data.citations && Array.isArray(data.citations)) {
      streamingCitations.value = data.citations as CitationItem[];
    }
    streamingContent.value = data.content || '已为你检索并梳理该课程要点。';
    finalizeAssistantMessage(query);
  }

  async function sendMessage(promptText: string, courseId: number = 101, options?: StreamOptions) {
    const text = promptText.trim();
    if (!text || streaming.value) return;

    activeStreamCourseId.value = courseId;

    if (!currentSessionId.value) {
      await createNewSession(courseId, true);
    }

    // 1. 若非重新生成，将用户提问上屏；若为重新生成，列表里已保留该提问，避免重复追加相同提问气泡
    if (!options?.isRegenerate) {
      const userMsg: ChatMessage = {
        id: `user_${Date.now()}`,
        role: 'user',
        content: text,
        createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      messages.value.push(userMsg);
    }

    // 2. 激活流式状态与阶段提示
    followUpPrompts.value = [];
    resetStreamingState();
    streamPhaseMessage.value = '正在检索课程知识切片并深度思考...';
    streaming.value = true;
    scrollToBottomInstant();

    // 乐观更新列表标题，避免等待接口期间仍显示「新问答会话」
    const curSess = sessions.value.find((s) => s.id === currentSessionId.value);
    if (curSess && isDefaultSessionTitle(curSess.title)) {
      const previewTitle = buildSessionTitleFromPrompt(text);
      if (!isDefaultSessionTitle(previewTitle)) {
        curSess.title = previewTitle;
      }
    }

    try {
      const streamed = await streamAssistantChat(text, courseId, options);
      if (userStoppedGeneration.value) return;
      if (!streamed && !streamingContent.value) {
        if (options?.isRegenerate) {
          throw new Error('重新生成未返回有效内容，请稍后重试');
        }
        await fallbackAsk(text, courseId);
      }
    } catch (err: unknown) {
      if (userStoppedGeneration.value) return;
      if (options?.isRegenerate) {
        const errorMsg = err instanceof Error ? err.message : '服务繁忙，请稍后重试';
        messages.value.push({
          id: `ai_err_${Date.now()}`,
          role: 'assistant',
          content: `抱歉，重新生成失败：${errorMsg}`,
          createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        });
        resetStreamingState();
      } else {
        try {
          await fallbackAsk(text, courseId);
        } catch (fallbackErr: unknown) {
          const errorMsg = fallbackErr instanceof Error ? fallbackErr.message : (err instanceof Error ? err.message : '服务繁忙，请稍后重试');
          messages.value.push({
            id: `ai_err_${Date.now()}`,
            role: 'assistant',
            content: `抱歉，知识库研读解析遇到异常：${errorMsg}`,
            createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          });
          resetStreamingState();
        }
      }
    } finally {
      const wasStopped = userStoppedGeneration.value;
      userStoppedGeneration.value = false;
      streaming.value = false;
      scrollToBottomInstant();
      if (currentSessionId.value) {
        storeSessionId(courseId, currentSessionId.value);
        if (wasStopped) {
          persistMessageCache(courseId, currentSessionId.value, messages.value);
          await refreshSessionsMeta(courseId);
        } else {
          await syncSessionTitleIfDefault(currentSessionId.value, text, { tryLlmTitle: true });
          await loadSessions(courseId);
          persistMessageCache(courseId, currentSessionId.value, messages.value);
        }
      }
      nextTick(() => {
        if (messagesScrollRef.value) {
          bindMarkdownCodeCopy(messagesScrollRef.value);
        }
      });
    }
  }

  function stopStream() {
    if (!streaming.value) return;
    userStoppedGeneration.value = true;
    sseClient.stop();
    streaming.value = false;

    const answer = streamingAnswerBody.value || streamingContent.value;
    finishStreamingMarkdown();
    messages.value.push({
      id: `ai_${Date.now()}`,
      role: 'assistant',
      content: buildStoppedGenerationContent(answer),
      reasoningContent: cleanReasoningText(streamingReasoning.value || streamingThinkingBody.value),
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations: [...streamingCitations.value]
    });
    resetStreamingState();
    persistMessageCache(activeStreamCourseId.value, currentSessionId.value, messages.value);
    ElMessage.info('已停止当前生成');
    scrollToBottomInstant();
  }

  async function startNewChat(courseId?: number) {
    await createNewSession(courseId, true);
  }

  async function createNewSession(courseId?: number, force = false) {
    if (!force && messages.value.length === 0 && !streaming.value) {
      return;
    }
    try {
      const res = await createConversation({ courseId, title: '新问答会话' });
      const session = mapSession((res.data || {}) as unknown as Record<string, unknown>);
      sessions.value.unshift(session);
      currentSessionId.value = session.id;
      storeSessionId(courseId, session.id);
      messages.value = [];
      followUpPrompts.value = [];
    } catch {
      ElMessage.error('创建会话失败，请确认已登录且网络正常');
      throw new Error('create conversation failed');
    }
  }

  async function switchSession(id: string, courseId?: number) {
    if (streaming.value) {
      ElMessage.warning('当前正在生成中，请先停止生成');
      return;
    }
    currentSessionId.value = id;
    storeSessionId(courseId, id);
    followUpPrompts.value = [];
    resetStreamingState();
    await loadMessages(id, courseId);
    scrollToBottomInstant();
  }

  async function removeSession(id: string, courseId?: number) {
    try {
      await deleteConversation(id);
    } catch {
      // ignore
    }
    sessions.value = sessions.value.filter((s) => s.id !== id);
    clearStoredSessionId(courseId, id);
    clearMessageCache(courseId, id);
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value[0]?.id || '';
      if (currentSessionId.value) {
        storeSessionId(courseId, currentSessionId.value);
        await loadMessages(currentSessionId.value, courseId);
      } else {
        messages.value = [];
      }
    }
  }

  async function renameSession(id: string, title: string) {
    try {
      await renameConversation(id, title);
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    } catch {
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    }
  }

  function handleRegenerate(targetIdx: number, courseId: number = 101, options?: StreamOptions) {
    if (streaming.value) return;
    let userPrompt = '';
    for (let i = targetIdx; i >= 0; i--) {
      if (messages.value[i]?.role === 'user') {
        userPrompt = messages.value[i].content;
        break;
      }
    }
    if (!userPrompt) return;
    // 移除 targetIdx 及其后续的消息，使得末尾保留该轮用户的原有提问
    messages.value.splice(targetIdx);
    sendMessage(userPrompt, courseId, { ...options, isRegenerate: true });
  }

  async function confirmDeleteMessage(targetIdx: number) {
    try {
      await ElMessageBox.confirm(
        '将删除本条及其对应的一问一答，删除后无法恢复。确定继续吗？',
        '删除对话记录',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          lockScroll: false
        }
      );
    } catch {
      return;
    }

    const idsToDelete = collectPairedMessageIds(messages.value, targetIdx);
    if (!idsToDelete.length) return;

    const persistedId = idsToDelete.find((id) => isPersistedMessageId(id));
    try {
      let deletedIds = idsToDelete;
      if (persistedId) {
        const res = await deleteMessage(persistedId);
        if (res?.data?.deletedIds?.length) {
          deletedIds = res.data.deletedIds;
        }
      }
      messages.value = removeMessagesByIds(messages.value, deletedIds);
      persistMessageCache(activeStreamCourseId.value, currentSessionId.value, messages.value);
      followUpPrompts.value = [];
      ElMessage.success('已删除本轮对话记录');
    } catch {
      if (!persistedId) {
        messages.value = removeMessagesByIds(messages.value, idsToDelete);
        followUpPrompts.value = [];
        ElMessage.success('已删除本轮对话记录');
        return;
      }
      ElMessage.error('删除失败，请稍后重试');
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    streaming,
    messagesScrollRef,
    streamAnchorRef,
    // 流式状态
    streamingReasoning,
    streamingContent,
    streamingCitations,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    followUpPrompts,
    showThinkingPanel: computed(() => !isThinkingPanelHidden()),
    // 方法
    loadSessions,
    loadMessages,
    sendMessage,
    stopStream,
    createNewSession,
    startNewChat,
    switchSession,
    deleteSession: removeSession,
    renameSession,
    handleRegenerate,
    confirmDeleteMessage,
    scrollToBottomSmooth,
    scrollToBottomInstant,
    scheduleFollowStreamOutput,
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll
  };
}
