import { post } from '@/core/http/request';

export interface SummaryRequest {
  courseId?: number;
  documentId?: number;
  content?: string;
  useRag?: boolean;
}

export const generateSummary = (data: SummaryRequest) =>
  post<{ content: string }>('/ai/summary', data);
