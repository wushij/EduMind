import { get } from '@/core/http/request';
import type { BackendAuditLogEntity, BackendAuditSummary } from '@/utils/system/map-audit';

interface DailyTrendItem {
  date: string;
  tokens: number;
  calls: number;
}

interface PageResult<T> {
  list?: T[];
  total?: number;
}

export const getAuditSummary = () => get<BackendAuditSummary>('/system/ai-audit/summary');

export const getDailyTrend = (days = 7) =>
  get<DailyTrendItem[]>('/system/ai-audit/daily-trend', { days });

export const getAuditLogs = (params?: Record<string, unknown>) =>
  get<PageResult<BackendAuditLogEntity>>('/system/ai-audit/logs', params);

export const listAvailableAiModels = () =>
  get<{ list?: Array<{ modelName?: string; modelKey?: string; configName?: string }> }>('/system/models');
