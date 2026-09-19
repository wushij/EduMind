import { get, post, del } from '@/core/http/request';
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

export const submitAssignment = (id: number, answers: Array<{ questionId: number; answer: string }>) =>
  post<number>(`/assignments/${id}/submit`, { answers });

export const getAssignmentSubmissions = (id: number) => get<unknown[]>(`/assignments/${id}/submissions`);
