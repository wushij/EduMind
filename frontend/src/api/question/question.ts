import { get, post, put, del } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import { QuestionItem } from '@/types/question/question';
import { PageResult } from '@/types/common/api';

export const getQuestions = (params?: Record<string, any>, config?: HttpRequestConfig) =>
  get<PageResult<QuestionItem>>('/questions', params, config);

export const getQuestionDetail = (id: number | string) => get<QuestionItem>(`/questions/${id}`);

export const createQuestion = (data: Partial<QuestionItem>) => post<number | string>('/questions', data);

export const updateQuestion = (id: number | string, data: Partial<QuestionItem>) =>
  put<void>(`/questions/${id}`, data);

export const deleteQuestion = (id: number | string) => del<void>(`/questions/${id}`);

export const batchSaveQuestions = (courseId: number | string, questions: Partial<QuestionItem>[]) =>
  post<{ savedCount: number; questionIds: (number | string)[] }>('/questions/batch', { courseId, questions });
