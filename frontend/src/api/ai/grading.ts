import { get, post, put } from '@/core/http/request';

export const getGradingResult = (submissionId: number) =>
  get<any>(`/submissions/${submissionId}/grading`);

export const triggerGrading = (submissionId: number) =>
  post<any>(`/submissions/${submissionId}/grade`);

export const reviewGrading = (
  submissionId: number,
  items: Array<{ questionId: number; score: number; teacherComment?: string }>
) => put<void>(`/submissions/${submissionId}/grading/review`, { items });
