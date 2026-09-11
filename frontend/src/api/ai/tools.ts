import { get } from '@/core/http/request';
import { AITool } from '@/types/ai/tool';

export const getAITools = (category?: string, keyword?: string) =>
  get<AITool[]>('/ai/tools', { category, keyword });
