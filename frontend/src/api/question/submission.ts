import { get, post, put, del } from '@/core/http/request';
import type { PageResult } from '@/types/common/api';
import type { SubmissionItem, SubmissionOverviewStats } from '@/types/question/submission';

/** 请求可选配置：silent 为 true 时不弹全局错误提示，由调用方统一提示一次 */
export interface SubmissionRequestOptions {
  signal?: AbortSignal;
  silent?: boolean;
}

export const createSubmission = (assignmentId: number, answers: Array<{ questionId: number; answer: string }>) =>
  post<number>(`/submissions/assignments/${assignmentId}`, { answers });

export const getSubmissionDetail = (id: number, options?: SubmissionRequestOptions) =>
  get<SubmissionItem>(`/submissions/${id}`, undefined, { silent: options?.silent, signal: options?.signal });

export const deleteSubmission = (id: number, options?: SubmissionRequestOptions) =>
  del<void>(`/submissions/${id}`, undefined, { silent: options?.silent, signal: options?.signal });

export const getSubmissionGrading = (id: number, options?: SubmissionRequestOptions) =>
  get<SubmissionItem['gradingItems']>(`/submissions/${id}/grading`, undefined, {
    silent: options?.silent,
    signal: options?.signal
  });

export const gradeSubmission = (id: number, options?: SubmissionRequestOptions) =>
  post<void>(`/submissions/${id}/grade`, undefined, { signal: options?.signal, silent: options?.silent });

export const reviewGrading = (
  id: number,
  items: Array<{ questionId: number; score: number; teacherComment?: string }>,
  options?: SubmissionRequestOptions
) => put<void>(`/submissions/${id}/grading/review`, { items }, { silent: options?.silent, signal: options?.signal });

export const getSubmissionsByAssignment = (assignmentId: number) =>
  get<SubmissionItem[]>(`/submissions/assignments/${assignmentId}`);

export const getSubmissionsPage = (params?: {
  courseId?: number;
  assignmentId?: number;
  status?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
}) => get<PageResult<SubmissionItem>>('/submissions', params);

export const getSubmissionStats = (params?: { courseId?: number; assignmentId?: number }) =>
  get<SubmissionOverviewStats>('/submissions/stats', params);

export const batchGradeSubmissions = (body: {
  courseId?: number;
  assignmentId?: number;
  submissionIds?: number[];
  forceRegrade?: boolean;
}, options?: SubmissionRequestOptions) =>
  post<{ successCount: number }>('/submissions/batch-grade', body, { signal: options?.signal, silent: options?.silent });
