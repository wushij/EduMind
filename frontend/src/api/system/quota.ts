import { get, put } from '@/core/http/request';

export interface BackendUserQuota {
  id?: number;
  userId: number;
  dailyTokenLimit?: number;
  dailyCallLimit?: number;
  usedTokensToday?: number;
  usedCallsToday?: number;
}

export function fetchAiQuotas() {
  return get<BackendUserQuota[]>('/system/ai-quota');
}

export function updateAiQuotaHttp(
  userId: number,
  body: { dailyTokenLimit?: number; dailyCallLimit?: number }
) {
  return put(`/system/ai-quota/${userId}`, body);
}
