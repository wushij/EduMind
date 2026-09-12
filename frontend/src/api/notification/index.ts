import { get, put } from '@/core/http/request';
import type { NotificationVO } from '@/types/notification';

/** 获取未读通知列表 */
export function getUnreadNotifications() {
  return get<NotificationVO[]>('/notifications');
}

/** 获取所有通知列表 */
export function getAllNotifications() {
  return get<NotificationVO[]>('/notifications/all');
}

/** 标记单个通知为已读 */
export function markNotificationAsRead(id: number) {
  return put<void>(`/notifications/${id}/read`);
}

/** 标记所有通知为已读 */
export function markAllNotificationsAsRead() {
  return put<void>('/notifications/read-all');
}
