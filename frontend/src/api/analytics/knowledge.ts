import { get, post } from '@/core/http/request';
import type {
  KnowledgeHeatmapVO,
  KnowledgeMasteryQuery,
  KnowledgeMasteryVO,
  WrongQuestionAnalyticsVO,
  WrongQuestionQuery
} from '@/types/analytics/mastery';
import type { WrongQuestionDiagnoseVO } from '@/types/analytics/knowledge';

export const getKnowledgeMastery = (params: KnowledgeMasteryQuery) =>
  get<KnowledgeMasteryVO>('/analytics/knowledge-mastery', params);

export const getKnowledgeHeatmap = (courseId: number) =>
  get<KnowledgeHeatmapVO>('/analytics/knowledge-mastery/heatmap', { courseId });

export const getWrongQuestions = (params: WrongQuestionQuery) =>
  get<WrongQuestionAnalyticsVO>('/analytics/wrong-questions', params);

export const diagnoseWrongQuestion = (recordId: number) =>
  post<WrongQuestionDiagnoseVO>(`/analytics/wrong-questions/${recordId}/diagnose`);
