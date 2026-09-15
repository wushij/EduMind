import { post } from '@/core/http/request';
import type { SummaryRequest } from '@/types/ai/summary';

export const generateSummary = (data: SummaryRequest) =>
  post<{ content: string }>('/ai/summary', data);
