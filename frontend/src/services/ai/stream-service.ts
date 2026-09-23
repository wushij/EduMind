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
import { STOPPED_GENERATION_MARKER } from '@/utils/ai/copilot-stream-split';
import { runCopilotSseStream } from '@/services/ai/copilot-sse-stream';
import type { CitationItem } from '@/types/ai/assistant';

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
  recalledMemories?: Array<{ id: number; summary: string; memoryType?: string }>;
  followUpPrompts?: string[];
}

export interface ChatSession {
  id: string;
  title: string;
  updatedAt: string;
}

export interface StreamOptions {
  chapterId?: number;
  lessonChapterId?: number;
  modelKey?: string;
  model?: string;
  useRag?: boolean;
  enableThinking?: boolean;
  activeSectionTitle?: string;
  isRegenerate?: boolean;
  webSearch?: boolean;
  attachmentIds?: string[];
}

export interface LoadSessionsOptions {
  restoreLastSession?: boolean;
  skipMessageReload?: boolean;
}

export interface StreamDonePayload {
  conversationId?: string;
  messageId?: string;
  userMessageId?: string;
  citations?: CitationItem[];
  reasoningContent?: string;
}

export interface StreamEventHandlers {
  onStreamId?: (streamId: string) => void;
  onStatus?: (message: string, phase?: string) => void;
  onReasoningChunk?: (chunk: string) => void;
  onDeltaChunk?: (chunk: string, answerDelta: string) => void;
  onCitations?: (citations: CitationItem[]) => void;
  onMemory?: (memories: Array<{ id: number; summary: string; memoryType?: string }>) => void;
  onDone?: (payload: StreamDonePayload) => void;
  onFollowOutput?: () => void;
  isStopped?: () => boolean;
}

const SESSION_STORAGE_PREFIX = 'edumind_course_ai_session:';
const MESSAGE_CACHE_PREFIX = 'edumind_course_ai_messages:';

export function resolveStreamModelKey(options?: StreamOptions): string | undefined {
  return options?.modelKey || options?.model;
}

export function isDefaultSessionTitle(title?: string): boolean {
  if (!title?.trim()) return true;
  return /^(新会话|新问答会话)$/.test(title.trim());
}

export function messageCacheKey(courseId?: number, conversationId?: string): string {
  if (!courseId || !conversationId) return '';
  return `${MESSAGE_CACHE_PREFIX}${courseId}:${conversationId}`;
}

export function persistMessageCache(
  courseId: number | undefined,
  conversationId: string | undefined,
  msgs: ChatMessage[]
) {
  const key = messageCacheKey(courseId, conversationId);
  if (!key || msgs.length === 0) return;
  try {
    sessionStorage.setItem(key, JSON.stringify(msgs));
  } catch {
    // ignore quota errors
  }
}

export function readMessageCache(courseId?: number, conversationId?: string): ChatMessage[] {
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

export function clearMessageCache(courseId?: number, conversationId?: string) {
  const key = messageCacheKey(courseId, conversationId);
  if (key) sessionStorage.removeItem(key);
}

export function isStoppedAssistantMessage(msg: ChatMessage): boolean {
  return msg.role === 'assistant' && !!msg.content?.includes(STOPPED_GENERATION_MARKER);
}

/** 合并去重时忽略课程助教注入的前缀标签，避免本地/服务端文案细微差异导致重复气泡 */
export function normalizePromptForDedup(content: string): string {
  return content
    .replace(/^\[助教风格:[^\]]+\]\s*/g, '')
    .replace(/^\[针对课时:[^\]]+\]\s*/g, '')
    .replace(/^\[当前章节:[^\]]+\]\s*/gi, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function userPromptsEquivalent(a: string, b: string): boolean {
  if (a === b) return true;
  const na = normalizePromptForDedup(a);
  const nb = normalizePromptForDedup(b);
  return na.length > 0 && na === nb;
}

export function mergeServerWithLocalDrafts(server: ChatMessage[], local: ChatMessage[]): ChatMessage[] {
  if (!local.length) return server;
  if (!server.length) return local;

  const result = [...server];

  const hasUserContent = (content: string) =>
    result.some((m) => m.role === 'user' && userPromptsEquivalent(m.content, content));

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

    const userIdx = result.findIndex(
      (m) => m.role === 'user' && userPromptsEquivalent(m.content, userContent)
    );
    const next = result[userIdx + 1];
    if (!next || next.role !== 'assistant') {
      if (!hasAssistantContent(draftAssistant.content)) {
        result.splice(userIdx + 1, 0, draftAssistant);
      }
    }
  }

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

