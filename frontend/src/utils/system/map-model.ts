import type { AIModelConfigItem, AIProviderPresetsResponse } from '@/types/system/model';

interface ModelListResponse {
  records?: AIModelConfigItem[];
  list?: AIModelConfigItem[];
  total?: number;
}

interface TestModelResult {
  latencyMs?: number;
  latency?: number;
  success?: boolean;
}

export function unwrapModelList<T>(res: unknown): T[] {
  const data = (res as { data?: unknown })?.data ?? res;
  if (Array.isArray(data)) return data as T[];
  const page = data as ModelListResponse;
  if (Array.isArray(page?.records)) return page.records as unknown as T[];
  if (Array.isArray(page?.list)) return page.list as unknown as T[];
  return [];
}

export function mapVoToItem(vo: Record<string, unknown>): AIModelConfigItem {
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

export function mapTestModelResponse(res: unknown): { success: boolean; latencyMs: number; latency: number } {
  const raw = res as Record<string, unknown>;
  const data = (raw?.data && typeof raw.data === 'object' ? raw.data : raw) as TestModelResult;
  const latency = data?.latencyMs ?? data?.latency ?? 0;
  return { success: data?.success !== false, latencyMs: latency, latency };
}

export function isValidProviderPresets(data: unknown): data is AIProviderPresetsResponse {
  return Boolean(data && typeof data === 'object' && 'chat' in data);
}
