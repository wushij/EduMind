import { post } from '@/core/http/request';
import type { TeachingAdviceRequest, TeachingAdviceVO } from '@/types/analytics/mastery';

export const generateTeachingAdvice = (data: TeachingAdviceRequest) =>
  post<TeachingAdviceVO>('/analytics/teaching-advice', data);
