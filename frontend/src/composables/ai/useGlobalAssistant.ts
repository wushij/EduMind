import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import { streamGlobalAssistantChat } from '@/services/ai/copilot-sse-stream';
import { useAIStreamScrollFollow } from '@/composables/ai/useAIStreamScrollFollow';
import { askGlobalAssistant } from '@/api/ai/assistant';
import { cancelChatStream } from '@/api/ai/chat';
import {
  getMessages,
  deleteConversation,
  deleteMessage
} from '@/api/ai/chat';
import {
  collectPairedMessageIds,
  isPersistedMessageId,
  removeMessagesByIds
} from '@/utils/ai/chat-message-pair';
import { storage } from '@/core/storage/local';
import {
  splitCopilotStream,
  cleanReasoningText,
  buildStoppedGenerationContent
} from '@/utils/ai/copilot-stream-split';
import { useStreamingMarkdown } from '@/composables/ai/useStreamingMarkdown';
import { bindMarkdownCodeCopy } from '@/utils/ai/chat-markdown';
import { formatCitationMatchLabel } from '@/utils/ai/citation-score';
import { requestFollowUps, generateRemoteSessionTitle } from '@/services/ai/stream-service';
import { pickTurnMessage } from '@/utils/ai/follow-up-message';
import type {
  CitationItem,
  GlobalAssistantChatRequest,
  GlobalAssistantIntentEvent,
  GlobalAssistantMessage,
  GlobalAssistantSession
} from '@/types/ai/assistant';
import { useTeachingCopilotStore, OPEN_GLOBAL_ASSISTANT_EVENT } from '@/stores/ai/teaching-copilot-context';
import { buildTeachingContextRequestPayload } from '@/types/ai/teaching-copilot-context';
import { useAuthStore } from '@/stores/auth/auth';
import {
  getDefaultReasoningFolded,
  isThinkingPanelHidden
} from '@/utils/ai/thinking-display';


const INTENT_DESC_MAP: Record<string, string> = {
  agent: '智能体任务编排',
  rag: '知识库检索',
  navigate: '功能页面直达',
  chat: '课程助教答疑'
};

function resolveTargetPath(intent?: string, agentCode?: string): string | undefined {
  if (!agentCode) return undefined;
  if (agentCode.startsWith('/')) return agentCode;
  if (agentCode === 'exam' || intent === 'EXAM_COMPOSE') return '/ai/exam/generate';
  if (agentCode === 'learning') return '/analytics/knowledge-mastery';
  if (agentCode === 'kb_retrieval') return '/knowledge/retrieval';
  return undefined;
}

function resolveIntentDesc(intent?: string, agentCode?: string): string {
  const code = (agentCode || '').toLowerCase();
  if (code === 'exam' || code === 'question' || intent === 'EXAM_COMPOSE') return '识别意图：AI 智能组卷与出题';
  if (code === 'tutor' || code === 'question_tutor') return '识别意图：题目答疑辅导';
  if (code === 'platform') return '识别意图：平台能力咨询';
  if (code === 'teaching') return '识别意图：备课教学建议';
  if (code === 'grading') return '识别意图：作业智能批改';
  if (code === 'learning') return '识别意图：学情诊断分析';
  if (intent === 'rag') return '识别意图：知识库考点检索';
  if (intent === 'navigate') return '识别意图：页面功能直达';
  if (intent && INTENT_DESC_MAP[intent]) return `识别意图：${INTENT_DESC_MAP[intent]}`;
  return agentCode || intent || '智能助手';
}

const SESSION_STORAGE_PREFIX = 'edumind_global_assistant_session:';

function resolveSessionStorageKey(
  courseId?: number,
  lessonChapterId?: number,
  userId?: number
): string {
  if (courseId && lessonChapterId && userId) {
    return `edumind_lesson_copilot_session_${userId}_${courseId}_${lessonChapterId}`;
  }
  return `${SESSION_STORAGE_PREFIX}${courseId ?? 'global'}`;
}