export function getStoredSessionId(courseId?: number): string {
  if (!courseId) return '';
  const stored = storage.get(`${SESSION_STORAGE_PREFIX}${courseId}`);
  return typeof stored === 'string' ? stored : '';
}

export function storeSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  storage.set(`${SESSION_STORAGE_PREFIX}${courseId}`, sessionId);
}

export function clearStoredSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  if (getStoredSessionId(courseId) === sessionId) {
    storage.remove(`${SESSION_STORAGE_PREFIX}${courseId}`);
  }
}

export function buildSessionTitleFromPrompt(text: string): string {
  const cleaned = text
    .replace(/^\[当前章节:[^\]]+\]\s*/i, '')
    .replace(/\s+/g, ' ')
    .trim();
  return cleaned.slice(0, 20) || '新问答会话';
}

export function formatSessionTime(raw: unknown): string {
  if (!raw) return '刚刚';
  const d = new Date(String(raw));
  if (Number.isNaN(d.getTime())) return String(raw);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return `${d.toLocaleDateString([], { month: '2-digit', day: '2-digit' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

export function mapCitation(raw: Record<string, unknown>): CitationItem {
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

export interface FollowUpOptions {
  sectionTitle?: string;
  courseTitle?: string;
}

/**
 * 从用户提问及 AI 回答正文中动态提取核心知识主题与专有技术实体
 */
export function extractKeyTopicsFromContent(query: string, answer?: string): string[] {
  const topics: string[] = [];
  const seen = new Set<string>();

  const sanitizeTopic = (raw: string): string => {
    return raw
      .replace(/^[0-9一二三四五六七八九十]+[\.\、\:\s\-—]*/, '')
      .replace(/[*#`_~[\]()（）]/g, '')
      .replace(/(详细解析|深入分析|核心原理|总体架构|全景概述|主要内容|基本概念|简介)$/, '')
      .trim();
  };

  const addTopic = (t: string) => {
    const cleaned = sanitizeTopic(t);
    if (cleaned.length >= 2 && cleaned.length <= 24) {
      const lower = cleaned.toLowerCase();
      const isNoise = /^(总结|注意|提示|说明|例如|举例|关键|结论|分析|概述|引言|目录|思考|自测|小结|思考题|建议|核心要点|总体)$/.test(lower);
      if (!isNoise && !seen.has(lower)) {
        seen.add(lower);
        topics.push(cleaned);
      }
    }
  };

  // 1. 优先提取回答中的 Markdown 各级小标题 (##, ###)
  if (answer) {
    const headingLines = answer.match(/^(?:#{1,4})\s+(.+)$/gm);
    if (headingLines) {
      for (const line of headingLines) {
        const titleText = line.replace(/^#{1,4}\s+/, '').trim();
        addTopic(titleText);
        if (topics.length >= 4) break;
      }
    }
  }

  // 2. 提取回答中加粗标记的核心术语概念 (**概念**)
  if (answer && topics.length < 4) {
    const boldMatches = answer.match(/\*\*([^*]{2,20})\*\*/g);
    if (boldMatches) {
      for (const b of boldMatches) {
        const boldText = b.replace(/\*\*/g, '').trim();
        addTopic(boldText);
        if (topics.length >= 5) break;
      }
    }
  }

  // 3. 提取代码块中的关键类名或技术定义 (如 class Foo, interface Bar)
  if (answer && topics.length < 4) {
    const codeMatch = answer.match(/(?:class|interface|function|def|struct|enum)\s+([A-Za-z0-9_]{2,25})/);
    if (codeMatch && codeMatch[1]) {
      addTopic(codeMatch[1]);
    }
  }

  // 4. 从用户提问中提取用书名号、中括号包裹的专有名词
  const entityMatches = query.match(/[【《“]([^】》”]{2,20})[】》”]/g);
  if (entityMatches) {
    for (const em of entityMatches) {
      addTopic(em.slice(1, -1));
    }
  }

  return topics;
}

export function generateSmartFollowUps(
  query: string,
  answer?: string,
  options?: FollowUpOptions
): string[] {
  const cleanQuery = query.replace(/^\[[^\]]+\]\s*/g, '').trim();
  const q = cleanQuery.toLowerCase() || query.toLowerCase();

  // 1. 动态提取回答和提问中的真实技术实体与核心主题
  const topics = extractKeyTopicsFromContent(cleanQuery, answer);

  // 若成功提取到了具体的技术知识点，则生成具象化、直击核心的深度追问
  if (topics.length >= 2) {
    const t1 = topics[0];
    const t2 = topics[1];
    const t3 = topics[2] || topics[0];

    const results: string[] = [];

    // 追问 1：针对核心知识点 1 的底层运行机制与原理深入
    results.push(`深入剖析「${t1}」的底层工作原理与运行时机制`);

    // 追问 2：针对核心知识点 1 与 2 的横向对比或工程落地踩坑
    if (/区别|对比|不同|差异|辨析|选型/.test(q) || /与|和|vs/.test(t1) || /与|和|vs/.test(t2)) {
      results.push(`请用对比表格详细梳理「${t1}」与「${t2}」的核心差异与适用场景`);
    } else {
      results.push(`在企业级实际业务中，使用「${t2}」有哪些高频避坑指南或最佳实践？`);
    }

    // 追问 3：针对知识点 3 的代码示例演练或考查自测
    if (/代码|实现|函数|工程|实战|bug|异常/.test(q) || (answer && answer.includes('```'))) {
      results.push(`请提供一个关于「${t3}」的典型工业级可运行代码示例并逐行剖析`);
    } else {
      results.push(`围绕「${t3}」出一道考查深度理解的典型思考自测题并附解析`);
    }

    return results;
  }

  if (topics.length === 1) {
    const t = topics[0];
    return [
      `深入剖析「${t}」的底层实现原理与运行机制`,
      `在实际工程业务落地中，针对「${t}」有哪些高频踩坑点与优化策略？`,
      `围绕「${t}」给出一个典型的应用场景与可运行实操代码示例`
    ];
  }

  // 2. 兜底方案：结合章节上下文或意图模型生成更有针对性的追问
  const sectionHint = options?.sectionTitle?.trim() ? `「${options.sectionTitle.trim()}」` : '';

  if (/出题|题目|试题|试卷|考题|做题|作业|考点|数列|函数解析|数学|难题|随堂|真题|选择题|填空题|大题|解答题|难度系数/.test(q)) {
    return [
      `根据上述考点${sectionHint}，衍生 3 道同等难度的变式训练题`,
      '导出上述解题的评分采分点与核心评分细则标准',
      '深入剖析这道题在真实考试中学生最容易踩的失分陷阱'
    ];
  }

  if (/写代码|代码实现|写一个函数|写个类|报错|bug|debug|堆栈|异常处理|spring boot|vue|typescript|sql查询/.test(q) || (/代码|编程/.test(q) && /写|看|改|实现|重构/.test(q))) {
    return [
      '给出此逻辑更具健壮性的工业级优化版代码实现',
      '分析这段代码在极端并发或大数据量下的边界隐患与异常处理',
      '结合主流设计模式对该段代码进行结构重构与解耦'
    ];
  }

  if (/概念|原理|是什么|区别|体系|架构|为什么/.test(q)) {
    return [
      `用通俗生动的比喻进一步剖析${sectionHint || '该技术'}的底层原理`,
      `用对比表格清晰列出${sectionHint || '它'}与关联技术模块的关键异同`,
      '结合当前课程教学大纲，梳理其前驱知识依赖与后继拓展方向'
    ];
  }

  if (/算法|数据结构|复杂度|时间复杂度|空间复杂度|渐进表示|递归|动态规划|贪心/.test(q)) {
    return [
      '分析该算法在最好、最坏与平均情况下的具体时空复杂度',
      '用简明步骤图解该算法的内存变化与执行推演过程',
      '对比该算法与其他常见替代算法的性能优缺点与适用边界'
    ];
  }

  return [
    `结合${sectionHint || '当前考点'}给出一个通俗生动的教学应用案例`,
    `针对上述讲解内容，设计 2 道课堂即兴互动自测提问`,
    `该知识点在企业级实际生产项目中有哪些典型的落地应用场景？`
  ];
}

export function mapSession(raw: Record<string, unknown>): ChatSession {
  return {
    id: String(raw?.id || `sess_${Date.now()}`),
    title: (raw?.title as string) || '新会话',
    updatedAt: formatSessionTime(raw?.updateTime || raw?.updatedAt || raw?.createTime)
  };
}

export function mapMessage(raw: Record<string, unknown>): ChatMessage {
  const citations = Array.isArray(raw?.citations)
    ? (raw.citations as Record<string, unknown>[]).map(mapCitation)
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

export function findFirstUserPrompt(source: ChatMessage[]): string {
  return source.find((m) => m.role === 'user' && m.content?.trim())?.content?.trim() || '';
}

export async function fetchConversationList(courseId?: number) {
  const res = await getConversations(courseId);
  return (res.data || []).map((item) => mapSession(item as unknown as Record<string, unknown>));
}

export async function fetchMessageList(conversationId: string) {
  const res = await getMessages(conversationId);
  return (res.data || []).map((item) => mapMessage(item as unknown as Record<string, unknown>));
}

export async function createRemoteSession(courseId?: number) {
  const res = await createConversation({ courseId, title: '新问答会话' });
  return mapSession((res.data || {}) as unknown as Record<string, unknown>);
}

export async function renameRemoteSession(id: string, title: string) {
  await renameConversation(id, title);
}

export async function deleteRemoteSession(id: string) {
  await deleteConversation(id);
}

export async function deleteRemoteMessage(persistedId: string) {
  return deleteMessage(persistedId);
}

export async function generateRemoteSessionTitle(conversationId: string) {
  return generateConversationTitle(conversationId);
}

export async function syncSessionTitleIfDefault(
  sessions: ChatSession[],
  conversationId: string,
  messages: ChatMessage[],
  promptHint?: string,
  options?: { tryLlmTitle?: boolean }
): Promise<void> {
  if (!conversationId) return;
  const sess = sessions.find((s) => s.id === conversationId);
  if (!sess || !isDefaultSessionTitle(sess.title)) return;

  const prompt = promptHint || findFirstUserPrompt(messages);
  if (!prompt) return;

  const fallbackTitle = buildSessionTitleFromPrompt(prompt);
  if (isDefaultSessionTitle(fallbackTitle)) return;

  sess.title = fallbackTitle;
  try {
    await renameRemoteSession(conversationId, fallbackTitle);
  } catch {
    // keep local title
  }

  if (!options?.tryLlmTitle) return;
  void generateRemoteSessionTitle(conversationId)
    .then((res) => {
      const aiTitle = String(res?.data || '').trim();
      if (aiTitle && !isDefaultSessionTitle(aiTitle)) {
        sess.title = aiTitle;
      }
    })
    .catch(() => {});
}

export async function streamAssistantChat(
  sseClient: SSEClient,
  query: string,
  courseId: number,
  conversationId: string,
  options: StreamOptions | undefined,
  handlers: StreamEventHandlers,
  streamingContentRef: { value: string }
): Promise<boolean> {
  return runCopilotSseStream(
    sseClient,
    `${API_BASE_URL}/ai/chat/stream`,
    {
      message: query,
      courseId,
      chapterId: options?.chapterId,
      lessonChapterId: options?.lessonChapterId,
      conversationId: conversationId || undefined,
      modelKey: resolveStreamModelKey(options),
      useRag: options?.useRag ?? true,
      regenerate: options?.isRegenerate === true,
      webSearch: options?.webSearch === true,
      attachmentIds: options?.attachmentIds
    },
    streamingContentRef,
    {
      isStopped: handlers.isStopped,
      onStreamId: handlers.onStreamId,
      onStatus: handlers.onStatus,
      onReasoningChunk: handlers.onReasoningChunk,
      onDeltaChunk: handlers.onDeltaChunk,
      onCitations: handlers.onCitations,
      onMemory: handlers.onMemory,
      onDone: (data) => handlers.onDone?.(data as StreamDonePayload),
      onFollowOutput: handlers.onFollowOutput,
      defaultErrorMessage: '流式输出服务异常'
    }
  );
}

export async function fallbackAskAssistant(query: string, courseId: number, conversationId: string) {
  const res = await askGlobalAssistant({
    message: query,
    courseId,
    conversationId
  });
  const data = res?.data;
  if (!data) throw new Error('AI 服务暂未返回有效数据');
  return {
    content: data.content || '已为你检索并梳理该课程要点。',
    reasoningContent: data.reasoningContent as string | undefined,
    citations: (data.citations as CitationItem[] | undefined) || []
  };
}

export const aiStreamService = {
  resolveStreamModelKey,
  isDefaultSessionTitle,
  persistMessageCache,
  readMessageCache,
  clearMessageCache,
  mergeServerWithLocalDrafts,
  getStoredSessionId,
  storeSessionId,
  clearStoredSessionId,
  buildSessionTitleFromPrompt,
  generateSmartFollowUps,
  mapSession,
  mapMessage,
  fetchConversationList,
  fetchMessageList,
  createRemoteSession,
  renameRemoteSession,
  deleteRemoteSession,
  deleteRemoteMessage,
  syncSessionTitleIfDefault,
  streamAssistantChat,
  fallbackAskAssistant
};
