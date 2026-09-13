import type { GatewayMetricsVO, GatewayRouteVO } from '@/types/system/gateway';

export const MOCK_GATEWAY_METRICS: GatewayMetricsVO = {
  totalRequests: 12840,
  successRate: 99.2,
  avgLatencyMs: 1180,
  fallbackCount: 42,
  circuitOpenCount: 3,
  rateLimitedCount: 17,
  retryCount: 28,
  byProvider: [
    { provider: 'DeepSeek', calls: 7200, tokens: 4120000 },
    { provider: 'OpenAI', calls: 3840, tokens: 2180000 },
    { provider: 'Qwen', calls: 1800, tokens: 960000 }
  ]
};

export const MOCK_GATEWAY_ROUTES: GatewayRouteVO[] = [
  { scene: 'chat', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'gpt-4o-mini' },
  { scene: 'grading', primaryModelKey: 'gpt-4o-mini', fallbackModelKey: 'deepseek-chat' },
  { scene: 'question', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'qwen-plus' },
  { scene: 'embedding', primaryModelKey: 'text-embedding-3-small', fallbackModelKey: 'bge-m3' },
  { scene: 'agent', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'gpt-4o' }
];
