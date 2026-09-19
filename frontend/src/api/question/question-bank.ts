import { get, post, put, del } from '@/core/http/request';
import { PageResult } from '@/types/common/api';

export const getQuestionBanks = (params?: Record<string, any>) =>
  get<PageResult<any>>('/question-banks', params);

export const getQuestionBankDetail = (id: number | string) => get<any>(`/question-banks/${id}`);

export const createQuestionBank = (data: Record<string, any>) => post<number | string>('/question-banks', data);

export const updateQuestionBank = (id: number | string, data: Record<string, any>) =>
  put<void>(`/question-banks/${id}`, data);

export const addQuestionsToBank = (bankId: number | string, questionIds: (number | string)[]) =>
  post<void>(`/question-banks/${bankId}/questions`, { questionIds });

export const removeQuestionFromBank = (bankId: number | string, questionId: number | string) =>
  del<void>(`/question-banks/${bankId}/questions/${questionId}`);

export const deleteQuestionBank = (id: number | string) => del<void>(`/question-banks/${id}`);


