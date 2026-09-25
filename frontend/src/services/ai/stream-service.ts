import { SSEClient } from '@/core/sse/client';
import { API_BASE_URL } from '@/config';
import { storage } from '@/core/storage/local';
import { askGlobalAssistant, suggestFollowUps } from '@/api/ai/assistant';
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
  sectionTitle?: string;
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

function getSessionStorageKey(courseId?: number): string {
  if (!courseId) return '';
  const currentUserId = storage.get('edumind_user_id') || '';
  return currentUserId ? `${SESSION_STORAGE_PREFIX}${currentUserId}:${courseId}` : `${SESSION_STORAGE_PREFIX}${courseId}`;
}

export function getStoredSessionId(courseId?: number): string {
  if (!courseId) return '';
  const key = getSessionStorageKey(courseId);
  const stored = storage.get(key);
  if (typeof stored === 'string' && stored) return stored;
  const legacyStored = storage.get(`${SESSION_STORAGE_PREFIX}${courseId}`);
  return typeof legacyStored === 'string' ? legacyStored : '';
}

export function storeSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  storage.set(getSessionStorageKey(courseId), sessionId);
}

export function clearStoredSessionId(courseId: number | undefined, sessionId: string) {
  if (!courseId || !sessionId) return;
  const key = getSessionStorageKey(courseId);
  if (storage.get(key) === sessionId) {
    storage.remove(key);
  }
  if (storage.get(`${SESSION_STORAGE_PREFIX}${courseId}`) === sessionId) {
    storage.remove(`${SESSION_STORAGE_PREFIX}${courseId}`);
  }
}

