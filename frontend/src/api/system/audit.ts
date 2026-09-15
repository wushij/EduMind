import { get } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { AIAuditLog, AuditSummaryVO, AuditDailyTrendItem } from '@/types/system/audit';

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
  username?: string;
  realName?: string;
  avatar?: string;
  userRole?: string;
  courseId?: number;
  conversationId?: string;
  model?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  latencyMs?: number;
  scene?: string;
  knowledgeBaseId?: number;
  retrievalHitCount?: number;
  citationDocIds?: string;
  createTime?: string;
}

interface PageResult<T> {
  list?: T[];
  total?: number;
}

const USER_META_MAP: Record<number, { username: string; realName: string; role: string; avatar?: string }> = {
  1: { username: 'admin', realName: '系统超级管理员', role: 'ADMIN' },
  2: { username: 'teacher', realName: '李华教授', role: 'TEACHER' },
  3: { username: 'student', realName: '张子轩', role: 'STUDENT' },
  4: { username: 'student2', realName: '李梦琪', role: 'STUDENT' }
};

function mapSummary(raw: BackendSummary): AuditSummaryVO {
  const prompt = raw.totalPromptTokens ?? 0;
  const completion = raw.totalCompletionTokens ?? 0;
  const totalTokens = prompt + completion;
  return {
    totalCalls: raw.totalCalls ?? 0,
    totalTokens,
    totalCostRMB: Number(((totalTokens / 1000) * 0.002).toFixed(2)),
    avgLatencyMs: raw.avgLatencyMs ?? 0,
    successRate: 100,
    dailyTrend: (raw.dailyTrend || []).map((item) => {
      const tokens = Number(item.tokens) || 0;
      const calls = Number(item.calls) || 0;
      return {
        date: item.date,
        calls,
        tokens,
        promptTokens: Math.round(tokens * 0.4),
        completionTokens: Math.round(tokens * 0.6),
        cost: Number(((tokens / 1000) * 0.002).toFixed(4))
      };
    }),
    modelDistribution: (raw.modelDistribution || []).map((item) => ({
      model: item.model,
      count: item.tokens,
      percentage: 0
    }))
  };
}

export const getDailyTrend = async (days = 7): Promise<AuditDailyTrendItem[]> => {
  try {
    const res = await get<DailyTrendItem[]>('/system/ai-audit/daily-trend', { days });
    if (res?.data) {
      return res.data.map((item) => {
        const tokens = Number(item.tokens) || 0;
        const calls = Number(item.calls) || 0;
        return {
          date: item.date,
          calls,
          tokens,
          promptTokens: Math.round(tokens * 0.4),
          completionTokens: Math.round(tokens * 0.6),
          cost: Number(((tokens / 1000) * 0.002).toFixed(4))
        };
      });
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
  const total = raw.totalTokens ?? (prompt + completion);
  const uid = raw.userId ?? 0;

  // 优先取后端实时关联的真实用户信息，若无则使用兜底元数据
  const meta = USER_META_MAP[uid];
  const username = raw.username || meta?.username || (uid ? `user_${uid}` : 'system');
  const realName = raw.realName || meta?.realName || (uid === 1 ? '系统超级管理员' : `用户 #${uid}`);
  const userRole = raw.userRole || meta?.role || (uid === 1 ? 'ADMIN' : 'USER');
  const avatar = raw.avatar || meta?.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${username}&backgroundColor=e0e7ff`;

  const rawModel = (raw.model || '').trim();
  let provider = '—';
  if (rawModel) {
    const lower = rawModel.toLowerCase();
    if (lower.includes('deepseek')) {
      provider = 'DeepSeek';
    } else if (lower.includes('gpt') || lower.includes('openai')) {
      provider = 'OpenAI';
    } else if (lower.includes('qwen')) {
      provider = 'Qwen';
    } else if (lower.includes('claude') || lower.includes('anthropic')) {
      provider = 'Anthropic';
    } else if (lower.includes('edumind') || lower.includes('mock')) {
      provider = 'EduMind AI';
    } else {
      provider = '—';
    }
  }

  return {
    id: raw.id,
    traceId: `tr_${raw.id}`,
    userId: uid,
    username,
    realName,
    avatar,
    userRole,
    model: rawModel,
    provider,
    toolName: raw.scene || 'CHAT',
    scene: raw.scene || 'CHAT',
    courseId: raw.courseId,
    conversationId: raw.conversationId,
    knowledgeBaseId: raw.knowledgeBaseId,
    retrievalHitCount: raw.retrievalHitCount ?? 0,
    citationDocIds: raw.citationDocIds,
    promptTokens: prompt,
    completionTokens: completion,
    totalTokens: total,
    estimatedCost: Number(((total / 1000) * 0.002).toFixed(4)),
    durationMs: raw.latencyMs ?? 0,
    status: 'SUCCESS',
    ipAddress: '127.0.0.1',
    createdAt: raw.createTime ? raw.createTime.replace('T', ' ').substring(0, 19) : ''
  };
}

function emptyAuditSummary(): AuditSummaryVO {
  return {
    totalCalls: 0,
    totalTokens: 0,
    totalCostRMB: 0,
    avgLatencyMs: 0,
    successRate: 100,
    dailyTrend: [],
    modelDistribution: []
  };
}

export const getAvailableAiModels = async (): Promise<string[]> => {
  try {
    const res = await get<{ list?: Array<{ modelName?: string; modelKey?: string; configName?: string }> }>('/system/models');
    const records = res?.data?.list || [];
    const models = records
      .map((m) => m.modelName || m.modelKey || m.configName)
      .filter((m): m is string => Boolean(m));
    if (models.length > 0) {
      return Array.from(new Set(models));
    }
  } catch (err) {
    console.warn('Failed to load system models', err);
  }
  return USE_MOCK ? ['deepseek-v4-flash'] : [];
};

export const getAuditSummary = async (): Promise<AuditSummaryVO> => {
  try {
    const res = await get<BackendSummary>('/system/ai-audit/summary');
    if (res?.data) {
      return mapSummary(res.data);
    }
    if (USE_MOCK) return mockAuditSummary;
    return emptyAuditSummary();
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Audit API] Fallback mockAuditSummary', err);
    return mockAuditSummary;
  }
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
