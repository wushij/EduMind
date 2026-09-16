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
  weekTokensUsed?: number;
  monthTokensUsed?: number;
  totalTokensUsed?: number;

  todayCalls?: number;
  weekCalls?: number;
  monthCalls?: number;
  totalCalls?: number;

  todayCostRMB?: number;
  weekCostRMB?: number;
  monthCostRMB?: number;
  totalCostRMB?: number;

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
