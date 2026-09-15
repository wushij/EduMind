import type { AIAuditLog, AuditSummaryVO } from '@/types/system/audit';

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
