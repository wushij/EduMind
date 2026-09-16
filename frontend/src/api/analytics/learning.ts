import { get } from '@/core/http/request';
import type {
  AiUsageAnalyticsQuery,
  AiUsageAnalyticsVO,
  LearningAnalyticsQuery,
  LearningAnalyticsVO,
  StudentPortraitQuery,
  StudentPortraitVO
} from '@/types/analytics/learning';

export const getLearningAnalytics = (params: LearningAnalyticsQuery) =>
  get<LearningAnalyticsVO>('/analytics/learning', params);

export const getStudentPortrait = (params: StudentPortraitQuery) =>
  get<StudentPortraitVO>('/analytics/learning/portrait', params);

export const getAiUsageAnalytics = (params?: AiUsageAnalyticsQuery) =>
  get<AiUsageAnalyticsVO>('/analytics/ai-usage', params);
