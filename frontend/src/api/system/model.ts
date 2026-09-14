import { get, post, put, del } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import {
  AIModelConfigItem,
  AIProviderPresetsResponse,
  ModelProviderConfig,
  toLegacyModelConfig
} from '@/types/system/model';

interface ModelListResponse {
  records?: AIModelConfigItem[];
  list?: AIModelConfigItem[];
  total?: number;
}

function unwrapList<T>(res: unknown): T[] {
  const data = (res as { data?: unknown })?.data ?? res;
  if (Array.isArray(data)) return data as T[];
  const page = data as ModelListResponse;
  if (Array.isArray(page?.records)) return page.records as unknown as T[];
  if (Array.isArray(page?.list)) return page.list as unknown as T[];
  return [];
}

function mapVoToItem(vo: Record<string, unknown>): AIModelConfigItem {
  return {
    id: vo.id as number | undefined,
    name: (vo.name || vo.configName || vo.modelKey || '') as string,
    modelKey: (vo.modelKey || vo.name || vo.configName || vo.modelName || '') as string,
    provider: (vo.provider || 'mock') as string,
    configType: ((vo.configType as string) || 'chat') as AIModelConfigItem['configType'],
    modelName: (vo.modelName || vo.modelKey || '') as string,
    baseUrl: (vo.baseUrl as string) || '',
    hasApiKey: Boolean(vo.hasApiKey),
    temperature: Number(vo.temperature ?? 0.7),
    reasoningEffort: (vo.reasoningEffort as AIModelConfigItem['reasoningEffort']) || 'low',
    dimension: vo.dimension as number | undefined,
    status: ((vo.status as string) || (vo.enabled ? 'enabled' : 'disabled')) as AIModelConfigItem['status'],
    isDefault: Boolean(vo.isDefault),
    sortOrder: vo.sortOrder as number | undefined
  };
}

export const getProviderPresets = async (): Promise<AIProviderPresetsResponse> => {
  try {
    const res = await get<AIProviderPresetsResponse>('/system/ai-models/provider-presets');
    const data = (res as { data?: AIProviderPresetsResponse })?.data ?? res;
    if (data && typeof data === 'object' && 'chat' in data) {
      return data as AIProviderPresetsResponse;
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
  }
  return fallbackPresets();
};

export const fetchModels = async (): Promise<AIModelConfigItem[]> => {
  try {
    const res = await get<ModelListResponse>('/system/ai-models', { page: 1, pageSize: 100 });
    const raw = unwrapList<Record<string, unknown>>(res);
    if (raw.length > 0) {
      return raw.map(mapVoToItem);
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Model API] fallback mock', err);
  }
  return USE_MOCK ? fallbackModels() : [];
};

export const getModelConfigs = async (): Promise<ModelProviderConfig[]> => {
  const items = await fetchModels();
  return items.map(toLegacyModelConfig);
};

export const createModel = async (data: Partial<AIModelConfigItem>): Promise<AIModelConfigItem> => {
  const res = await post<Record<string, unknown>>('/system/ai-models', data);
  const vo = ((res as { data?: Record<string, unknown> })?.data ?? res) as Record<string, unknown>;
  return mapVoToItem(vo);
};

export const updateModel = async (name: string, data: Partial<AIModelConfigItem>): Promise<void> => {
  await put(`/system/ai-models/${encodeURIComponent(name)}`, data);
};

export const deleteModel = async (name: string): Promise<void> => {
  await del(`/system/ai-models/${encodeURIComponent(name)}`);
};

export const setDefaultModel = async (name: string): Promise<void> => {
  await post(`/system/ai-models/${encodeURIComponent(name)}/default`);
};

interface TestModelResult {
  latencyMs?: number;
  latency?: number;
  success?: boolean;
}

export const testModel = async (
  name: string
): Promise<{ success: boolean; latencyMs: number; latency: number }> => {
  const res = await post<TestModelResult>(
    `/system/ai-models/${encodeURIComponent(name)}/test`
  );
  const raw = res as unknown as Record<string, unknown>;
  const data = (raw?.data && typeof raw.data === 'object' ? raw.data : raw) as TestModelResult;
  const latency = data?.latencyMs ?? data?.latency ?? 0;
  return { success: data?.success !== false, latencyMs: latency, latency };
};

export const testModelDraft = async (data: {
  name?: string;
  provider: string;
  configType: 'chat' | 'embedding';
  modelName: string;
  baseUrl?: string;
  apiKey?: string;
}): Promise<{ success: boolean; latencyMs: number; latency: number }> => {
  const res = await post<TestModelResult>(
    '/system/ai-models/test',
    data
  );
  const raw = res as unknown as Record<string, unknown>;
  const body = (raw?.data && typeof raw.data === 'object' ? raw.data : raw) as TestModelResult;
  const latency = body?.latencyMs ?? body?.latency ?? 0;
  return { success: body?.success !== false, latencyMs: latency, latency };
};

export const updateModelConfig = async (
  id: number,
  data: Partial<ModelProviderConfig>
): Promise<boolean> => {
  await put(`/system/ai-models/${id}`, data);
  return true;
};

function fallbackPresets(): AIProviderPresetsResponse {
  return {
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
}

function fallbackModels(): AIModelConfigItem[] {
  return [
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
}

export const mockModelConfigs: ModelProviderConfig[] = fallbackModels().map(toLegacyModelConfig);
