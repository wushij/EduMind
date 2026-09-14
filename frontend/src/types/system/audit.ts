export interface AIAuditLog {
  id: number;
  traceId: string;
  userId: number;
  username: string;
  realName?: string;
  avatar?: string;
  userRole: string;
  model: string;
  provider: string;
  toolName?: string;
  scene?: string;
  courseId?: number;
  conversationId?: string;
  knowledgeBaseId?: number;
  retrievalHitCount?: number;
  citationDocIds?: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number; // ¥ RMB
  durationMs: number;
  status: 'SUCCESS' | 'FAILED' | 'TIMEOUT';
  ipAddress: string;
  createdAt: string;
}

export interface AuditDailyTrendItem {
  date: string;
  calls: number;
  tokens: number;
  promptTokens: number;
  completionTokens: number;
  cost: number;
}

export interface AuditSummaryVO {
  totalCalls: number;
  totalTokens: number;
  totalCostRMB: number;
  avgLatencyMs: number;
  successRate: number;
  dailyTrend: AuditDailyTrendItem[];
  modelDistribution: { model: string; count: number; percentage: number }[];
}
