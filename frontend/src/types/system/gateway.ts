export interface GatewayProviderMetricVO {
  provider: string;
  calls: number;
  tokens: number;
  promptTokens?: number;
  completionTokens?: number;
  avgLatencyMs?: number;
  successRate?: number;
  cost?: number;
}

export interface GatewaySceneMetricVO {
  scene: string;
  sceneName: string;
  calls: number;
  tokens: number;
  avgLatencyMs?: number;
}

export interface TimeSeriesPointVO {
  time: string;
  calls: number;
  tokens: number;
  avgLatencyMs?: number;
}

export interface GatewayCircuitStateVO {
  modelKey: string;
  status: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | string;
  openUntilMs?: number;
  consecutiveFailures?: number;
}

export interface GatewayMetricsVO {
  totalRequests: number;
  totalTokens?: number;
  promptTokens?: number;
  completionTokens?: number;
  estimatedCost?: number;
  successRate: number;
  avgLatencyMs: number;
  p95LatencyMs?: number;
  p99LatencyMs?: number;
  fallbackCount: number;
  circuitOpenCount?: number;
  rateLimitedCount?: number;
  retryCount?: number;
  byProvider: GatewayProviderMetricVO[];
  byScene?: GatewaySceneMetricVO[];
  timeSeriesTrend?: TimeSeriesPointVO[];
  circuitStates?: GatewayCircuitStateVO[];
}

export interface GatewayRouteVO {
  scene: string;
  primaryModelKey: string;
  fallbackModelKey: string;
}

export interface GatewayTraceLogVO {
  id: number;
  userId?: number;
  model: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  scene: string;
  createTime: string;
  retrievalHitCount?: number;
  citationDocIds?: string;
  status?: string;
}
