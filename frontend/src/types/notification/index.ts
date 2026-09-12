export interface NotificationVO {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: 'SYSTEM' | 'COURSE' | 'AI_TASK' | 'EXAM' | string;
  isRead: number; // 0: unread, 1: read
  createTime: string;
}
