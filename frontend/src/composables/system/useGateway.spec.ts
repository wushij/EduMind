import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  formatTokens,
  computePromptRatio,
  filterProviders,
  getCallsPercentage,
  getModelProviderBrand,
  getCircuitStateClass,
  getCircuitStateText,
  translateSceneName,
  getSceneName,
  buildModelOption
} from './useGateway';

vi.mock('@/api/system/gateway', () => ({
  getGatewayMetrics: vi.fn(),
  getGatewayLogs: vi.fn(),
  resetGatewayCircuit: vi.fn(),
  listGatewayRoutes: vi.fn(),
  updateGatewayRoutes: vi.fn()
}));

vi.mock('@/composables/system/useAIModel', () => ({
  resolveModels: vi.fn()
}));

describe('useGateway helpers', () => {
  it('formats token counts', () => {
    expect(formatTokens(500)).toBe('500');
    expect(formatTokens(12000)).toBe('1.2 万');
    expect(formatTokens(1500000)).toBe('1.50 M');
    expect(formatTokens()).toBe('0');
  });

  it('computes prompt ratio', () => {
    expect(computePromptRatio(30, 70)).toBe(30);
    expect(computePromptRatio(0, 0)).toBe(40);
  });

  it('filters providers by search key', () => {
    const list = [{ provider: 'deepseek-chat' }, { provider: 'gpt-4o' }];
    expect(filterProviders(list, '')).toHaveLength(2);
    expect(filterProviders(list, 'deep')).toHaveLength(1);
  });

  it('calculates call percentage', () => {
    expect(getCallsPercentage(25, 100)).toBe(25);
    expect(getCallsPercentage(10, 0)).toBe(0);
  });

  it('maps provider brands and circuit states', () => {
    expect(getModelProviderBrand('deepseek-v4')).toContain('DeepSeek');
    expect(
      getCircuitStateClass('m1', [{ modelKey: 'm1', status: 'OPEN' }])
    ).toBe('open');
    expect(
      getCircuitStateText('m1', [{ modelKey: 'm1', status: 'HALF_OPEN' }])
    ).toContain('HALF_OPEN');
  });

  it('translates scene names', () => {
    expect(translateSceneName('chat')).toBe('智能助教答疑');
    expect(getSceneName('question_generate')).toContain('出题');
  });

  it('builds model options', () => {
    expect(buildModelOption({ name: 'Flash', modelName: 'deepseek-v4-flash' }).label).toContain('Flash');
  });
});

describe('useGateway composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads gateway metrics', async () => {
    const { getGatewayMetrics } = await import('@/api/system/gateway');
    vi.mocked(getGatewayMetrics).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        totalTokens: 1000,
        promptTokens: 400,
        completionTokens: 600,
        totalRequests: 50,
        successRate: 99.5,
        avgLatencyMs: 120,
        fallbackCount: 0,
        byProvider: []
      },
      timestamp: Date.now()
    });

    const { useGateway } = await import('./useGateway');
    const { metrics, getPromptRatio, loadMetrics } = useGateway();

    await loadMetrics();

    expect(metrics.value?.totalTokens).toBe(1000);
    expect(getPromptRatio.value).toBe(40);
  });
});
