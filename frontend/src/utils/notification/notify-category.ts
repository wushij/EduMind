import type { NotificationCategory, NotificationType } from '@/types/notification';

export interface NotifyTypeMeta {
  label: string;
  cssClass: string;
  metaTag: string;
}

const TYPE_META: Record<string, NotifyTypeMeta> = {
  SYSTEM: { label: '系统通知', cssClass: 'type-system', metaTag: '平台消息' },
  BROADCAST: { label: '系统广播', cssClass: 'type-system', metaTag: '广播消息' },
  COURSE: { label: '课程动态', cssClass: 'type-course', metaTag: '教学提醒' },
  ASSIGNMENT: { label: '作业提醒', cssClass: 'type-assignment', metaTag: '作业任务' },
  EXAM: { label: '考试通知', cssClass: 'type-exam', metaTag: '考试安排' },
  KNOWLEDGE_INDEX: { label: '知识库', cssClass: 'type-knowledge', metaTag: '索引状态' },
  AI_TASK: { label: 'AI 任务', cssClass: 'type-ai', metaTag: '智能助手' }
};

export function getNotifyTypeMeta(type: NotificationType): NotifyTypeMeta {
  return TYPE_META[type] || { label: '系统通知', cssClass: 'type-system', metaTag: '平台消息' };
}

export function getNotifyTypeClass(type: NotificationType): string {
  return getNotifyTypeMeta(type).cssClass;
}

export const NOTIFY_CATEGORIES: { key: NotificationCategory; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'system', label: '系统' },
  { key: 'teaching', label: '教学' },
  { key: 'knowledge', label: '知识库' },
  { key: 'ai', label: 'AI' }
];

const TEACHING_TYPES = new Set(['COURSE', 'ASSIGNMENT', 'EXAM']);

export function matchesNotifyCategory(type: NotificationType, category: NotificationCategory): boolean {
  if (category === 'all') return true;
  if (category === 'system') return type === 'SYSTEM' || type === 'BROADCAST';
  if (category === 'teaching') return TEACHING_TYPES.has(type);
  if (category === 'knowledge') return type === 'KNOWLEDGE_INDEX';
  if (category === 'ai') return type === 'AI_TASK';
  return true;
}

export function getCategoryEmptyText(category: NotificationCategory): string {
  if (category === 'all') return '暂无消息通知';
  const found = NOTIFY_CATEGORIES.find((c) => c.key === category);
  return found ? `暂无「${found.label}」类消息` : '暂无消息通知';
}
