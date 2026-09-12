export interface LearningTrendPoint {
  date: string;
  activeUsers: number;
}

export interface ScoreTrendPoint {
  date: string;
  avgScore: number;
}

export interface LearningTrendData {
  learning: LearningTrendPoint[];
  score: ScoreTrendPoint[];
}

export interface LearningAnalyticsVO {
  courseId: number;
  studentCount: number;
  completionRate: number;
  avgScore: number;
  avgStudyMinutes: number;
  knowledgeMasteryAvg: number;
  aiUsageCount: number;
  trends: LearningTrendData;
}

export interface AiUsageDailyVO {
  date: string;
  calls: number;
  tokens: number;
}

export interface AiUsageByProviderVO {
  provider: string;
  calls: number;
  tokens: number;
}

export interface AiUsageAnalyticsVO {
  totalCalls: number;
  totalTokens: number;
  daily: AiUsageDailyVO[];
  byProvider: AiUsageByProviderVO[];
}

export interface LearningAnalyticsQuery {
  courseId: number;
  range?: string;
  classId?: number;
}

export interface AiUsageAnalyticsQuery {
  courseId?: number;
  range?: string;
}
