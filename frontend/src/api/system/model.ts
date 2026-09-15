import { get, post, put, del } from '@/core/http/request';
import type { AIModelConfigItem, AIProviderPresetsResponse, ModelProviderConfig } from '@/types/system/model';

interface ModelListResponse {
  records?: AIModelConfigItem[];
  list?: AIModelConfigItem[];
  total?: number;
}

export const getProviderPresets = () =>
  get<AIProviderPresetsResponse>('/system/ai-models/provider-presets');

export const listModels = (params?: { page?: number; pageSize?: number }) =>
  get<ModelListResponse>('/system/ai-models', { page: 1, pageSize: 100, ...params });

export const createModel = (data: Partial<AIModelConfigItem>) =>
  post<Record<string, unknown>>('/system/ai-models', data);

export const updateModel = (name: string, data: Partial<AIModelConfigItem>) =>
  put(`/system/ai-models/${encodeURIComponent(name)}`, data);

export const deleteModel = (name: string) =>
  del(`/system/ai-models/${encodeURIComponent(name)}`);

export const setDefaultModel = (name: string) =>
  post(`/system/ai-models/${encodeURIComponent(name)}/default`);

export const testModel = (name: string) =>
  post<{ latencyMs?: number; latency?: number; success?: boolean }>(
    `/system/ai-models/${encodeURIComponent(name)}/test`
  );

export const testModelDraft = (data: {
  name?: string;
  provider: string;
  configType: 'chat' | 'embedding';
  modelName: string;
  baseUrl?: string;
  apiKey?: string;
}) => post<{ latencyMs?: number; latency?: number; success?: boolean }>('/system/ai-models/test', data);

export const updateModelConfig = (id: number, data: Partial<ModelProviderConfig>) =>
  put(`/system/ai-models/${id}`, data);
