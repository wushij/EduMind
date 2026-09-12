import { get, post } from '@/core/http/request';
import { AITool } from '@/types/ai/tool';

export const getAITools = (category?: string, keyword?: string) =>
  get<AITool[]>('/ai/tools', { category, keyword });

export const recordToolUse = (id: string) => post<void>(`/ai/tools/${id}/use`);
