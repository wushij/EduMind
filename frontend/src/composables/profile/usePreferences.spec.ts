import { describe, it, expect } from 'vitest';
import { resolveProviderTagType, FALLBACK_MODELS, getRecommendedModelKey } from './usePreferences';
import type { ModelProviderConfig } from '@/types/system/model';

describe('resolveProviderTagType', () => {
  it('maps known providers to element tag types', () => {
    expect(resolveProviderTagType('DeepSeek')).toBe('primary');
    expect(resolveProviderTagType('Qwen')).toBe('success');
    expect(resolveProviderTagType('Zhipu')).toBe('warning');
  });

  it('returns info for unknown providers', () => {
    expect(resolveProviderTagType('')).toBe('info');
    expect(resolveProviderTagType('OpenAI')).toBe('info');
  });
});

describe('FALLBACK_MODELS', () => {
  it('contains four enabled fallback models', () => {
    expect(FALLBACK_MODELS).toHaveLength(4);
    expect(FALLBACK_MODELS.every((model) => model.enabled !== false)).toBe(true);
  });

  it('marks deepseek-v3 as default', () => {
    expect(FALLBACK_MODELS.find((model) => model.modelKey === 'deepseek-v3')?.isDefault).toBe(true);
  });
});

describe('getRecommendedModelKey', () => {
  it('returns undefined for empty model list', () => {
    expect(getRecommendedModelKey([])).toBeUndefined();
  });

  it('returns the modelKey with isDefault true when present', () => {
    const list = [
      { modelKey: 'deepseek-v3', isDefault: false } as ModelProviderConfig,
      { modelKey: 'v4.1flash', isDefault: true } as ModelProviderConfig,
      { modelKey: 'qwen-turbo', isDefault: false } as ModelProviderConfig
    ];
    expect(getRecommendedModelKey(list)).toBe('v4.1flash');
  });

  it('falls back to the first model if no isDefault flag is marked', () => {
    const list = [
      { modelKey: 'qwen-2.5', isDefault: false } as ModelProviderConfig,
      { modelKey: 'glm-4', isDefault: false } as ModelProviderConfig
    ];
    expect(getRecommendedModelKey(list)).toBe('qwen-2.5');
  });
});
