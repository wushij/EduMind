import { get } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type {
  AiCallLogPageResult,
  AiUsageAnalyticsQuery,
  AiUsageAnalyticsVO,
  AiUsageLogQuery,
  LearningAnalyticsQuery,
  LearningAnalyticsVO,
  StudentPortraitQuery,
  StudentPortraitVO
} from '@/types/analytics/learning';

export const getLearningAnalytics = (params: LearningAnalyticsQuery) =>
  get<LearningAnalyticsVO>('/analytics/learning', params);

export const getStudentPortrait = (params: StudentPortraitQuery, config?: HttpRequestConfig) =>
  get<StudentPortraitVO>('/analytics/learning/portrait', params, config);

export const getAiUsageAnalytics = (params?: AiUsageAnalyticsQuery) =>
  get<AiUsageAnalyticsVO>('/analytics/ai-usage', params);

export const getAiUsageLogs = (params?: AiUsageLogQuery) =>
  get<AiCallLogPageResult>('/analytics/ai-usage/logs', params);

