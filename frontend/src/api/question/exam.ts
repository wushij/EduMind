import { get, post, put, del } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
import { ExamPaper } from '@/types/question/exam';
import { PageResult } from '@/types/common/api';

export const getExams = (params?: { courseId?: number; keyword?: string; page?: number; pageSize?: number }) =>
  get<PageResult<ExamPaper>>('/exams', params);

export const getExamDetail = (id: number) => get<ExamPaper>(`/exams/${id}`);

export const exportExam = (id: number) => get<any>(`/exams/${id}/export`);

export const createExam = (exam: Partial<ExamPaper>) => post<{ examId: number }>('/exams', exam);

export const updateExam = (id: number, exam: Partial<ExamPaper>) => put<void>(`/exams/${id}`, exam);

export const deleteExam = (id: number) => del<void>(`/exams/${id}`);

export const generateExam = (params: any) =>
  post<any>('/ai/exams/generate', params, { timeout: AI_REQUEST_TIMEOUT });
