import type { PageResult } from '@/types/common/api';

export type NotificationCategory = 'all' | 'system' | 'teaching' | 'knowledge' | 'ai';

export type NotificationType =
  | 'SYSTEM'
  | 'BROADCAST'
  | 'COURSE'
  | 'ASSIGNMENT'
  | 'EXAM'
  | 'KNOWLEDGE_INDEX'
  | 'AI_TASK'
  | string;

export interface NotificationVO {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: NotificationType;
  refId?: number;
  priority?: number;
  isRead: number;
  createTime: string;
}

export interface NotificationListResult {
  unreadCount: number;
  totalCount: number;
  list: PageResult<NotificationVO>;
}

export interface NotificationUnreadCount {
  unreadCount: number;
  totalCount: number;
}

export interface NotificationPushPayload {
  id: number;
  type: NotificationType;
  title: string;
  content: string;
  refId?: number;
  priority?: number;
  isRead: number;
  createTime: string;
  unreadCount: number;
}
