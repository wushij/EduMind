import { get, post } from '@/core/http/request';
import type { WrongQuestionListVO, WrongQuestionRecordItem } from '@/types/learning/wrong-question';

export function getWrongQuestionList(params: {
  courseId: number;
  knowledgePointId?: number;
  page?: number;
  pageSize?: number;
}) {
  return get<WrongQuestionListVO>('/analytics/wrong-questions', params);
}

export function diagnoseWrongQuestion(id: number) {
  return post<WrongQuestionRecordItem>(`/analytics/wrong-questions/${id}/diagnose`);
}
