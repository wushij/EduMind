import { get } from '@/core/http/request';
import type { RecommendationQuestion, RecommendationResource } from '@/types/learning/recommendation';

export const getQuestionRecommendations = (params?: { courseId?: number; chapterId?: number; limit?: number }) =>
  get<RecommendationQuestion[]>('/learning/recommendations/questions', params);

export const getResourceRecommendations = (params?: { courseId?: number; chapterId?: number; limit?: number }) =>
  get<RecommendationResource[]>('/learning/recommendations/resources', params);
