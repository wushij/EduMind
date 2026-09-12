import { get, put } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { ModelProviderConfig } from '@/types/system/model';

export const mockModelConfigs: ModelProviderConfig[] = [
  {
    id: 1,
    modelKey: 'deepseek-chat',
    name: 'DeepSeek-V3 (Chat)',
    provider: 'DeepSeek',
    endpoint: 'https://api.deepseek.com/v1',
    apiKeyMasked: 'sk-dpsk••••••••••••••••••••••••3fa9',
    contextLength: 64000,
    maxOutputTokens: 4096,
    temperature: 0.3,
    supportsStreaming: true,
    supportsEmbedding: false,
    supportsVision: false,
    enabled: true,
    isDefault: true,
    costPer1kPrompt: 0.001,
    costPer1kCompletion: 0.002,
    healthStatus: 'HEALTHY'
  },
  {
    id: 2,
    modelKey: 'qwen-plus',
    name: '通义千问 Qwen-Plus',
    provider: 'Qwen',
    endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    apiKeyMasked: 'sk-ali••••••••••••••••••••••••8c12',
    contextLength: 32000,
    maxOutputTokens: 2048,
    temperature: 0.2,
    supportsStreaming: true,
    supportsEmbedding: false,
    supportsVision: true,
    enabled: true,
    isDefault: false,
    costPer1kPrompt: 0.004,
    costPer1kCompletion: 0.012,
    healthStatus: 'HEALTHY'
  },
  {
    id: 3,
    modelKey: 'bge-large-zh-v1.5',
    name: 'BAAI BGE-Large-ZH (Embedding)',
    provider: 'DeepSeek',
    endpoint: 'https://api.deepseek.com/v1',
    apiKeyMasked: 'sk-dpsk••••••••••••••••••••••••3fa9',
    contextLength: 512,
    maxOutputTokens: 0,
    temperature: 0.0,
    supportsStreaming: false,
    supportsEmbedding: true,
    supportsVision: false,
    enabled: true,
    isDefault: true,
    costPer1kPrompt: 0.0001,
    costPer1kCompletion: 0.0,
    healthStatus: 'HEALTHY'
  },
  {
    id: 4,
    modelKey: 'gpt-4o-mini',
    name: 'OpenAI GPT-4o Mini',
    provider: 'OpenAI',
    endpoint: 'https://api.openai.com/v1',
    apiKeyMasked: 'sk-proj••••••••••••••••••••••••99ff',
    contextLength: 128000,
    maxOutputTokens: 4096,
    temperature: 0.3,
    supportsStreaming: true,
    supportsEmbedding: false,
    supportsVision: true,
    enabled: true,
    isDefault: false,
    costPer1kPrompt: 0.0011,
    costPer1kCompletion: 0.0044,
    healthStatus: 'HEALTHY'
  }
];

export const getModelConfigs = async (): Promise<ModelProviderConfig[]> => {
  try {
    const res = await get<ModelProviderConfig[]>('/system/models');
    const list = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : [];
    if (list.length > 0) return list;
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Model API] Fallback mockModelConfigs', err);
  }
  return USE_MOCK ? mockModelConfigs : [];
};

export const updateModelConfig = async (id: number, data: Partial<ModelProviderConfig>): Promise<boolean> => {
  try {
    await put(`/system/models/${id}`, data);
    return true;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Model API] Fallback update mock', err);
    return true;
  }
};
