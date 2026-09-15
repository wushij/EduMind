import type { AIModelConfigItem, AIProviderPresetsResponse, ModelProviderConfig } from '@/types/system/model';
import { toLegacyModelConfig } from '@/types/system/model';

export const mockProviderPresets: AIProviderPresetsResponse = {
  catalogVersion: '2026-09-06-v4',
  chat: {
    deepseek: {
      label: 'DeepSeek',
      modelName: 'deepseek-v4-pro',
      baseUrl: 'https://api.deepseek.com/v1',
      portalUrl: 'https://platform.deepseek.com',
      protocol: 'openai_compatible',
      modelOptions: ['deepseek-v4-flash-vision-exp', 'deepseek-v4-pro', 'deepseek-v4-flash']
    },
    openai: {
      label: 'OpenAI',
      modelName: 'gpt-5',
      baseUrl: 'https://api.openai.com/v1',
      portalUrl: 'https://platform.openai.com',
      protocol: 'openai_compatible',
      modelOptions: ['gpt-5.5', 'gpt-5', 'gpt-5-codex', 'gpt-5.4-mini']
    },
    qwen: {
      label: 'Qwen（通义千问）',
      modelName: 'qwen3.8-max',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      portalUrl: 'https://bailian.console.aliyun.com',
      protocol: 'openai_compatible',
      modelOptions: ['qwen3.8-max', 'qwen3.8-max-0902', 'qwen3.8-flash']
    },
    zhipu: {
      label: 'GLM（智谱）',
      modelName: 'glm-5.3',
      baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
      portalUrl: 'https://open.bigmodel.cn',
      protocol: 'openai_compatible',
      modelOptions: ['glm-5.3', 'glm-5.3-flash']
    },
    minimax: {
      label: 'MiniMax',
      modelName: 'MiniMax-M3',
      baseUrl: 'https://api.minimaxi.com/v1',
      portalUrl: 'https://platform.minimaxi.com',
      protocol: 'openai_compatible',
      modelOptions: ['MiniMax-M3', 'MiniMax-M2.7-highspeed']
    },
    claude: {
      label: 'Claude（Anthropic）',
      modelName: 'claude-opus-5',
      baseUrl: 'https://api.anthropic.com/v1',
      portalUrl: 'https://console.anthropic.com',
      protocol: 'anthropic_or_proxy',
      modelOptions: ['claude-opus-5', 'claude-sonnet-5', 'claude-fable-5-1', 'claude-haiku-4-5']
    },
    kimi: {
      label: 'Kimi（月之暗面）',
      modelName: 'kimi-k3',
      baseUrl: 'https://api.moonshot.cn/v1',
      portalUrl: 'https://platform.moonshot.cn',
      protocol: 'openai_compatible',
      modelOptions: ['kimi-k3', 'kimi-k2.7-code', 'kimi-k2.7-code-highspeed']
    },
    mock: {
      label: 'Mock（开发测试）',
      modelName: 'mock-chat',
      baseUrl: '',
      portalUrl: '',
      protocol: 'mock',
      modelOptions: ['mock-chat']
    }
  },
  embedding: {
    openai: {
      label: 'OpenAI Embedding',
      modelName: 'text-embedding-3-large',
      baseUrl: 'https://api.openai.com/v1',
      portalUrl: 'https://platform.openai.com',
      protocol: 'openai_compatible',
      modelOptions: ['text-embedding-3-large', 'text-embedding-3-small']
    },
    qwen: {
      label: 'Qwen Embedding',
      modelName: 'text-embedding-v4',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      portalUrl: 'https://bailian.console.aliyun.com',
      protocol: 'openai_compatible',
      modelOptions: ['text-embedding-v4', 'text-embedding-v3']
    }
  }
};

export const mockModels: AIModelConfigItem[] = [
  {
    name: 'deepseek-chat',
    provider: 'deepseek',
    configType: 'chat',
    modelName: 'deepseek-chat',
    baseUrl: 'https://api.deepseek.com/v1',
    hasApiKey: false,
    temperature: 0.7,
    reasoningEffort: 'low',
    status: 'enabled',
    isDefault: true
  },
  {
    name: 'mock',
    provider: 'mock',
    configType: 'chat',
    modelName: 'mock',
    baseUrl: '',
    hasApiKey: false,
    temperature: 0.7,
    reasoningEffort: 'low',
    status: 'enabled',
    isDefault: false
  }
];

export const mockModelConfigs: ModelProviderConfig[] = mockModels.map(toLegacyModelConfig);
