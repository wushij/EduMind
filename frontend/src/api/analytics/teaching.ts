import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type { TeachingAdviceRequest, TeachingAdviceVO } from '@/types/analytics/mastery';

export const generateTeachingAdvice = (data: TeachingAdviceRequest, config?: HttpRequestConfig) =>
  post<TeachingAdviceVO>('/analytics/teaching-advice', data, config);
