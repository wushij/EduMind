import { get, post, del } from '@/core/http/request';
import { PageResult } from '@/types/common/api';

export const getQuestionBanks = (params?: Record<string, any>) =>
  get<PageResult<any>>('/question-banks', params);

export const getQuestionBankDetail = (id: number) => get<any>(`/question-banks/${id}`);

export const createQuestionBank = (data: Record<string, any>) => post<number>('/question-banks', data);

export const addQuestionsToBank = (bankId: number, questionIds: number[]) =>
  post<void>(`/question-banks/${bankId}/questions`, { questionIds });

export const removeQuestionFromBank = (bankId: number, questionId: number) =>
  del<void>(`/question-banks/${bankId}/questions/${questionId}`);

