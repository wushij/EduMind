import { get, post } from '@/core/http/request';
import { Assignment } from '@/types/question/assignment';
import { PageResult } from '@/types/common/api';

export const getAssignments = (params?: { courseId?: number; status?: string; page?: number; pageSize?: number }) =>
  get<PageResult<Assignment>>('/assignments', params);

export const getAssignmentDetail = (id: number) => get<Assignment>(`/assignments/${id}`);

export const createAssignment = (data: Record<string, any>) => post<number>('/assignments', data);

export const publishAssignment = (id: number) => post<void>(`/assignments/${id}/publish`);

export const submitAssignment = (id: number, answers: Array<{ questionId: number; answer: string }>) =>
  post<number>(`/assignments/${id}/submit`, { answers });

export const getAssignmentSubmissions = (id: number) => get<any[]>(`/assignments/${id}/submissions`);
