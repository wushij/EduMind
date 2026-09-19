import type { LessonInsertIntent } from '@/utils/ai/lesson-copilot-intent';

export type TeachingCopilotContextModule =
  | 'lesson_studio'
  | 'lesson_learn'
  | 'course_space'
  | 'question_bank'
  | 'global';

export interface TeachingCopilotContext {
  contextModule: TeachingCopilotContextModule;
  /** 课节备课侧栏/菜单触发的插入意图，优先于回复内容猜测 */
  lessonInsertIntent?: LessonInsertIntent;
  courseId?: number;
  lessonChapterId?: number;
  /** 题库题目辅导锚定 */
  questionId?: number;
  title?: string;
  /** 题干摘要等，用于 RAG 检索 query */
  draftTitle?: string;
  description?: string;
  lessonType?: string;
  wordCount?: number;
  contentStatus?: string;
  selectedText?: string;
  draftExcerpt?: string;
  objectiveExcerpt?: string;
}

export function buildTeachingContextRequestPayload(
  ctx: TeachingCopilotContext | null
): Partial<import('@/types/ai/assistant').GlobalAssistantChatRequest> {
  if (!ctx) return {};
  return {
    contextModule: ctx.contextModule,
    lessonChapterId: ctx.lessonChapterId,
    questionId: ctx.questionId,
    selectedText: ctx.selectedText,
    draftExcerpt: ctx.draftExcerpt,
    draftTitle: ctx.draftTitle || ctx.title,
    draftDescription: ctx.description,
    objectiveExcerpt: ctx.objectiveExcerpt
  };
}
