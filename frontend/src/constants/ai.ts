import type { ModelProviderConfig } from '@/types/system/model';

/** 模型接口不可用时的兜底列表（与后端 mock / 默认配置对齐） */
export const DEFAULT_CHAT_MODELS: ModelProviderConfig[] = [
  {
    id: 1,
    modelKey: 'deepseek-chat',
    name: 'DeepSeek Chat',
    provider: 'DeepSeek',
    endpoint: '',
    apiKeyMasked: '',
    contextLength: 64000,
    maxOutputTokens: 4096,
    temperature: 0.3,
    supportsStreaming: true,
    supportsEmbedding: false,
    supportsVision: false,
    enabled: true,
    isDefault: true,
    costPer1kPrompt: 0,
    costPer1kCompletion: 0,
    healthStatus: 'HEALTHY'
  },
  {
    id: 2,
    modelKey: 'mock',
    name: 'Mock 模型',
    provider: 'DeepSeek',
    endpoint: '',
    apiKeyMasked: '',
    contextLength: 32000,
    maxOutputTokens: 4096,
    temperature: 0.3,
    supportsStreaming: true,
    supportsEmbedding: false,
    supportsVision: false,
    enabled: true,
    isDefault: false,
    costPer1kPrompt: 0,
    costPer1kCompletion: 0,
    healthStatus: 'HEALTHY'
  }
];
