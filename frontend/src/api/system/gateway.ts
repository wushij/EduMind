import { get, put, post } from '@/core/http/request';
import type { GatewayMetricsVO, GatewayRouteVO, GatewayTraceLogVO } from '@/types/system/gateway';

export const getGatewayMetrics = (range = '24h') =>
  get<GatewayMetricsVO>('/system/gateway/metrics', { range });

export const listGatewayRoutes = () => get<GatewayRouteVO[]>('/system/gateway/routes');

export const updateGatewayRoutes = (routes: GatewayRouteVO[]) =>
  put<void>('/system/gateway/routes', routes);

export const getGatewayLogs = (params?: { scene?: string; model?: string; page?: number; pageSize?: number }) =>
  get<{ list: GatewayTraceLogVO[]; total: number }>('/system/gateway/logs', params);

export const resetGatewayCircuit = (modelKey?: string) =>
  post<void>('/system/gateway/circuit/reset', null, { params: modelKey ? { modelKey } : {} });
