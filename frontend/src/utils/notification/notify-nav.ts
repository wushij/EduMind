import type { NotificationType } from '@/types/notification';

/**
 * 根据通知类型与关联 ID 推断跳转路径
 */
export function getNotificationNavigatePath(type: NotificationType, refId?: number): string | null {
  if (!refId) return null;
  switch (type) {
    case 'KNOWLEDGE_INDEX':
      return `/knowledge/${refId}`;
    case 'COURSE':
      return `/course/${refId}/overview`;
    case 'ASSIGNMENT':
      return `/question/assignments/${refId}`;
    case 'EXAM':
      return `/question/exams/${refId}`;
    case 'AI_TASK':
      return '/course/ai';
    default:
      return null;
  }
}
