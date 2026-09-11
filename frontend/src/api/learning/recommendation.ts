import { get } from '@/core/http/request';

export interface RecommendationQuestion {
  questionId: number;
  type: string;
  difficulty: string;
  stem: string;
  knowledgePointName?: string;
  reason?: string;
}

export interface RecommendationResource {
  resourceId?: number;
  documentId?: number;
  title: string;
  resourceType?: string;
  reason?: string;
}

export const getQuestionRecommendations = (params?: { courseId?: number; chapterId?: number; limit?: number }) =>
  get<RecommendationQuestion[]>('/learning/recommendations/questions', params);

export const getResourceRecommendations = (params?: { courseId?: number; chapterId?: number; limit?: number }) =>
  get<RecommendationResource[]>('/learning/recommendations/resources', params);
