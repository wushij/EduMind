import { del, get, post } from '@/core/http/request';
import type {
  BroadcastCreateRequest,
  BroadcastEstimateVO,
  BroadcastListResult,
  BroadcastStatsVO,
  NotificationBroadcastVO
} from '@/types/notification/broadcast';

export function listBroadcasts(params: {
  page?: number;
  pageSize?: number;
  targetType?: string;
}) {
  return get<BroadcastListResult>('/notifications/broadcast/list', params);
}

export function getBroadcastStats() {
  return get<BroadcastStatsVO>('/notifications/broadcast/stats');
}

export function estimateBroadcastAudience(params: {
  targetType: string;
  targetPayload?: string;
}) {
  return get<BroadcastEstimateVO>('/notifications/broadcast/estimate', params);
}

export function createBroadcast(data: BroadcastCreateRequest) {
  return post<NotificationBroadcastVO>('/notifications/broadcast', data);
}

export function getBroadcastDetail(id: number) {
  return get<NotificationBroadcastVO>(`/notifications/broadcast/${id}`);
}

export function deleteBroadcast(id: number) {
  return del<void>(`/notifications/broadcast/${id}`);
}

export function clearAllBroadcasts() {
  return del<void>('/notifications/broadcast/clear-all');
}

export function getBroadcastRecipients(
  id: number,
  params?: {
    isRead?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }
) {
  return get<import('@/types/notification/broadcast').BroadcastRecipientSummaryVO>(
    `/notifications/broadcast/${id}/recipients`,
    params
  );
}
