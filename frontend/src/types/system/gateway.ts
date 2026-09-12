export interface GatewayProviderMetricVO {
  provider: string;
  calls: number;
  tokens: number;
}

export interface GatewayMetricsVO {
  totalRequests: number;
  successRate: number;
  avgLatencyMs: number;
  fallbackCount: number;
  byProvider: GatewayProviderMetricVO[];
}

export interface GatewayRouteVO {
  scene: string;
  primaryModelKey: string;
  fallbackModelKey: string;
}