/** 兜底标题：去掉模板前缀与装饰括号，并压到 14 字以内（历史列表里 20 字太长、不像标题） */
export function buildSessionTitleFromPrompt(text: string): string {
  const cleaned = text
    .replace(/^\[当前知识锚定章节:[^\]]+\]\s*/i, '')
    .replace(/^\[当前章节:[^\]]+\]\s*/i, '')
    .replace(/^\[当前微课节ID:[^\]]+\]\s*/i, '')
    .replace(/[「」『』“”"'《》【】]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return cleaned.slice(0, 14) || '新问答会话';
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

/** 追问请求的可选参数：onUpdate 用于异步回写，其余用于放宽 / 收紧生成门槛 */
export interface FollowUpRequestOptions extends FollowUpOptions {
  onUpdate?: (prompts: string[]) => void;
  /** 材料长度门槛：低于该长度直接不展示（默认 80，避免寒暄、报错占位生成无意义追问） */
  minMaterialChars?: number;
  /** 期望条数，默认 3，上限 5（后端同样封顶 5） */
  count?: number;
}

/** 章节结构词：本身不承载知识点，不能拿来当追问锚点 */
const TOPIC_NOISE_PATTERN =
  /^(总结|小结|概述|摘要|引言|前言|目录|思考|思考题|自测|建议|注意|提示|说明|例如|举例|关键|结论|分析|核心要点|总体|底层原理|运行原理|运行机制|方法选择|统一解题流程|最核心的底层逻辑|易错点|延伸|拓展|背景|目标|学习目标|重点|难点|作业|练习|随堂|本节|本章|上文|下文|含义|定义|性质|分类|流程|步骤|为什么|是什么|怎么做|有什么区别|怎么用)$/;

/** 以代词 / 连接词开头的片段是叙述句，不是知识点（如「它有没有一个稳定目标」） */
const TOPIC_SENTENCE_HEAD_PATTERN =
  /^(它|他|她|这|那|该|此|其|我|你|我们|你们|这些|那些|上述|以上|以下|下面|其中|同时|首先|其次|再次|最后|因此|所以|但是|如果|只要|只有|由于|因为|例如|比如|假设|可见|总之|综上|那么|若|则)/;

/** 疑问结尾的片段是问题描述，套进任何句式都会变成驴唇不对马嘴的追问 */
const TOPIC_QUESTION_TAIL_PATTERN = /[？?]$|吗$|呢$|吧$/;

function splitTopicCandidates(raw: string): string[] {
  return raw
    .replace(/[*#`~[\]()（）“”"'「」『』《》【】]/g, '')
    .split(/[：:，。；;！!]+/)
    .map((segment) => segment.trim())
    .filter((segment) => segment.length > 0);
}

function normalizeTopic(segment: string): string {
  return segment
    .replace(/^[0-9一二三四五六七八九十]+[.、:：\s\-—]*/, '')
    .replace(/(详细解析|深入分析|核心原理|总体架构|全景概述|主要内容|基本概念|简介)$/, '')
    .replace(/[，。；、]+$/, '')
    .trim();
}

function isUsableTopic(text: string): boolean {
  if (text.length < 2 || text.length > 20) return false;
  if (TOPIC_NOISE_PATTERN.test(text)) return false;
  if (TOPIC_SENTENCE_HEAD_PATTERN.test(text)) return false;
  if (TOPIC_QUESTION_TAIL_PATTERN.test(text)) return false;
  if (/[，。；？?]/.test(text)) return false;
  return true;
}

/**
 * 从用户提问与助手回答中提取「可复用的知识锚点」。
 *
 * <p>锚点必须像一个能在下一轮被指代的名词：优先取小标题里冒号前的知识点名
 * （「数列极限：ε-N 是一场误差合同」→「数列极限」），其次取加粗术语。
 * 叙述句（代词开头、问号结尾、带逗号从句）一律丢弃——否则会出现
 * 「深入剖析『它有没有一个稳定目标？』的底层工作原理」这类荒唐追问。</p>
 */
export function extractKeyTopicsFromContent(query: string, answer?: string): string[] {
  const topics: string[] = [];
  const seen = new Set<string>();

  const addTopic = (raw: string) => {
    // 一个标题 / 加粗片段只产出一个锚点：取第一段能当知识点的片段
    for (const candidate of splitTopicCandidates(raw)) {
      const cleaned = normalizeTopic(candidate);
      if (!isUsableTopic(cleaned)) continue;
      const lower = cleaned.toLowerCase();
      if (!seen.has(lower)) {
        seen.add(lower);
        topics.push(cleaned);
      }
      return;
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

type FollowUpDomain = 'math' | 'code' | 'generic';

/** 学科信号词：用于挑选「符合本学科语境」的提问句式（数学课不该出现「工业级代码示例」） */
const MATH_SIGNALS =
  /极限|数列|收敛|有界|导数|微分|积分|定理|推论|证明|推导|公式|方程|不等式|矩阵|向量|行列式|概率|统计|洛必达|夹逼|无穷小|无穷大|未定式|单调|保号|左右极限|函数极限|三角函数|多项式|对数|指数|求导|解析几何|复数|级数|ε/g;
const CODE_SIGNALS =
  /```|代码|编译|报错|异常|堆栈|接口|数据库|SQL|索引|并发|线程|锁|缓存|部署|重构|单元测试|日志|前端|后端|框架|依赖|类名|算法|递归|动态规划|数组|链表|二叉树|哈希|时间复杂度|空间复杂度|Java|Python|TypeScript|Vue|React|Spring|MyBatis|Redis|Linux|Git|API/g;

function detectFollowUpDomain(query: string, answer: string): FollowUpDomain {
  const sample = `${query}\n${answer}`.slice(0, 3000);
  const mathHits = (sample.match(MATH_SIGNALS) || []).length;
  const codeHits = (sample.match(CODE_SIGNALS) || []).length;
  // 有代码块时按代码处理；否则谁命中多信谁，「函数」这类两栖词不计入任何一方
  if (answer.includes('```') && codeHits >= mathHits) return 'code';
  if (mathHits >= 2 && mathHits > codeHits) return 'math';
  if (codeHits >= 2 && codeHits > mathHits) return 'code';
  return 'generic';
}

/** 有真实锚点时：三条追问分别指向推导、易错 / 反例、用法自测，且句句带具体内容 */
function buildAnchoredFollowUps(domain: FollowUpDomain, anchors: [string, string, string]): string[] {
  const [t1, t2, t3] = anchors;
  // 刻意不加「」这类括号：追问胶囊里读起来像机器模板，直接写术语本身更像学生提问
  if (domain === 'math') {
    return [
      `${t1}是怎么推导出来的，能补上关键一步吗？`,
      `${t2}最容易错在哪里，有反例吗？`,
      `用${t3}做题时，第一步该判断什么？`
    ];
  }
  if (domain === 'code') {
    return [
      `${t1}在真实项目里最容易踩的坑是什么？`,
      `${t2}和${t1}有什么区别，该怎么选？`,
      `能用一个最小可运行示例演示${t3}吗？`
    ];
  }
  return [
    `${t1}能再举一个更直观的例子吗？`,
    `${t2}最容易混淆的地方是什么？`,
    `围绕${t3}给我出 2 道自测题`
  ];
}

/** 提不出锚点时：仍按学科给出可用的追问方向，但不再硬塞「企业级 / 代码示例」这类错位说法 */
function buildContextFollowUps(domain: FollowUpDomain, sectionTitle?: string): string[] {
  // 与锚点模板一致：不加「」这类装饰括号，避免看起来像机器模板
  const spot = sectionTitle ? `本节内容（${sectionTitle}）` : '这一节';
  if (domain === 'math') {
    return [
      `${spot}里最容易混淆的两个定义是什么？`,
      '这类题的完整步骤能按「先判断什么、再做什么」再梳理一遍吗？',
      '给我 2 道同等难度的变式题练手'
    ];
  }
  if (domain === 'code') {
    return [
      `${spot}里最容易踩的坑是什么？`,
      '这段逻辑的健壮性与边界情况还能怎么补强？',
      '结合一个真实项目场景说明它该怎么落地'
    ];
  }
  return [
    `${spot}的重点内容能再完整梳理一遍吗？`,
    '这里最容易混淆的概念是什么？',
    '围绕这一节内容给我出 2 道自测题'
  ];
}

/**
 * 规则兜底追问。
 *
 * <p>仅在「模型没返回可用结果」时使用（见 {@link requestFollowUps}），因此这里的目标
 * 不是替代模型，而是保证降级时也不会出现与学科语境的错位提问。</p>
 */
export function generateSmartFollowUps(
  query: string,
  answer?: string,
  options?: FollowUpOptions
): string[] {
  const cleanQuery = query.replace(/^\[[^\]]+\]\s*/g, '').trim();
  const body = (answer || '').trim();
  const topics = extractKeyTopicsFromContent(cleanQuery, body);
  const domain = detectFollowUpDomain(cleanQuery, body);

  if (topics.length >= 2) {
    return buildAnchoredFollowUps(domain, [topics[0], topics[1], topics[2] || topics[0]]);
  }
  if (topics.length === 1) {
    return buildAnchoredFollowUps(domain, [topics[0], topics[0], topics[0]]);
  }

  // 提不出锚点时，题目 / 考点类对话最能受益于固定方向的提问
  const sectionTitle = options?.sectionTitle?.trim();
  if (/出题|题目|试题|试卷|考题|做题|作业|考点|随堂|真题|选择题|填空题|大题|解答题|变式|难度/.test(cleanQuery)) {
    const spot = sectionTitle ? `「${sectionTitle}」` : '';
    return [
      `这道题考查的核心考点${spot}能再归纳一下吗？`,
      '给我 2 道同等难度的变式题练手',
      '这类题在考试里最容易在哪儿失分？'
    ];
  }
  return buildContextFollowUps(domain, sectionTitle);
}

/**
 * 追问缓存版本号：净化规则 / 提示词变更后**必须递增**。
 *
 * <p>否则旧结果会被缓存原样回放，表现就是「代码已经改了、界面还是老样子」——
 * v1 的缓存里存着被错误剥掉小节号的结果（1.1 → 1），所以升到 v2 并顺手清掉 v1。</p>
 */
const FOLLOW_UP_CACHE_VERSION = 'v2';
const FOLLOW_UP_CACHE_STORAGE_KEY = `edumind:ai:follow-up-cache-${FOLLOW_UP_CACHE_VERSION}`;
const LEGACY_FOLLOW_UP_CACHE_KEYS = ['edumind:ai:follow-up-cache'];
let legacyFollowUpCachePurged = false;
const FOLLOW_UP_CACHE_LIMIT = 80;
const FOLLOW_UP_REQUEST_TIMEOUT_MS = 12000;
/** 回答短于该长度（报错占位、寒暄）不足以支撑内容化追问，直接不展示胶囊 */
const FOLLOW_UP_MIN_ANSWER_CHARS = 80;
const FOLLOW_UP_PROMPT_COUNT = 3;
const FOLLOW_UP_MAX_PROMPT_CHARS = 40;

/** 与后端 FollowUpSuggestServiceImpl 的套话黑名单保持一致：模型偷懒时不暴露到 UI */
const FOLLOW_UP_GENERIC_PATTERN =
  /还有什么(想|需要)|需要我(进一步|继续|再|展开)|希望(这些|以上|对你有帮助)|如需(更多|进一步)|欢迎(继续|随时)|还有(什么)?(疑问|问题吗)|你想(了解|深入|先看)哪|想(先|深入)?了解哪|上述(回答|内容|讲解)|上文|本文|以上(内容|回答)|^好的[，,。]|^当然|以下(是|为)|如下[：:]|建议(的)?(追问|问题)|希望对你/;

/**
 * 追问请求序号：模型响应是异步到达的，用户很可能已经进入下一轮或切换会话。
 * 只允许「最后一次发起」的响应回写 UI，避免旧轮次的追问顶掉当前轮次。
 */
let followUpRequestSeq = 0;

function followUpCacheKey(question: string, answer: string): string {
  const raw = `${question.trim()}::${answer.trim()}`;
  let hash = 5381;
  for (let i = 0; i < raw.length; i += 1) {
    hash = ((hash << 5) + hash + raw.charCodeAt(i)) | 0;
  }
  return `q${(hash >>> 0).toString(36)}_${raw.length}`;
}

function purgeLegacyFollowUpCache(): void {
  if (legacyFollowUpCachePurged) return;
  legacyFollowUpCachePurged = true;
  LEGACY_FOLLOW_UP_CACHE_KEYS.forEach((key) => storage.remove(key));
}

function readFollowUpCache(): Record<string, string[]> {
  purgeLegacyFollowUpCache();
  try {
    const cached = storage.get(FOLLOW_UP_CACHE_STORAGE_KEY);
    return cached && typeof cached === 'object' ? (cached as Record<string, string[]>) : {};
  } catch {
    return {};
  }
}

function writeFollowUpCache(key: string, prompts: string[]): void {
  try {
    const cache = readFollowUpCache();
    cache[key] = prompts;
    const keys = Object.keys(cache);
    if (keys.length > FOLLOW_UP_CACHE_LIMIT) {
      keys.slice(0, keys.length - FOLLOW_UP_CACHE_LIMIT).forEach((stale) => delete cache[stale]);
    }
    storage.set(FOLLOW_UP_CACHE_STORAGE_KEY, cache);
  } catch {
    // 本地缓存写入失败不影响追问展示
  }
}

/** 读取「同一轮问答」已生成过的追问：命中后切会话/刷新页面都不再重复调用模型 */
export function readCachedFollowUps(question: string, answer: string): string[] | null {
  if (!question.trim() || !answer.trim()) return null;
  const cached = readFollowUpCache()[followUpCacheKey(question, answer)];
  return Array.isArray(cached) && cached.length > 0 ? [...cached] : null;
}

/**
 * 把被挤在同一行的多条追问拆开（模型偶尔会把「1. 甲？2. 乙？3. 丙？」写成一行）。
 *
 * <p>后端已做同样处理，这里再兜一层：万一接口返回的是旧版实现（整串一行），
 * 前端也不会因为「单条超长」把整批追问丢干净。刻意不用正则后行断言，避免老浏览器解析期报错。</p>
 */
function splitMergedFollowUpLines(text: string): string[] {
  // (?!\d)：小节号「1.1」不能被当成切分点，否则「什么是 1.1 算法复杂度…」会被从中间切开
  const marked = text.replace(/([？?。！!\s])(\d{1,2}\s*[.、)）:：](?!\d))/g, '$1\u0000$2');
  const segments: string[] = [];
  for (const line of marked.split(/\r?\n/)) {
    for (const part of line.split('\u0000')) {
      const trimmed = part.trim();
      if (!trimmed) continue;
      const marks = trimmed.match(/[？?]/g);
      if (trimmed.length > FOLLOW_UP_MAX_PROMPT_CHARS && marks && marks.length >= 2) {
        let current = '';
        for (const ch of trimmed) {
          current += ch;
          if (ch === '？' || ch === '?') {
            segments.push(current.trim());
            current = '';
          }
        }
        if (current.trim()) segments.push(current.trim());
      } else {
        segments.push(trimmed);
      }
    }
  }
  return segments;
}

/** 净化模型输出：剥离编号 / Markdown 残留，剔除套话、超长与重复条目 */
export function normalizeFollowUpPrompts(raw: unknown, max = FOLLOW_UP_PROMPT_COUNT): string[] {
  if (!Array.isArray(raw)) return [];
  const flattened: string[] = [];
  for (const item of raw) {
    if (typeof item === 'string') flattened.push(...splitMergedFollowUpLines(item));
  }

  const result: string[] = [];
  const seen = new Set<string>();
  for (const item of flattened) {
    const text = item
      // (?!\d) 不可省：内容里的小节号「1.1 算法复杂度…」不能被当成列表编号剥掉，否则会变成「1算法复杂度…」
      .replace(/^\s*(?:[-*•·]+|\d{1,2}\s*[.、)）:：](?!\d)|[（(]\d{1,2}[)）])\s*/, '')
      .replace(/[*`_#>]/g, '')
      .replace(/^["'“”‘’「」《》【】]+|["'“”‘’「」《》【】]+$/g, '')
      .trim();
    if (text.length < 4 || text.length > FOLLOW_UP_MAX_PROMPT_CHARS) continue;
    if (FOLLOW_UP_GENERIC_PATTERN.test(text)) continue;
    const key = text.replace(/[\s，。！？、；：（）【】「」《》“”‘’]/g, '').toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    result.push(text);
    if (result.length >= max) break;
  }
  return result;
}

/** 长回答保留头尾：开头是结论主线，结尾是易错点与总结，掐掉结尾会明显拉低追问质量 */
function clipAnswerForFollowUp(answer: string): string {
  const trimmed = answer.trim();
  if (trimmed.length <= 6000) return trimmed;
  return `${trimmed.slice(0, 4000)}\n…（中间内容已省略）…\n${trimmed.slice(-2000)}`;
}

function warnFollowUpDegrade(error: unknown): void {
  if (import.meta.env.DEV) {
    console.warn(
      '[follow-ups] 模型追问生成失败，已回落到本地规则；若长期出现请检查后端是否已重启、POST /api/ai/assistant/follow-ups 是否 200',
      error
    );
  }
}

/**
 * 追问主入口（全局助手 / 课程 AI / 题目辅导共用）。
 *
 * <p>模型没返回之前一律不展示规则模板：规则模板句式固定、且难以贴合学科语境
 * （高数课会出现「企业级业务避坑」「工业级代码示例」），先闪出来既误导用户也掩盖了
 * 模型链路故障。因此这里的策略是「模型优先、失败才降级」：</p>
 * <ul>
 *   <li>命中本地缓存 → 直接返回（同一轮问答不重复调模型）；</li>
 *   <li>调用方没有 onUpdate（纯同步场景）→ 返回规则结果；</li>
 *   <li>否则发起模型请求，成功回写模型追问，失败 / 超时 / 输出不合法时回写规则结果。</li>
 * </ul>
 */
export function requestFollowUps(
  question: string,
  answer: string,
  options?: FollowUpRequestOptions
): string[] {
  const cleanQuestion = (question || '').trim();
  const cleanAnswer = (answer || '').trim();
  const minMaterialChars = options?.minMaterialChars ?? FOLLOW_UP_MIN_ANSWER_CHARS;
  if (!cleanQuestion || cleanAnswer.length < minMaterialChars) {
    return [];
  }

  const cached = readCachedFollowUps(cleanQuestion, cleanAnswer);
  if (cached) return cached;

  const fallback = () => generateSmartFollowUps(cleanQuestion, cleanAnswer, options);
  if (!options?.onUpdate) {
    return fallback();
  }

  const wanted = Math.min(Math.max(options?.count ?? FOLLOW_UP_PROMPT_COUNT, 1), 5);
  const cacheKey = followUpCacheKey(cleanQuestion, cleanAnswer);
  const seq = (followUpRequestSeq += 1);
  const context = [options.courseTitle, options.sectionTitle].filter(Boolean).join(' · ');
  void suggestFollowUps(
    {
      question: cleanQuestion.slice(0, 1000),
      answer: clipAnswerForFollowUp(cleanAnswer),
      ...(context ? { context } : {}),
      count: wanted
    },
    { silent: true, timeout: FOLLOW_UP_REQUEST_TIMEOUT_MS }
  )
    .then((res) => {
      if (seq !== followUpRequestSeq) return;
      const prompts = res?.data?.aiGenerated ? normalizeFollowUpPrompts(res.data.prompts, wanted) : [];
      if (prompts.length === 0) {
        options.onUpdate?.(fallback());
        return;
      }
      writeFollowUpCache(cacheKey, prompts);
      options.onUpdate?.(prompts);
    })
    .catch((error: unknown) => {
      if (seq !== followUpRequestSeq) return;
      warnFollowUpDegrade(error);
      options.onUpdate?.(fallback());
    });

  return [];
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
  sessions: ChatSession[] | (() => ChatSession[]),
  conversationId: string,
  messages: ChatMessage[],
  promptHint?: string,
  options?: { tryLlmTitle?: boolean }
): Promise<void> {
  if (!conversationId) return;
  // 会话列表在刷新时会被整体替换，因此统一用 getter 读「当前数组」，避免把标题写到已被丢弃的旧数组上
  const readSessions = typeof sessions === 'function' ? sessions : () => sessions;
  const prompt = promptHint || findFirstUserPrompt(messages);

  if (prompt) {
    const fallbackTitle = buildSessionTitleFromPrompt(prompt);
    const sess = readSessions().find((s) => s.id === conversationId);
    // 仅当本地标题仍是默认标题时才回落到「提问前缀」并改名：
    // 否则会把已经生成好的 AI 标题重新覆盖成长提问，历史列表就越看越长
    if (!isDefaultSessionTitle(fallbackTitle) && (!sess || isDefaultSessionTitle(sess.title))) {
      if (sess) sess.title = fallbackTitle;
      try {
        await renameRemoteSession(conversationId, fallbackTitle);
      } catch {
        // keep local title
      }
    }
  }

  if (!options?.tryLlmTitle) return;
  void generateRemoteSessionTitle(conversationId)
    .then((res) => {
      const aiTitle = String(res?.data || '').trim();
      if (!aiTitle || isDefaultSessionTitle(aiTitle)) return;
      const target = readSessions().find((s) => s.id === conversationId);
      if (target) target.title = aiTitle;
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
      sectionTitle: options?.sectionTitle || options?.activeSectionTitle,
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
  requestFollowUps,
  readCachedFollowUps,
  normalizeFollowUpPrompts,
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
