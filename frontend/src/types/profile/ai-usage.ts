export interface PersonalAiUsageLogVO {
  id: number;
  scene: string;
  sceneLabel: string;
  model: string;
  totalTokens: number;
  createTime: string;
}

export interface PersonalAiUsageVO {
  todayTokensUsed: number;
  dailyTokenLimit: number;
  remainingPercent: number;
  quotaStatus: string;
  totalQaAndGenerateCalls: number;
  avgLatencyMs: number;
  semesterEstimatedCostRMB: number;
  recentLogs: PersonalAiUsageLogVO[];
  totalLogCount: number;
  logPageNum: number;
  logPageSize: number;
}

export interface PersonalAiUsageQuery {
  logDays?: number;
  pageNum?: number;
  pageSize?: number;
}
