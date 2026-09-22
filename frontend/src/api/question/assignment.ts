import { get, post, del } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import {
  Assignment,
  AssignmentPaper,
  AssignmentStats,
  StudentAssignment
} from '@/types/question/assignment';
import { PageResult } from '@/types/common/api';

export const getAssignments = (params?: {
  courseId?: number;
  status?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
}) => get<PageResult<Assignment>>('/assignments', params);

export const getAssignmentStats = (params?: { courseId?: number }) =>
  get<AssignmentStats>('/assignments/stats', params);

export const getMyAssignments = (params?: { courseId?: number }) =>
  get<StudentAssignment[]>('/assignments/mine', params);

export const getAssignmentDetail = (id: number) => get<Assignment>(`/assignments/${id}`);

export const getAssignmentPaper = (id: number) => get<AssignmentPaper>(`/assignments/${id}/paper`);

export const createAssignment = (data: Record<string, unknown>) => post<number>('/assignments', data);

export const publishAssignment = (id: number) => post<void>(`/assignments/${id}/publish`);

export const closeAssignment = (id: number) => post<void>(`/assignments/${id}/close`);

export const remindAssignment = (id: number) =>
  post<{ remindedCount: number }>(`/assignments/${id}/remind`);

export const deleteAssignment = (id: number) => del<void>(`/assignments/${id}`);

/**
 * 提交答卷（交卷后会触发服务端 AI 评阅：客观题即时判分 + 主观题逐题串行调用大模型）。
 * 支持传入 config 放宽超时 / 挂 AbortSignal，否则会撞上 30 秒默认超时。
 */
export const submitAssignment = (
  id: number,
  answers: Array<{ questionId: number; answer: string }>,
  config?: HttpRequestConfig
) => post<number>(`/assignments/${id}/submit`, { answers }, config);

export const getAssignmentSubmissions = (id: number) => get<unknown[]>(`/assignments/${id}/submissions`);
