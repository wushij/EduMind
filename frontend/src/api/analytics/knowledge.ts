import { get, post } from '@/core/http/request';
import type {
  KnowledgeMasteryQuery,
  KnowledgeMasteryVO,
  WrongQuestionAnalyticsVO,
  WrongQuestionQuery
} from '@/types/analytics/mastery';

export const getKnowledgeMastery = (params: KnowledgeMasteryQuery) =>
  get<KnowledgeMasteryVO>('/analytics/knowledge-mastery', params);

export const getWrongQuestions = (params: WrongQuestionQuery) =>
  get<WrongQuestionAnalyticsVO>('/analytics/wrong-questions', params);

export interface WrongQuestionDiagnoseVO {
  id: number;
  diagnosis: string;
  variantQuestionIds: string;
}

export const diagnoseWrongQuestion = (recordId: number) =>
  post<WrongQuestionDiagnoseVO>(`/analytics/wrong-questions/${recordId}/diagnose`);
