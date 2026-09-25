import { get, post } from '@/core/http/request';
import type {
  KnowledgeHeatmapVO,
  KnowledgeMasteryQuery,
  KnowledgeMasteryVO,
  WrongQuestionAnalyticsVO,
  WrongQuestionQuery
} from '@/types/analytics/mastery';
import { AI_REQUEST_TIMEOUT } from '@/config';
import type { WrongQuestionDiagnoseVO } from '@/types/analytics/knowledge';

export const getKnowledgeMastery = (params: KnowledgeMasteryQuery) =>
  get<KnowledgeMasteryVO>('/analytics/knowledge-mastery', params);

export const getKnowledgeHeatmap = (courseId: number) =>
  get<KnowledgeHeatmapVO>('/analytics/knowledge-mastery/heatmap', { courseId });

export const getWrongQuestions = (params: WrongQuestionQuery) =>
  get<WrongQuestionAnalyticsVO>('/analytics/wrong-questions', params);

export const diagnoseWrongQuestion = (recordId: number) =>
  post<WrongQuestionDiagnoseVO>(`/analytics/wrong-questions/${recordId}/diagnose`, null, {
    timeout: AI_REQUEST_TIMEOUT
  });

export const cancelDiagnoseWrongQuestion = (recordId: number) =>
  post<void>(`/analytics/wrong-questions/${recordId}/cancel-diagnose`);

export const diagnoseWrongQuestionMacro = (courseId: number) =>
  post<{ report: string }>(`/analytics/wrong-questions/macro-diagnose`, null, {
    params: { courseId },
    timeout: AI_REQUEST_TIMEOUT
  });

export const cancelDiagnoseWrongQuestionMacro = (courseId: number) =>
  post<void>(`/analytics/wrong-questions/cancel-macro-diagnose`, null, { params: { courseId } });


