import { get, post, put } from '@/core/http/request';

export const getGradingResult = (submissionId: number, options?: { signal?: AbortSignal; silent?: boolean }) =>
  get<any>(`/submissions/${submissionId}/grading`, undefined, {
    silent: options?.silent,
    signal: options?.signal
  });

export const triggerGrading = (submissionId: number, options?: { signal?: AbortSignal; silent?: boolean }) =>
  post<any>(`/submissions/${submissionId}/grade`, undefined, {
    silent: options?.silent,
    signal: options?.signal
  });

export const reviewGrading = (
  submissionId: number,
  items: Array<{ questionId: number; score: number; teacherComment?: string }>,
  options?: { signal?: AbortSignal; silent?: boolean }
) =>
  put<void>(`/submissions/${submissionId}/grading/review`, { items }, {
    silent: options?.silent,
    signal: options?.signal
  });
