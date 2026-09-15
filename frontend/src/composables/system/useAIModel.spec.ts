import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { AIModelConfigItem } from '@/types/system/model';
import {
  filterModelsByType,
  reasoningLabel,
  getProviderDisplayName,
  suggestDimension
} from './useAIModel';

vi.mock('@/api/system/model', () => ({
  listModels: vi.fn(),
  createModel: vi.fn(),
  updateModel: vi.fn(),
  deleteModel: vi.fn(),
  setDefaultModel: vi.fn(),
  testModel: vi.fn(),
  testModelDraft: vi.fn(),
  getProviderPresets: vi.fn()
}));

const models: AIModelConfigItem[] = [
  { name: 'chat-1', provider: 'deepseek', configType: 'chat', modelName: 'deepseek-chat', status: 'enabled', temperature: 0.7, isDefault: true },
  { name: 'emb-1', provider: 'qwen', configType: 'embedding', modelName: 'text-embedding-v3', status: 'enabled', dimension: 1536, temperature: 0, isDefault: false }
];

describe('useAIModel helpers', () => {
  it('filters models by config type', () => {
    expect(filterModelsByType(models, 'chat')).toHaveLength(1);
    expect(filterModelsByType(models, 'embedding')).toHaveLength(1);
  });

  it('maps reasoning effort and provider names', () => {
    expect(reasoningLabel('high')).toContain('HIGH');
    expect(getProviderDisplayName('deepseek')).toContain('DeepSeek');
    expect(getProviderDisplayName('unknown')).toBe('unknown');
  });

  it('suggests embedding dimensions', () => {
    expect(suggestDimension('bge-large-zh')).toBe(1024);
    expect(suggestDimension('text-embedding-3-small')).toBe(1536);
  });
});

describe('useAIModel composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads models and splits by tab', async () => {
    const { listModels, getProviderPresets } = await import('@/api/system/model');
    vi.mocked(listModels).mockResolvedValue({ data: { records: models } } as never);
    vi.mocked(getProviderPresets).mockResolvedValue({ data: { catalogVersion: '1', chat: {}, embedding: {} } } as never);

    const { useAIModel } = await import('./useAIModel');
    const { chatModels, embeddingModels, handleRefresh } = useAIModel();

    await handleRefresh();

    expect(chatModels.value).toHaveLength(1);
    expect(embeddingModels.value).toHaveLength(1);
  });
});
