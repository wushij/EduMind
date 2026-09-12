export interface ModelProviderConfig {
  id: number;
  modelKey: string;
  name: string;
  provider: 'DeepSeek' | 'Qwen' | 'OpenAI' | 'Zhipu';
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
}
