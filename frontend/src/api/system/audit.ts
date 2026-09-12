import { get } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { AIAuditLog, AuditSummaryVO } from '@/types/system/audit';

export const mockAuditSummary: AuditSummaryVO = {
  totalCalls: 0,
  totalTokens: 0,
  totalCostRMB: 0,
  avgLatencyMs: 0,
  successRate: 100,
  dailyTrend: [],
  modelDistribution: []
};

export const mockAuditLogs: AIAuditLog[] = [];

interface DailyTrendItem {
  date: string;
  tokens: number;
  calls: number;
}

interface ModelDistributionItem {
  model: string;
  tokens: number;
}

interface BackendSummary {
  totalCalls?: number;
  totalPromptTokens?: number;
  totalCompletionTokens?: number;
  avgLatencyMs?: number;
  dailyTrend?: DailyTrendItem[];
  modelDistribution?: ModelDistributionItem[];
}

interface BackendLogEntity {
  id: number;
  userId?: number;
  model?: string;
  promptTokens?: number;
  completionTokens?: number;
  latencyMs?: number;
  scene?: string;
  createTime?: string;
}

interface PageResult<T> {
  list?: T[];
  total?: number;
}

function mapSummary(raw: BackendSummary): AuditSummaryVO {
  const prompt = raw.totalPromptTokens ?? 0;
  const completion = raw.totalCompletionTokens ?? 0;
  return {
    totalCalls: raw.totalCalls ?? 0,
    totalTokens: prompt + completion,
    totalCostRMB: ((prompt + completion) / 1000) * 0.002,
    avgLatencyMs: raw.avgLatencyMs ?? 0,
    successRate: 100,
    dailyTrend: (raw.dailyTrend || []).map((item) => ({
      date: item.date,
      calls: item.calls,
      promptTokens: Math.round(item.tokens * 0.4),
      completionTokens: Math.round(item.tokens * 0.6),
      cost: (item.tokens / 1000) * 0.002
    })),
    modelDistribution: (raw.modelDistribution || []).map((item) => ({
      model: item.model,
      count: item.tokens,
      percentage: 0
    }))
  };
}

export const getDailyTrend = async (days = 7): Promise<AuditSummaryVO['dailyTrend']> => {
  try {
    const res = await get<DailyTrendItem[]>('/system/ai-audit/daily-trend', { days });
    if (res?.data) {
      return res.data.map((item) => ({
        date: item.date,
        calls: item.calls,
        promptTokens: Math.round(item.tokens * 0.4),
        completionTokens: Math.round(item.tokens * 0.6),
        cost: (item.tokens / 1000) * 0.002
      }));
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
  }
  return [];
};

function mapLog(raw: BackendLogEntity): AIAuditLog {
  const prompt = raw.promptTokens ?? 0;
  const completion = raw.completionTokens ?? 0;
  return {
    id: raw.id,
    traceId: `tr_${raw.id}`,
    userId: raw.userId ?? 0,
    username: `user_${raw.userId ?? 0}`,
    userRole: 'USER',
    model: raw.model || 'unknown',
    provider: 'LLM',
    toolName: raw.scene,
    promptTokens: prompt,
    completionTokens: completion,
    totalTokens: prompt + completion,
    estimatedCost: ((prompt + completion) / 1000) * 0.002,
    durationMs: raw.latencyMs ?? 0,
    status: 'SUCCESS',
    ipAddress: '-',
    createdAt: raw.createTime || ''
  };
}

export const getAuditSummary = async (): Promise<AuditSummaryVO> => {
  try {
    const res = await get<BackendSummary>('/system/ai-audit/summary');
    if (res?.data) {
      return mapSummary(res.data);
    }
    if (!USE_MOCK) return mockAuditSummary;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Audit API] Fallback mockAuditSummary', err);
  }
  return USE_MOCK ? mockAuditSummary : mockAuditSummary;
};

export const getAuditLogs = async (params?: Record<string, unknown>): Promise<{ list: AIAuditLog[]; total: number }> => {
  try {
    const res = await get<PageResult<BackendLogEntity>>('/system/ai-audit/logs', params);
    if (res?.data?.list) {
      return {
        list: res.data.list.map(mapLog),
        total: res.data.total ?? res.data.list.length
      };
    }
    if (!USE_MOCK) return { list: [], total: 0 };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Audit API] Fallback mockAuditLogs', err);
  }
  return USE_MOCK ? { list: mockAuditLogs, total: 0 } : { list: [], total: 0 };
};
