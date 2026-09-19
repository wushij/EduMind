import { get, post } from '@/core/http/request';
import type {
  WrongBookDetailVO,
  WrongBookListVO,
  WrongBookOverviewVO,
  WrongQuestionRecordItem
} from '@/types/learning/wrong-question';

export function getWrongBook(params: {
  courseId: number;
  page?: number;
  pageSize?: number;
  errorType?: string;
  knowledgePointId?: number;
  status?: number;
}) {
  return get<WrongBookListVO>('/learning/wrong-book', params);
}

export function getWrongBookOverview(courseId: number) {
  return get<WrongBookOverviewVO>('/learning/wrong-book/overview', { courseId });
}

export function getWrongBookDetail(id: number) {
  return get<WrongBookDetailVO>(`/learning/wrong-book/${id}`);
}

export function diagnoseWrongBookItem(id: number) {
  return post<WrongQuestionRecordItem>(`/learning/wrong-book/${id}/diagnose`);
}

export function masterWrongBookItem(id: number) {
  return post<void>(`/learning/wrong-book/${id}/master`);
}
