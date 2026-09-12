export interface AIAuditLog {
  id: number;
  traceId: string;
  userId: number;
  username: string;
  userRole: string;
  model: string;
  provider: string;
  toolName?: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number; // ¥ RMB
  durationMs: number;
  status: 'SUCCESS' | 'FAILED' | 'TIMEOUT';
  ipAddress: string;
  createdAt: string;
}

export interface AuditSummaryVO {
  totalCalls: number;
  totalTokens: number;
  totalCostRMB: number;
  avgLatencyMs: number;
  successRate: number;
  dailyTrend: { date: string; calls: number; promptTokens: number; completionTokens: number; cost: number }[];
  modelDistribution: { model: string; count: number; percentage: number }[];
}
