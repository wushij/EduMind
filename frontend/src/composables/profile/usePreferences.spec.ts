import { describe, it, expect } from 'vitest';
import { resolveProviderTagType, FALLBACK_MODELS } from './usePreferences';

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
