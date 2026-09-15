import type { AIAuditLog, AuditSummaryVO, AuditDailyTrendItem } from '@/types/system/audit';

interface DailyTrendItem {
  date: string;
  tokens: number;
  calls: number;
}

interface ModelDistributionItem {
  model: string;
  tokens: number;
}

export interface BackendAuditSummary {
  totalCalls?: number;
  totalPromptTokens?: number;
  totalCompletionTokens?: number;
  avgLatencyMs?: number;
  dailyTrend?: DailyTrendItem[];
  modelDistribution?: ModelDistributionItem[];
}

export interface BackendAuditLogEntity {
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

export const USER_META_MAP: Record<number, { username: string; realName: string; role: string; avatar?: string }> = {
  1: { username: 'admin', realName: '系统超级管理员', role: 'ADMIN' },
  2: { username: 'teacher', realName: '李华教授', role: 'TEACHER' },
  3: { username: 'student', realName: '张子轩', role: 'STUDENT' },
  4: { username: 'student2', realName: '李梦琪', role: 'STUDENT' }
};

export function mapSummary(raw: BackendAuditSummary): AuditSummaryVO {
  const prompt = raw.totalPromptTokens ?? 0;
  const completion = raw.totalCompletionTokens ?? 0;
  const totalTokens = prompt + completion;
  return {
    totalCalls: raw.totalCalls ?? 0,
    totalTokens,
    totalCostRMB: Number(((totalTokens / 1000) * 0.002).toFixed(2)),
    avgLatencyMs: raw.avgLatencyMs ?? 0,
    successRate: 100,
    dailyTrend: (raw.dailyTrend || []).map((item) => mapDailyTrendItem(item)),
    modelDistribution: (raw.modelDistribution || []).map((item) => ({
      model: item.model,
      count: item.tokens,
      percentage: 0
    }))
  };
}

export function mapDailyTrendItem(item: DailyTrendItem): AuditDailyTrendItem {
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
}

export function mapAuditLog(raw: BackendAuditLogEntity): AIAuditLog {
  const prompt = raw.promptTokens ?? 0;
  const completion = raw.completionTokens ?? 0;
  const total = raw.totalTokens ?? (prompt + completion);
  const uid = raw.userId ?? 0;

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
