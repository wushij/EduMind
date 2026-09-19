import type { LearningPathTaskStatus, LearningPathTaskType } from '@/types/learning/learning-path';

export const LEARNING_PATH_TASK_TYPE_LABEL: Record<string, string> = {
  READ: '章节学习',
  PRACTICE: '巩固练习',
  AI_CHAT: 'AI 讲解',
  WRONG_BOOK: '错题攻坚',
  ASSIGNMENT: '课程作业',
  RESOURCE: '学习资料'
};

export const LEARNING_PATH_TASK_STATUS_LABEL: Record<LearningPathTaskStatus, string> = {
  PENDING: '待完成',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成'
};

export function resolvePathTaskTypeLabel(type: string): string {
  return LEARNING_PATH_TASK_TYPE_LABEL[type] || type;
}

export function resolvePathTaskStatusLabel(status: string): string {
  const key = status as LearningPathTaskStatus;
  return LEARNING_PATH_TASK_STATUS_LABEL[key] || status;
}