/** 兜底标题：与课程 AI 口径一致，去掉装饰括号并压到 14 字（历史列表里 20/36 字太长） */
function buildSessionTitleFromPrompt(text: string): string {
  const cleaned = text
    .replace(/[「」『』“”"'《》【】]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return cleaned.slice(0, 14) || '新问答会话';
}

function formatSessionTime(raw: unknown): string {
  if (!raw) return '刚刚';
  if (typeof raw === 'number') {
    const diff = Math.floor((Date.now() - raw) / 1000);
    if (diff < 60) return '刚刚';
    if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`;
    if (diff < 172800) return '昨天';
  }
  const d = new Date(String(raw));
  if (Number.isNaN(d.getTime())) return String(raw);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return `${d.toLocaleDateString([], { month: '2-digit', day: '2-digit' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

const SESSIONS_STORAGE_KEY = 'edumind_global_assistant_sessions_v1';

function readSessionsFromStorage(): GlobalAssistantSession[] {
  try {
    const raw = localStorage.getItem(SESSIONS_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.sort((a, b) => {
      const ta = typeof a.updatedAt === 'number' ? a.updatedAt : new Date(String(a.updatedAt || 0)).getTime();
      const tb = typeof b.updatedAt === 'number' ? b.updatedAt : new Date(String(b.updatedAt || 0)).getTime();
      return tb - ta;
    });
  } catch (e) {
    console.warn('Failed to parse assistant sessions from localStorage', e);
    return [];
  }
}

function writeSessionsToStorage(list: GlobalAssistantSession[]) {
  try {
    localStorage.setItem(SESSIONS_STORAGE_KEY, JSON.stringify(list));
  } catch (e) {
    console.warn('Failed to write assistant sessions to localStorage', e);
  }
}

function isBackendConversationId(id: string): boolean {
  return Boolean(id) && !/^conv[_-]/i.test(id);
}

function mapApiMessage(raw: Record<string, unknown>): GlobalAssistantMessage {
  const citations = Array.isArray(raw.citations)
    ? (raw.citations as CitationItem[])
    : undefined;
  return {
    id: String(raw.id || `msg_${Date.now()}`),
    role: (raw.role || 'assistant') as GlobalAssistantMessage['role'],
    content: String(raw.content || ''),
    reasoningContent: String(raw.reasoningContent || raw.reasoning_content || ''),
    citations,
    createdAt: raw.createTime ? String(raw.createTime) : undefined
  };
}

/**
 * 兜底快捷提问：仅在未锚定任何课程 / 课节 / 题目上下文时使用（全域通用教学空间）。
 *
 * 此处不假设任何学科与课程——用户此时恰恰最缺上下文，若把「微积分」「第一章」这类
 * 具体学科写进提问，点下去只会得到答不到点上的内容。改为围绕「平台能做什么、怎么用」，
 * 既能给新用户导览，也不依赖任何前置上下文。
 */
const DEFAULT_PRESET_CHIPS = [
  { label: '平台能力', prompt: '智教云现在都有哪些 AI 教学能力？分别适合解决什么问题？' },
  { label: '上手流程', prompt: '我想从零做出一份可用的试卷，完整流程是怎样的？' },
  { label: '知识入库', prompt: '上传的课件和讲义会变成什么？怎么做成可检索的知识库？' },
  { label: '学情来源', prompt: '学情分析的数据从哪里来？能看到哪些维度的结论？' }
];

const LESSON_STUDIO_PRESET_CHIPS = [
  {
    label: '润色导读',
    prompt: '请润色本课节侧栏导读文案，语气专业简洁，输出可直接填入导读的正文。'
  },
  {
    label: '学习目标',
    prompt: '请为本课节撰写 3–5 条可测量的学习目标，输出可直接填入学习目标卡的内容。'
  },
  {
    label: '续写正文',
    prompt: '请根据当前课节正文摘录自然续写下一小节 Markdown 正文，保持教学节奏。'
  }
];

const LESSON_LEARN_PRESET_CHIPS = [
  {
    label: '讲解难点',
    prompt: '请结合当前课节正文，用例子讲清最容易混淆的概念，并附 2 个自测小问题。'
  },
  {
    label: '知识脉络',
    prompt: '请梳理本课的知识结构，用要点说明各小节如何衔接、各自解决什么问题。'
  },
  {
    label: '检索切片',
    prompt: '请检索本课程知识库中与当前课节相关的讲义切片，概括要点并标注可继续阅读的方向。'
  },
  {
    label: '巩固练习',
    prompt: '请针对当前课节出 3 道练手题（含简要解题思路），难度与课堂讲义一致。'
  }
];

const QUESTION_BANK_PRESET_CHIPS = [
  {
    label: '讲解思路',
    prompt: '请结合当前锚定的完整题目，分步骤讲解解题思路，先不要直接给出最终选项字母。'
  },
  {
    label: '选项辨析',
    prompt: '请逐项说明当前题目各选项为什么对或错，并指出常见误区。'
  },
  {
    label: '考点归纳',
    prompt: '请说明这道题考查的核心知识点，并给出 1–2 道同类变式题的出题方向。'
  }
];

function buildCourseSpacePresetChips() {
  return [
    {
      label: '智能组卷',
      prompt: '请根据当前课程考查知识点，帮我组一份含选择、填空与简答的练习卷提纲。'
    },
    {
      label: '检索切片',
      prompt: '请深度检索当前课程知识库的核心知识点切片与讲义资料，并列出摘要。'
    },
    {
      label: '学情看板',
      prompt: '我想查看本课程近期知识点掌握度与薄弱点，请给出可操作的改进建议。'
    },
    {
      label: '知识图谱',
      prompt: '请梳理当前课程的知识拓扑关系，用层级要点说明前后依赖。'
    }
  ];
}

export function useGlobalAssistant() {
  const route = useRoute();
  const authStore = useAuthStore();
  const teachingCopilotStore = useTeachingCopilotStore();
  const router = useRouter();
  const sseClient = new SSEClient();
  let activeChatStreamId = '';

  async function notifyBackendStreamCancel() {
    const id = activeChatStreamId;
    activeChatStreamId = '';
    if (!id) return;
    try {
      await cancelChatStream(id);
    } catch {
      // ignore
    }
  }

  const drawerVisible = ref(false);
  const inputContent = ref('');
  const isStreaming = ref(false);
  const messages = ref<GlobalAssistantMessage[]>([]);
  const conversationId = ref<string>();
  const messagesScrollRef = ref<HTMLDivElement | null>(null);
  const streamAnchorRef = ref<HTMLDivElement | null>(null);

  // 历史对话：localStorage 为主（刷新不丢），UI 对标 Code Compass
  const isHistoryPanelOpen = ref(false);
  const isSessionLoading = ref(false);
  const sessions = ref<GlobalAssistantSession[]>(readSessionsFromStorage());

  // 流式过程中的临时状态
  const streamingReasoning = ref('');
  const streamingContent = ref('');
  const streamingCitations = ref<CitationItem[]>([]);
  const streamingIntent = ref<{ intent?: string; intentDesc?: string; targetCode?: string }>({});

  const getInitialReasoningFolded = getDefaultReasoningFolded;

  // 深度思考折叠状态（遵循个人偏好 → 深度思考默认呈现策略）
  const isReasoningFolded = ref(getInitialReasoningFolded());
  const isReasoningActive = ref(false);
  const streamPhaseMessage = ref('');
  const answerStreamStarted = ref(false);
  const followUpPrompts = ref<string[]>([]);
  /** 用户手动点击停止后置位，避免 fallback / 重新生成错误覆盖已结算内容 */
  const userStoppedGeneration = ref(false);

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

  const {
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll,
    scrollToBottomInstant,
    scrollToBottomSmooth,
    scheduleFollowStreamOutput
  } = useAIStreamScrollFollow({
    streaming: isStreaming,
    streamingRenderedHtml,
    streamingReasoning,
    messagesScrollRef,
    streamAnchorRef
  });

  function refreshMarkdownUi() {
    nextTick(() => {
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    });
  }

  watch(
    () => messages.value.map((m) => `${m.id ?? ''}:${m.content?.length ?? 0}`).join('|'),
    () => refreshMarkdownUi()
  );

  watch(isStreaming, (streaming, wasStreaming) => {
    if (wasStreaming && !streaming) {
      refreshMarkdownUi();
    }
  });

  // 是否手动指定为全域研读模式（彻底杜绝无故写死 #102）
  const manualGlobalScope = ref(false);

  const lessonSessionScope = computed(() => {
    const ctx = teachingCopilotStore.activeContext;
    if (ctx?.contextModule === 'lesson_studio' && ctx.lessonChapterId && ctx.courseId) {
      return {
        courseId: ctx.courseId,
        lessonChapterId: ctx.lessonChapterId
      };
    }
    return null;
  });

  function sessionStorageKey(courseId?: number): string {
    const scope = lessonSessionScope.value;
    const userId = authStore.currentUser?.id;
    if (scope?.courseId && scope.lessonChapterId && userId) {
      return resolveSessionStorageKey(scope.courseId, scope.lessonChapterId, userId);
    }
    return resolveSessionStorageKey(courseId, undefined, undefined);
  }

  function getStoredSessionId(courseId?: number): string {
    const stored = storage.get(sessionStorageKey(courseId));
    return typeof stored === 'string' ? stored : '';
  }

  function storeSessionId(courseId: number | undefined, sessionId: string) {
    if (!sessionId) return;
    storage.set(sessionStorageKey(courseId), sessionId);
  }

  function clearStoredSessionId(courseId: number | undefined, sessionId: string) {
    if (!sessionId) return;
    if (getStoredSessionId(courseId) === sessionId) {
      storage.remove(sessionStorageKey(courseId));
    }
  }

  const activeCourseId = computed<number | undefined>(() => {
    if (manualGlobalScope.value) return undefined;
    const ctx = teachingCopilotStore.activeContext;
    if (
      ctx?.courseId &&
      (ctx.contextModule === 'lesson_studio' ||
        ctx.contextModule === 'lesson_learn' ||
        ctx.contextModule === 'course_space' ||
        ctx.contextModule === 'question_bank')
    ) {
      return ctx.courseId;
    }
    const queryCourseId = route.query.courseId;
    if (queryCourseId) {
      const parsed = Number(queryCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    const paramCourseId = route.params.courseId || route.params.id;
    // 只有在课程相关路由下，才解析课程参数
    if (paramCourseId && (route.path.includes('/course') || route.name?.toString().includes('course'))) {
      const parsed = Number(paramCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    return undefined;
  });

  const isLessonStudioContext = computed(
    () =>
      !manualGlobalScope.value &&
      teachingCopilotStore.activeContext?.contextModule === 'lesson_studio'
  );

  const isLessonLearnContext = computed(
    () =>
      !manualGlobalScope.value &&
      teachingCopilotStore.activeContext?.contextModule === 'lesson_learn'
  );

  const isQuestionBankContext = computed(
    () =>
      !manualGlobalScope.value &&
      teachingCopilotStore.activeContext?.contextModule === 'question_bank'
  );

  const activeCourseLabel = computed(() => {
    if (manualGlobalScope.value) {
      return '全域研读模式 · 通用教学空间';
    }
    const ctx = teachingCopilotStore.activeContext;
    if (ctx?.contextModule === 'lesson_studio') {
      const statusLabel = ctx.contentStatus === 'PUBLISHED' ? '已发布' : '草稿';
      return `课节备课 · ${ctx.title || '未命名课节'}（${statusLabel}）`;
    }
    if (ctx?.contextModule === 'lesson_learn') {
      return `课节学习 · ${ctx.title || '未命名课节'}`;
    }
    if (ctx?.contextModule === 'question_bank') {
      return ctx.title || '题库题目辅导';
    }
    if (activeCourseId.value) {
      return `课程空间: #${activeCourseId.value} 教学研读中枢`;
    }
    // 动态感知当前路由页面标题（如“智能题库中心”、“学情分析”等）
    const pageTitle = (route.meta?.title as string) || '';
    if (pageTitle) {
      return `教学研读空间: 智教云 · ${pageTitle}`;
    }
    return '全域研读模式 · 通用教学空间';
  });

  function toggleScopeMode() {
    manualGlobalScope.value = !manualGlobalScope.value;
    if (manualGlobalScope.value) {
      ElMessage.info('已切换为全域通用教学研读模式');
    } else {
      ElMessage.info('已同步当前页面研读空间');
    }
  }

  const presetChips = computed(() => {
    const ctx = teachingCopilotStore.activeContext;
    if (isLessonStudioContext.value) return LESSON_STUDIO_PRESET_CHIPS;
    if (isLessonLearnContext.value) return LESSON_LEARN_PRESET_CHIPS;
    if (isQuestionBankContext.value) return QUESTION_BANK_PRESET_CHIPS;
    if (!manualGlobalScope.value && activeCourseId.value) {
      return buildCourseSpacePresetChips();
    }
    return DEFAULT_PRESET_CHIPS;
  });

  // 模型选择已移除：统一由后端按「场景路由 → 平台默认模型(is_default)」决定，
  // 与后台「AI 模型配置」里标了默认的对话模型保持一致，前端不再提供自选入口。

  // 侧边栏 AI 无需秒表计时器展示，保持轻量高效
  const streamTimerText = ref('');
  function startStreamTimer() {}
  function stopStreamTimer() {}

  function buildDynamicPromptPrefix(): string {
    const parts: string[] = [];
    if (route.path.includes('/question') || route.path.includes('/submission')) {
      parts.push(
        '【智教云试题命题与学情批改助教】你正在辅助教师进行作业评阅、答卷诊断与试题考点剖析。请严格遵守学术严谨性，对数学与理工科问题务必使用规范 LaTeX 语法输出公式（行内 $...$，独立块 $$...$$），给出精准的分步推演、解题思路与评分标准。'
      );
    } else if (route.path.includes('/learning') || route.path.includes('/analytics')) {
      parts.push(
        '【智教云学情分析与个性化导学专家】请结合知识图谱掌握度，为学生提供有针对性的薄弱点突破建议与清晰步骤解答，数学公式采用规范 LaTeX 语法渲染。'
      );
    } else {
      parts.push(
        '【智教云 AI 教学副驾驶】你是由知识图谱与教学大模型驱动的专业智能助教。请准确解答教学、备课与学科问题，涉及公式及推导时规范使用 LaTeX 格式输出。'
      );
    }
    return parts.join('\n');
  }

  function buildAssistantRequestBase(): Omit<GlobalAssistantChatRequest, 'message'> {
    const ctxPayload = manualGlobalScope.value
      ? {}
      : buildTeachingContextRequestPayload(teachingCopilotStore.activeContext);
    return {
      courseId: activeCourseId.value,
      conversationId: conversationId.value,
      // 不传 modelKey：由后端按「场景路由 → 平台默认对话模型(is_default)」自动选择
      promptPrefix: buildDynamicPromptPrefix(),
      ...ctxPayload
    };
  }

  function toggleDrawer() {
    drawerVisible.value = !drawerVisible.value;
    if (drawerVisible.value) {
      sessions.value = readSessionsFromStorage();
      restoreFollowUpsForLastTurn();
      nextTick(() => {
        scrollToBottomSmooth();
        bindMarkdownCodeCopy(messagesScrollRef.value);
      });
    }
  }

  function saveCurrentSessionToHistory() {
    if (messages.value.length === 0) return;

    if (!conversationId.value) {
      conversationId.value = `conv_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    }
    const currentId = conversationId.value;
    const firstUserMsg = messages.value.find((m) => m.role === 'user');
    const rawTitle = firstUserMsg?.content?.trim() || '全新教学研读会话';
    const promptTitle = buildSessionTitleFromPrompt(rawTitle);
    const existing = sessions.value.find((s) => s.id === currentId);
    // 关键：每轮保存都会重写会话对象，已被 AI 命名的标题不能被「提问前 14 字」重新覆盖，
    // 否则历史列表永远停在长提问上（与课程 AI 之前的同类问题）
    const keepAiTitle = Boolean(existing?.titleFromAi && existing.title);

    const sessionItem: GlobalAssistantSession = {
      id: currentId,
      title: keepAiTitle ? existing!.title : promptTitle,
      titleFromAi: keepAiTitle ? existing!.titleFromAi : undefined,
      updatedAt: Date.now(),
      messageCount: messages.value.length,
      messages: JSON.parse(JSON.stringify(messages.value))
    };

    const existingIdx = sessions.value.findIndex((s) => s.id === currentId);
    if (existingIdx >= 0) {
      sessions.value.splice(existingIdx, 1, sessionItem);
    } else {
      sessions.value.unshift(sessionItem);
    }

    if (sessions.value.length > 30) {
      sessions.value = sessions.value.slice(0, 30);
    }
    writeSessionsToStorage(sessions.value);
    storeSessionId(activeCourseId.value, currentId);
  }

  /**
   * 副驾驶会话标题：交给模型生成短标题（此前历史列表一直显示「提问前 36 字」）。
   *
   * <p>后端在答完后也会异步兜底生成；这里再主动请求一次是为了拿到标题后**立刻**刷新本地列表，
   * 不必等用户刷新页面。失败静默保留兜底标题。</p>
   */
  function refreshSessionAiTitle() {
    const convId = conversationId.value;
    if (!convId) return;
    void generateRemoteSessionTitle(convId)
      .then((res) => {
        const aiTitle = String(res?.data || '').trim();
        if (!aiTitle) return;
        const target = sessions.value.find((s) => s.id === convId);
        if (!target) return;
        target.title = aiTitle;
        target.titleFromAi = true;
        writeSessionsToStorage(sessions.value);
      })
      .catch(() => {});
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
        const targetId = lastMsg.id;
        const prompts = requestFollowUps(lastUserMsg.content, lastMsg.content, {
          courseId: activeCourseId.value,
          onUpdate: (next) => {
            // 按 id 判定「仍然是当轮消息」，并通过数组里的代理对象回写（引用比较在响应式代理下不可靠）
            const current = pickTurnMessage(messages.value, targetId);
            if (!current) return;
            current.followUpPrompts = next;
            followUpPrompts.value = next;
            saveCurrentSessionToHistory();
          }
        });
        followUpPrompts.value = prompts;
        lastMsg.followUpPrompts = prompts;
      }
    } else {
      followUpPrompts.value = [];
    }
  }

  async function loadSessionMessagesFromBackend(sessionId: string) {
    isSessionLoading.value = true;
    try {
      const res = await getMessages(sessionId);
      messages.value = (res.data || []).map((item) =>
        mapApiMessage(item as unknown as Record<string, unknown>)
      );
      conversationId.value = sessionId;
      storeSessionId(activeCourseId.value, sessionId);
      restoreFollowUpsForLastTurn();
    } catch {
      messages.value = [];
      followUpPrompts.value = [];
      ElMessage.error('加载会话记录失败');
    } finally {
      isSessionLoading.value = false;
    }
  }

  function toggleHistoryPanel() {
    isHistoryPanelOpen.value = !isHistoryPanelOpen.value;
    if (isHistoryPanelOpen.value) {
      sessions.value = readSessionsFromStorage();
    }
  }

  async function selectSession(session: GlobalAssistantSession) {
    if (isStreaming.value) {
      ElMessage.warning('当前正在生成中，请先停止生成');
      return;
    }
    if (messages.value.length > 0) {
      saveCurrentSessionToHistory();
    }
    conversationId.value = session.id;
    followUpPrompts.value = [];
    resetStreamingState();
    isHistoryPanelOpen.value = false;

    if (session.messages && session.messages.length > 0) {
      messages.value = JSON.parse(JSON.stringify(session.messages));
      restoreFollowUpsForLastTurn();
    } else if (isBackendConversationId(session.id)) {
      await loadSessionMessagesFromBackend(session.id);
    } else {
      messages.value = [];
      followUpPrompts.value = [];
    }

    nextTick(() => {
      scrollToBottomInstant();
      bindMarkdownCodeCopy(messagesScrollRef.value);
    });
  }

  function confirmDeleteSession(sessionId: string) {
    const target = sessions.value.find((s) => s.id === sessionId);
    ElMessageBox.confirm(
      `确定要删除「${target?.title || '该会话'}」吗？删除后将无法找回。`,
      '删除历史会话',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        lockScroll: false
      }
    )
      .then(async () => {
        if (isBackendConversationId(sessionId)) {
          try {
            await deleteConversation(sessionId);
          } catch {
            // 本地会话仍允许删除
          }
        }
        sessions.value = sessions.value.filter((s) => s.id !== sessionId);
        writeSessionsToStorage(sessions.value);
        clearStoredSessionId(activeCourseId.value, sessionId);
        if (conversationId.value === sessionId) {
          conversationId.value = undefined;
          messages.value = [];
          followUpPrompts.value = [];
          resetStreamingState();
        }
        ElMessage.success('会话已删除');
      })
      .catch(() => {});
  }

  function confirmClearAllSessions() {
    if (sessions.value.length === 0) return;
    ElMessageBox.confirm('确定要清空全部历史会话记录吗？清空后将无法恢复。', '清空全部历史记录', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    })
      .then(async () => {
        if (isStreaming.value) {
          stopStreaming();
        }
        const ids = sessions.value.map((s) => s.id);
        await Promise.all(
          ids.filter(isBackendConversationId).map((id) => deleteConversation(id).catch(() => undefined))
        );
        sessions.value = [];
        writeSessionsToStorage([]);
        conversationId.value = undefined;
        messages.value = [];
        followUpPrompts.value = [];
        resetStreamingState();
        storage.remove(sessionStorageKey(activeCourseId.value));
        ElMessage.success(ids.length > 0 ? `已清空 ${ids.length} 条历史会话` : '暂无历史会话可清空');
      })
      .catch(() => {});
  }

  function clearMessages() {
    if (messages.value.length === 0) return;
    ElMessageBox.confirm('确定要清空当前的全部对话记录吗？清空后将无法恢复。', '清空会话确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    })
      .then(async () => {
        const currentId = conversationId.value;
        if (currentId) {
          if (isBackendConversationId(currentId)) {
            try {
              await deleteConversation(currentId);
            } catch {
              // ignore
            }
          }
          sessions.value = sessions.value.filter((s) => s.id !== currentId);
          writeSessionsToStorage(sessions.value);
          clearStoredSessionId(activeCourseId.value, currentId);
        }
        messages.value = [];
        conversationId.value = undefined;
        followUpPrompts.value = [];
        resetStreamingState();
        ElMessage.success('已清空当前对话记录');
      })
      .catch(() => {});
  }

  function startNewSession() {
    if (isStreaming.value) {
      stopStreaming();
    }
    if (messages.value.length > 0) {
      saveCurrentSessionToHistory();
    }
    if (messages.value.length === 0 && !conversationId.value) {
      isHistoryPanelOpen.value = false;
      return;
    }
    conversationId.value = undefined;
    messages.value = [];
    followUpPrompts.value = [];
    resetStreamingState();
    isHistoryPanelOpen.value = false;
  }


  function getIntentTagType(intent?: string) {
    if (intent === 'agent' || intent === 'EXAM_COMPOSE') return 'danger';
    if (intent === 'rag' || intent === 'KNOWLEDGE_RETRIEVAL') return 'warning';
    if (intent === 'navigate' || intent === 'REPORT_ANALYTICS') return 'success';
    return 'primary';
  }

  function applyIntentEvent(data: GlobalAssistantIntentEvent) {
    const routeType = data.route || '';
    const agentCode = data.agentCode || '';
    streamingIntent.value = {
      intent: routeType,
      intentDesc: resolveIntentDesc(routeType, agentCode),
      targetCode: resolveTargetPath(routeType, agentCode)
    };
  }

  let currentTurnSettled = false;

  function stopStreaming() {
    if (!isStreaming.value) return;
    stopStreamTimer();
    userStoppedGeneration.value = true;
    void notifyBackendStreamCancel();
    sseClient.stop();
    isStreaming.value = false;

    const answer = streamingAnswerBody.value || streamingContent.value;
    finishStreamingMarkdown();
    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      content: buildStoppedGenerationContent(answer),
      reasoningContent: cleanReasoningText(streamingReasoning.value || streamingThinkingBody.value),
      intent: streamingIntent.value.intent,
      intentDesc: streamingIntent.value.intentDesc,
      targetCode: streamingIntent.value.targetCode,
      citations: [...streamingCitations.value],
      createdAt: Date.now()
    });

    resetStreamingState();
    saveCurrentSessionToHistory();
    ElMessage.info('已停止生成');
    nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
  }

  function resetStreamingState() {
    streamingReasoning.value = '';
    streamingContent.value = '';
    streamingCitations.value = [];
    streamingIntent.value = {};
    isReasoningFolded.value = getInitialReasoningFolded();
    isReasoningActive.value = false;
    streamPhaseMessage.value = '';
    answerStreamStarted.value = false;
    resetStreamingMarkdown();
  }

  function finalizeAssistantMessage() {
    stopStreamTimer();
    finishStreamingMarkdown();
    const finalReasoning = cleanReasoningText(
      streamingReasoning.value || streamingThinkingBody.value
    );
    const finalAnswer = streamingAnswerBody.value || streamingContent.value;
    const lastUserPrompt = [...messages.value].reverse().find((m) => m.role === 'user')?.content || '';

    const assistantMessage: GlobalAssistantMessage = {
      id: Date.now(),
      role: 'assistant',
      content: finalAnswer.trim(),
      reasoningContent: finalReasoning,
      intent: streamingIntent.value.intent,
      intentDesc: streamingIntent.value.intentDesc,
      targetCode: streamingIntent.value.targetCode,
      citations: [...streamingCitations.value],
      followUpPrompts: []
    };

    // 追问：模型结果异步到达后才回写（拿不到就静默回落到规则生成）
    const finalFollowUps = requestFollowUps(lastUserPrompt, finalAnswer, {
      courseId: activeCourseId.value,
      onUpdate: (prompts) => {
        // 用 pickTurnMessage 按 id 拿「数组里的代理对象」回写：
        // 直接与原始对象做引用比较会恒为 false（响应式代理），表现就是「追问要刷新才出现」
        const current = pickTurnMessage(messages.value, assistantMessage.id);
        if (!current) return;
        current.followUpPrompts = prompts;
        followUpPrompts.value = prompts;
        saveCurrentSessionToHistory();
      }
    });
    assistantMessage.followUpPrompts = finalFollowUps;
    followUpPrompts.value = finalFollowUps;
    messages.value.push(assistantMessage);

    currentTurnSettled = true;
    resetStreamingState();
    // 关键修复：一旦回答结算入库，流式状态必须立即置为 false，消除正在进行的思考气泡
    isStreaming.value = false;
    saveCurrentSessionToHistory();
    // 会话标题改由模型生成：先落库兜底标题，再异步换取短标题并刷新本地历史列表
    refreshSessionAiTitle();
    window.dispatchEvent(new CustomEvent('edumind:ai-usage-changed'));
    nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
  }

  async function streamChat(query: string): Promise<boolean> {
    return streamGlobalAssistantChat(
      sseClient,
      query,
      buildAssistantRequestBase(),
      streamingContent,
      {
        isStopped: () => userStoppedGeneration.value,
        onStreamId: (streamId) => {
          activeChatStreamId = streamId;
        },
        onIntent: (data) => applyIntentEvent(data as GlobalAssistantIntentEvent),
        onStatus: (message, phase) => {
          streamPhaseMessage.value = message;
          if (phase === 'reasoning') isReasoningActive.value = true;
          if (phase === 'composing') isReasoningActive.value = false;
        },
        onReasoningChunk: (chunk) => {
          streamingReasoning.value += chunk;
          isReasoningActive.value = true;
          streamPhaseMessage.value = '';
        },
        onDeltaChunk: (_chunk, answerDelta) => {
          if (!answerDelta) return;
          isReasoningActive.value = false;
          streamPhaseMessage.value = '';
          if (!answerStreamStarted.value) {
            answerStreamStarted.value = true;
            isReasoningFolded.value = getInitialReasoningFolded();
          }
          appendStreamingMarkdown(answerDelta);
        },
        onCitations: (cits) => {
          streamingCitations.value = cits;
        },
        onDone: (data) => {
          const d = data as {
            conversationId?: string;
            citations?: CitationItem[];
            reasoningContent?: string;
          };
          if (d.conversationId) conversationId.value = String(d.conversationId);
          if (d.citations && d.citations.length > 0) {
            streamingCitations.value = d.citations;
          }
          if (d.reasoningContent && !streamingReasoning.value) {
            streamingReasoning.value = String(d.reasoningContent);
          }
          finalizeAssistantMessage();
        },
        onFollowOutput: scheduleFollowStreamOutput
      }
    );
  }

  async function fallbackAsk(query: string) {
    const res = await askGlobalAssistant({
      message: query,
      ...buildAssistantRequestBase()
    });
    const data = res?.data;
    if (!data) {
      throw new Error('服务未返回有效数据');
    }
    conversationId.value = data.conversationId;
    streamingIntent.value = {
      intent: data.intent,
      intentDesc: data.intentDesc,
      targetCode: data.targetCode
    };
    if (data.reasoningContent) {
      streamingReasoning.value = data.reasoningContent;
    }
    if (data.citations && Array.isArray(data.citations)) {
      streamingCitations.value = data.citations as CitationItem[];
    }
    streamingContent.value = data.content || '已处理您的教学助手请求。';
    finalizeAssistantMessage();
  }

  async function handleSubmit(options?: { isRegenerate?: boolean } | Event) {
    const query = inputContent.value.trim();
    if (!query || isStreaming.value) return;

    const isRegenerate = Boolean(options && typeof options === 'object' && 'isRegenerate' in options && (options as { isRegenerate?: boolean }).isRegenerate);
    if (!isRegenerate) {
      const lessonInsertIntent = teachingCopilotStore.activeContext?.lessonInsertIntent;
      teachingCopilotStore.patchContext({ lessonInsertIntent: undefined });
      messages.value.push({
        id: Date.now(),
        role: 'user',
        content: query,
        createdAt: Date.now(),
        ...(lessonInsertIntent ? { lessonInsertIntent } : {})
      });
    }
    inputContent.value = '';
    followUpPrompts.value = [];
    currentTurnSettled = false;
    resetStreamingState();
    isStreaming.value = true;
    startStreamTimer();
    scrollToBottomInstant();

    try {
      const streamed = await streamChat(query);
      if (userStoppedGeneration.value || currentTurnSettled) return;
      if (!streamed && !streamingContent.value) {
        if (isRegenerate) {
          throw new Error('重新生成未返回有效内容，请稍后重试');
        }
        await fallbackAsk(query);
      }
    } catch {
      if (userStoppedGeneration.value || currentTurnSettled) return;
      if (isRegenerate) {
        const message = '重新生成失败，请检查模型 API Key 或网络后重试';
        messages.value.push({
          id: Date.now() + 2,
          role: 'assistant',
          content: message,
          error: true,
          createdAt: Date.now()
        });
        resetStreamingState();
        saveCurrentSessionToHistory();
      } else {
        try {
          await fallbackAsk(query);
        } catch (err: unknown) {
          const message = err instanceof Error ? err.message : '服务异常，请检查网络或稍后重试';
          messages.value.push({
            id: Date.now() + 2,
            role: 'assistant',
            content: `请求失败：${message}`,
            error: true,
            createdAt: Date.now()
          });
          resetStreamingState();
          saveCurrentSessionToHistory();
        }
      }
    } finally {
      stopStreamTimer();
      userStoppedGeneration.value = false;
      isStreaming.value = false;
      scrollToBottomInstant();
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    }
  }

  function handleSendPrompt(promptText: string) {
    if (isStreaming.value) return;
    inputContent.value = promptText;
    handleSubmit();
  }

  function handleRegenerate(targetIdx: number) {
    if (isStreaming.value) return;
    let userPrompt = '';
    for (let i = targetIdx; i >= 0; i--) {
      if (messages.value[i]?.role === 'user') {
        userPrompt = messages.value[i].content;
        break;
      }
    }
    if (!userPrompt) return;

    messages.value.splice(targetIdx);
    inputContent.value = userPrompt;
    handleSubmit({ isRegenerate: true });
  }

  async function confirmDeleteMessage(targetIdx: number) {
    try {
      await ElMessageBox.confirm(
        '将删除本条及其对应的一问一答，删除后无法恢复。确定继续吗？',
        '删除对话确认',
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
      saveCurrentSessionToHistory();
      ElMessage.success('已删除本轮对话记录');
    } catch {
      if (!persistedId) {
        messages.value = removeMessagesByIds(messages.value, idsToDelete);
        saveCurrentSessionToHistory();
        ElMessage.success('已删除本轮对话记录');
        return;
      }
      ElMessage.error('删除失败，请稍后重试');
    }
  }

  async function copyMessage(content: string) {
    if (!content) return;
    try {
      await navigator.clipboard.writeText(content);
      ElMessage.success('已复制内容到剪贴板');
    } catch {
      ElMessage.warning('复制失败，请手动选取复制');
    }
  }

  function jumpToCitation(citation: CitationItem) {
    const ctx = teachingCopilotStore.activeContext;
    const lessonId = citation.lessonChapterId ?? ctx?.lessonChapterId;
    const courseId = ctx?.courseId ?? activeCourseId.value;

    if (courseId && lessonId) {
      drawerVisible.value = false;
      const hash = citation.anchor ? `#${encodeURIComponent(citation.anchor)}` : '';
      const cleanExcerpt = (citation.snippet || citation.excerpt || '')
        .replace(/[#*`$\\]/g, '')
        .replace(/\s+/g, ' ')
        .trim()
        .slice(0, 36);

      router.push({
        path: `/course/${courseId}/learn/${lessonId}`,
        hash,
        query: {
          ...(citation.anchor ? { anchor: citation.anchor } : {}),
          ...(citation.chunkId ? { chunkId: String(citation.chunkId) } : {}),
          ...(cleanExcerpt ? { excerpt: cleanExcerpt } : {})
        }
      });
      return;
    }

    const kbId = citation.knowledgeBaseId;
    const docId = citation.documentId;
    if (kbId && citation.chunkId) {
      drawerVisible.value = false;
      router.push({
        path: `/knowledge/${kbId}/chunks`,
        query: {
          ...(docId ? { docId: String(docId) } : {}),
          chunkId: String(citation.chunkId)
        }
      });
      return;
    }

    ElMessage.info(`参考来源：${citation.documentName || '课程知识切片'}`);
  }

  function handleNavigate(path: string) {
    drawerVisible.value = false;
    router.push(path);
  }

  function formatMatchScore(score?: number, peerScores?: number[]): string {
    return formatCitationMatchLabel(score, peerScores);
  }

  watch(
    () => messages.value.length,
    () => {
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    }
  );

  function handleOpenAssistantEvent(e: Event) {
    const detail = (e as CustomEvent<{ prefill?: string; autoSend?: boolean; startNewSession?: boolean }>).detail;
    const prefill = detail?.prefill?.trim();
    const autoSend = Boolean(detail?.autoSend && prefill);
    drawerVisible.value = true;
    sessions.value = readSessionsFromStorage();
    // 携带新题目/资料上下文打开时开启新会话：
    // 否则会沿用上一轮对话与追问，既显示旧聊天记录，也会把快捷提问顶掉。
    if (detail?.startNewSession) {
      startNewSession();
    } else {
      restoreFollowUpsForLastTurn();
    }
    if (prefill) {
      inputContent.value = prefill;
    }
    nextTick(() => {
      scrollToBottomSmooth();
      bindMarkdownCodeCopy(messagesScrollRef.value);
      if (autoSend) {
        void handleSubmit();
      }
    });
  }

  onMounted(() => {
    window.addEventListener(OPEN_GLOBAL_ASSISTANT_EVENT, handleOpenAssistantEvent);
  });

  onUnmounted(() => {
    stopStreamTimer();
    window.removeEventListener(OPEN_GLOBAL_ASSISTANT_EVENT, handleOpenAssistantEvent);
  });

  return {
    drawerVisible,
    inputContent,
    isStreaming,
    messages,
    conversationId,
    messagesScrollRef,
    streamAnchorRef,
    showScrollToBottom,
    activeCourseId,
    activeCourseLabel,
    manualGlobalScope,
    toggleScopeMode,
    presetChips,
    isLessonStudioContext,
    followUpPrompts,
    showThinkingPanel: computed(() => !isThinkingPanelHidden()),
    // 秒表计时状态
    streamTimerText,
    // 历史会话管理 (对标 Code Compass 原型)
    isHistoryPanelOpen,
    isSessionLoading,
    sessions,
    toggleHistoryPanel,
    selectSession,
    confirmDeleteSession,
    confirmClearAllSessions,
    formatSessionTime,
    // 流式状态
    streamingReasoning,
    streamingContent,
    streamingCitations,
    streamingIntent,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    // 方法
    toggleDrawer,
    clearMessages,
    startNewSession,
    handleSubmit,
    handleSendPrompt,
    stopStreaming,
    handleRegenerate,
    confirmDeleteMessage,
    copyMessage,
    jumpToCitation,
    handleNavigate,
    scrollToBottomSmooth,
    scrollToBottomInstant,
    scheduleFollowStreamOutput,
    pauseAutoScrollFollow,
    handleViewportScroll,
    getIntentTagType,
    formatMatchScore
  };
}

