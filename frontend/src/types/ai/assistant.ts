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

export interface GlobalAssistantSession {
  id: string;
  title: string;
  updatedAt: number | string;
  messageCount?: number;
  messages?: GlobalAssistantMessage[];
}

