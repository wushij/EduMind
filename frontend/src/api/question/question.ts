import { get, post, put, del } from '@/core/http/request';
import { QuestionItem } from '@/types/question/question';
import { PageResult } from '@/types/common/api';

export const getQuestions = (params?: Record<string, any>) =>
  get<PageResult<QuestionItem>>('/questions', params);

export const getQuestionDetail = (id: number) => get<QuestionItem>(`/questions/${id}`);

export const createQuestion = (data: Partial<QuestionItem>) => post<number>('/questions', data);

export const updateQuestion = (id: number, data: Partial<QuestionItem>) =>
  put<void>(`/questions/${id}`, data);

export const deleteQuestion = (id: number) => del<void>(`/questions/${id}`);

export const batchSaveQuestions = (courseId: number, questions: Partial<QuestionItem>[]) =>
  post<{ savedCount: number; questionIds: number[] }>('/questions/batch', { courseId, questions });
