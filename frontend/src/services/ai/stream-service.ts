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

export function generateSmartFollowUps(query: string): string[] {
  const cleanQuery = query.replace(/^\[[^\]]+\]\s*/g, '').trim().toLowerCase();
  const q = cleanQuery || query.toLowerCase();

  if (/出题|题目|试题|试卷|考题|做题|作业|考点|数列|函数解析|数学|难题|随堂|真题|选择题|填空题|大题|解答题|难度系数/.test(q)) {
    return [
      '根据此题型衍生 3 道同等难度的变式训练题',
      '导出试题解析、踩分点与评分细则标准',
      '分析这道题目考察的底层核心知识点与易错陷阱'
    ];
  }

  if (/写代码|代码实现|写一个函数|写个类|报错|bug|debug|堆栈|异常处理|spring boot|vue|typescript|sql查询/.test(q) || (/代码|编程/.test(q) && /写|看|改|实现|重构/.test(q))) {
    return [
      '给出此逻辑的完整工程优化版代码实现',
      '分析这段代码可能存在的边界隐患与异常处理',
      '结合设计模式重构此核心业务逻辑'
    ];
  }

  if (/概念|原理|是什么|区别|体系|架构|为什么/.test(q)) {
    return [
      '用通俗生动的比喻进一步解释底层原理',
      '用对比表格列出它与关联知识的关键异同',
      '结合当前课程知识大纲展开前驱依赖与后继拓展'
    ];
  }

  if (/算法|数据结构|复杂度|时间复杂度|空间复杂度|渐进表示|递归|动态规划|贪心/.test(q)) {
    return [
      '分析该算法在最好、最坏与平均情况下的时空复杂度',
      '用简明步骤图解该算法的执行推演过程',
      '对比该算法与其他替代算法的性能优缺点'
    ];
  }

  return [
    '用更通俗生动的教学案例进一步解释',
    '为该考点出 2 道课堂即兴互动提问',
    '结合实际工程案例列举具体应用场景'
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
      regenerate: options?.isRegenerate === true
    },
    streamingContentRef,
    {
      isStopped: handlers.isStopped,
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
