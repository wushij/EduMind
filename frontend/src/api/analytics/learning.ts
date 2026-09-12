import { get } from '@/core/http/request';
import type {
  AiUsageAnalyticsQuery,
  AiUsageAnalyticsVO,
  LearningAnalyticsQuery,
  LearningAnalyticsVO
} from '@/types/analytics/learning';

export const getLearningAnalytics = (params: LearningAnalyticsQuery) =>
  get<LearningAnalyticsVO>('/analytics/learning', params);

export const getAiUsageAnalytics = (params?: AiUsageAnalyticsQuery) =>
  get<AiUsageAnalyticsVO>('/analytics/ai-usage', params);
