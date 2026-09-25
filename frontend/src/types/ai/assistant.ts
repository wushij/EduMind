import type { LessonInsertIntent } from '@/utils/ai/lesson-copilot-intent';

/** RAG 引用切片（课程 AI / 全局助手 / 知识库溯源共用） */
export interface CitationItem {
  id?: number | string;
  docTitle?: string;
  documentName?: string;
  page?: number;
  pageNo?: number;
  chunkId?: number | string;
  chunkIndex?: number;
  snippet?: string;
  excerpt?: string;
  content?: string;
  score?: number;
  lessonChapterId?: number;
  anchor?: string;
  documentId?: number;
  knowledgeBaseId?: number;
}

export interface GlobalAssistantMessage {
  id?: string | number;
  role: 'user' | 'assistant';
  content: string;
  reasoningContent?: string;
  reasoningFolded?: boolean;
  isReasoningActive?: boolean;
  streamPhaseMessage?: string;
  intent?: string;
  intentDesc?: string;
  targetCode?: string;
  streaming?: boolean;
  citations?: CitationItem[];
  followUpPrompts?: string[];
  createdAt?: string | number;
  error?: boolean;
  /** 课节备课：发起本条提问时锁定的插入目标（对齐 Code Compass 按轮次判定） */
  lessonInsertIntent?: LessonInsertIntent;
}

export interface GlobalAssistantChatRequest {
  message: string;
  courseId?: number;
  conversationId?: string;
  contextModule?: string;
  lessonChapterId?: number;
  selectedText?: string;
  draftExcerpt?: string;
  draftTitle?: string;
  draftDescription?: string;
  objectiveExcerpt?: string;
  questionId?: number;
  modelKey?: string;
  promptPrefix?: string;
}

export interface GlobalAssistantAskResponse {
  conversationId: string;
  intent: string;
  intentDesc: string;
  targetCode?: string;
  content: string;
  reasoningContent?: string;
  citations?: CitationItem[];
}

export interface GlobalAssistantIntentEvent {
  route?: string;
  agentCode?: string;
  confidence?: number;
  slots?: Record<string, unknown>;
}

/** 追问建议生成入参：以本轮真实问答内容为依据，由模型现生成「下一步追问」 */
export interface FollowUpSuggestRequest {
  question: string;
  answer: string;
  /** 可选场景上下文（课程名 / 课节标题 / 题目考点） */
  context?: string;
  /**
   * 本轮问答所属课程 ID。
   *
   * <p>追问属于该课程的 AI 消耗：不带课程号时这条调用不归属任何课程，
   * 会被课程维度的调用明细过滤掉（表现为「课程里用了 AI 追问，消耗明细查不到」）。</p>
   */
  courseId?: number;
  /** 生成条数，默认 3，上限 5 */
  count?: number;
  modelKey?: string;
}

export interface FollowUpSuggestResponse {
  prompts: string[];
  /** false 表示模型未返回可用追问（网关抖动 / 输出不合法），前端应回落规则生成 */
  aiGenerated: boolean;
  sourceLabel?: string;
}

export interface GlobalAssistantSession {
  id: string;
  title: string;
  /** true 表示标题已由模型生成/用户改过，后续保存不得再用「提问前缀」覆盖 */
  titleFromAi?: boolean;
  updatedAt: number | string;
  messageCount?: number;
  messages?: GlobalAssistantMessage[];
}

