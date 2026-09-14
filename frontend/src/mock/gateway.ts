import type { GatewayMetricsVO, GatewayRouteVO, GatewayTraceLogVO } from '@/types/system/gateway';

export const MOCK_GATEWAY_METRICS: GatewayMetricsVO = {
  totalRequests: 12840,
  totalTokens: 7260000,
  promptTokens: 2840000,
  completionTokens: 4420000,
  estimatedCost: 14.52,
  successRate: 99.2,
  avgLatencyMs: 860,
  p95LatencyMs: 1420,
  p99LatencyMs: 2350,
  fallbackCount: 42,
  circuitOpenCount: 3,
  rateLimitedCount: 17,
  retryCount: 28,
  byProvider: [
    {
      provider: 'deepseek-chat',
      calls: 7200,
      tokens: 4120000,
      promptTokens: 1600000,
      completionTokens: 2520000,
      avgLatencyMs: 780,
      successRate: 99.6,
      cost: 8.24
    },
    {
      provider: 'gpt-4o-mini',
      calls: 3840,
      tokens: 2180000,
      promptTokens: 880000,
      completionTokens: 1300000,
      avgLatencyMs: 940,
      successRate: 99.1,
      cost: 4.36
    },
    {
      provider: 'qwen-plus',
      calls: 1450,
      tokens: 820000,
      promptTokens: 310000,
      completionTokens: 510000,
      avgLatencyMs: 820,
      successRate: 98.7,
      cost: 1.64
    },
    {
      provider: 'mock',
      calls: 350,
      tokens: 140000,
      promptTokens: 50000,
      completionTokens: 90000,
      avgLatencyMs: 120,
      successRate: 100.0,
      cost: 0.28
    }
  ],
  byScene: [
    { scene: 'chat', sceneName: '课程智能助教答疑', calls: 6420, tokens: 3620000, avgLatencyMs: 720 },
    { scene: 'question_generate', sceneName: 'AI 题库出题与变式', calls: 3280, tokens: 1950000, avgLatencyMs: 1140 },
    { scene: 'grading', sceneName: '作业/主观题智能批改', calls: 1840, tokens: 1080000, avgLatencyMs: 980 },
    { scene: 'agent', sceneName: 'Agent 多步任务规划', calls: 860, tokens: 460000, avgLatencyMs: 1480 },
    { scene: 'knowledge', sceneName: 'RAG 知识库检索增强', calls: 440, tokens: 150000, avgLatencyMs: 510 }
  ],
  timeSeriesTrend: [
    { time: '00:00', calls: 180, tokens: 92000, avgLatencyMs: 650 },
    { time: '02:00', calls: 85, tokens: 41000, avgLatencyMs: 620 },
    { time: '04:00', calls: 40, tokens: 18000, avgLatencyMs: 590 },
    { time: '06:00', calls: 120, tokens: 63000, avgLatencyMs: 680 },
    { time: '08:00', calls: 780, tokens: 440000, avgLatencyMs: 790 },
    { time: '10:00', calls: 1650, tokens: 980000, avgLatencyMs: 890 },
    { time: '12:00', calls: 1120, tokens: 610000, avgLatencyMs: 820 },
    { time: '14:00', calls: 1890, tokens: 1150000, avgLatencyMs: 910 },
    { time: '16:00', calls: 2150, tokens: 1320000, avgLatencyMs: 960 },
    { time: '18:00', calls: 1420, tokens: 820000, avgLatencyMs: 840 },
    { time: '20:00', calls: 2340, tokens: 1390000, avgLatencyMs: 920 },
    { time: '22:00', calls: 1065, tokens: 336000, avgLatencyMs: 770 }
  ],
  circuitStates: [
    { modelKey: 'deepseek-chat', status: 'CLOSED', openUntilMs: 0, consecutiveFailures: 0 },
    { modelKey: 'gpt-4o-mini', status: 'CLOSED', openUntilMs: 0, consecutiveFailures: 0 },
    { modelKey: 'qwen-plus', status: 'CLOSED', openUntilMs: 0, consecutiveFailures: 0 },
    { modelKey: 'mock', status: 'CLOSED', openUntilMs: 0, consecutiveFailures: 0 }
  ]
};

export const MOCK_GATEWAY_ROUTES: GatewayRouteVO[] = [
  { scene: 'chat', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'gpt-4o-mini' },
  { scene: 'grading', primaryModelKey: 'gpt-4o-mini', fallbackModelKey: 'deepseek-chat' },
  { scene: 'question', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'qwen-plus' },
  { scene: 'embedding', primaryModelKey: 'text-embedding-3-small', fallbackModelKey: 'bge-m3' },
  { scene: 'agent', primaryModelKey: 'deepseek-chat', fallbackModelKey: 'gpt-4o' }
];

export const MOCK_GATEWAY_LOGS: GatewayTraceLogVO[] = [
  { id: 101, userId: 1, model: 'deepseek-chat', promptTokens: 420, completionTokens: 860, totalTokens: 1280, latencyMs: 750, scene: 'chat', createTime: '2026-09-14 10:45:12', status: 'SUCCESS' },
  { id: 102, userId: 2, model: 'deepseek-chat', promptTokens: 1250, completionTokens: 1640, totalTokens: 2890, latencyMs: 1420, scene: 'question_generate', createTime: '2026-09-14 10:42:08', status: 'SUCCESS' },
  { id: 103, userId: 1, model: 'gpt-4o-mini', promptTokens: 980, completionTokens: 1100, totalTokens: 2080, latencyMs: 1120, scene: 'grading', createTime: '2026-09-14 10:39:55', status: 'SUCCESS' },
  { id: 104, userId: 3, model: 'qwen-plus', promptTokens: 350, completionTokens: 480, totalTokens: 830, latencyMs: 640, scene: 'chat', createTime: '2026-09-14 10:36:20', status: 'SUCCESS' },
  { id: 105, userId: 2, model: 'mock', promptTokens: 180, completionTokens: 210, totalTokens: 390, latencyMs: 110, scene: 'agent', createTime: '2026-09-14 10:30:15', status: 'SUCCESS' }
];
