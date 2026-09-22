import { post } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
import type { SummaryRequest } from '@/types/ai/summary';

export const generateSummary = (data: SummaryRequest) =>
  post<{ content: string }>('/ai/summary', data, { timeout: AI_REQUEST_TIMEOUT });
