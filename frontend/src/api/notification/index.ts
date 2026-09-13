import { get, put, del } from '@/core/http/request';
import type {
  NotificationListResult,
  NotificationUnreadCount,
  NotificationVO,
  NotificationCategory
} from '@/types/notification';

/** 分页获取通知列表 */
export function listNotifications(params: {
  page?: number;
  pageSize?: number;
  category?: NotificationCategory;
}) {
  return get<NotificationListResult>('/notifications/list', params);
}

/** 获取未读数量 */
export function getUnreadCount() {
  return get<NotificationUnreadCount>('/notifications/unread-count');
}

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

/** 删除单条通知 */
export function deleteNotification(id: number) {
  return del<void>(`/notifications/${id}`);
}

/** 清空所有通知 */
export function clearAllNotifications() {
  return del<void>('/notifications/clear-all');
}
