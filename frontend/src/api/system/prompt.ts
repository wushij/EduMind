import { get, post, put } from '@/core/http/request';
import type { PromptTestRequest, PromptVersionItem } from '@/types/system/prompt';

export const listPromptTemplates = (category?: string) => {
  const queryParam = category && category !== 'ALL' ? { category } : undefined;
  return get<Record<string, unknown>[]>('/system/prompts', queryParam);
};

export const getPromptTemplate = (id: number) =>
  get<Record<string, unknown>>(`/system/prompts/${id}`);

export const createPromptTemplate = (payload: Record<string, unknown>) =>
  post<{ id: number }>('/system/prompts', payload);

export const updatePromptTemplate = (id: number, payload: Record<string, unknown>) =>
  put(`/system/prompts/${id}`, payload);

export const publishPromptTemplate = (id: number) =>
  post(`/system/prompts/${id}/publish`);

export const listPromptVersions = (id: number) =>
  get<PromptVersionItem[]>(`/system/prompts/${id}/versions`);

export const rollbackPromptTemplate = (id: number, targetVersion: number) =>
  post(`/system/prompts/${id}/rollback`, { targetVersion });

export const testPromptTemplate = (templateId: number, req: PromptTestRequest) =>
  post<unknown>(`/system/prompts/${templateId}/test`, {
    variables: req.variables,
    modelKey: req.model,
    systemPrompt: req.systemPrompt,
    userPromptTemplate: req.userPromptTemplate,
    temperature: req.temperature,
    maxTokens: req.maxTokens
  });

export type { PromptVersionItem };
