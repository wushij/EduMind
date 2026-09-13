export type ModelConfigType = 'chat' | 'embedding';
export type ReasoningEffort = 'low' | 'medium' | 'high' | 'max';
export type ModelStatus = 'enabled' | 'disabled';

/** 对齐 goblog-web AIModelConfigItem，EduMind 模型配置主类型 */
export interface AIModelConfigItem {
  id?: number;
  name: string;
  provider: string;
  configType: ModelConfigType;
  modelName: string;
  baseUrl?: string;
  apiKey?: string;
  hasApiKey?: boolean;
  temperature: number;
  reasoningEffort?: ReasoningEffort;
  dimension?: number;
  status: ModelStatus;
  isDefault: boolean;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AIProviderPreset {
  label: string;
  modelName: string;
  baseUrl: string;
  portalUrl: string;
  protocol: string;
  modelOptions: string[];
}

export interface AIProviderPresetsResponse {
  catalogVersion: string;
  chat: Record<string, AIProviderPreset>;
  embedding: Record<string, AIProviderPreset>;
}

/** @deprecated 保留供 chat 降级兼容，新页面请使用 AIModelConfigItem */
export interface ModelProviderConfig {
  id: number;
  modelKey: string;
  name: string;
  provider: 'DeepSeek' | 'Qwen' | 'OpenAI' | 'Zhipu' | string;
  endpoint: string;
  apiKeyMasked: string;
  contextLength: number;
  maxOutputTokens: number;
  temperature: number;
  supportsStreaming: boolean;
  supportsEmbedding: boolean;
  supportsVision: boolean;
  enabled: boolean;
  isDefault: boolean;
  costPer1kPrompt: number;
  costPer1kCompletion: number;
  healthStatus: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';
  reasoningEffort?: ReasoningEffort;
  configType?: ModelConfigType;
}

export function toLegacyModelConfig(item: AIModelConfigItem): ModelProviderConfig {
  const isEmbedding = item.configType === 'embedding';
  return {
    id: item.id ?? 0,
    modelKey: item.modelName,
    name: item.name,
    provider: item.provider,
    endpoint: item.baseUrl || '',
    apiKeyMasked: item.hasApiKey ? '••••••••••••••••' : '',
    contextLength: isEmbedding ? 512 : 64000,
    maxOutputTokens: isEmbedding ? 0 : 8192,
    temperature: item.temperature,
    supportsStreaming: !isEmbedding,
    supportsEmbedding: isEmbedding,
    supportsVision: false,
    enabled: item.status === 'enabled',
    isDefault: item.isDefault,
    costPer1kPrompt: 0,
    costPer1kCompletion: 0,
    healthStatus: 'HEALTHY',
    reasoningEffort: item.reasoningEffort,
    configType: item.configType
  };
}
