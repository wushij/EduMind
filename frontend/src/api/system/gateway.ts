import { get, put } from '@/core/http/request';
import type { GatewayMetricsVO, GatewayRouteVO } from '@/types/system/gateway';

export const getGatewayMetrics = (range = '24h') =>
  get<GatewayMetricsVO>('/system/gateway/metrics', { range });

export const listGatewayRoutes = () => get<GatewayRouteVO[]>('/system/gateway/routes');

export const updateGatewayRoutes = (routes: GatewayRouteVO[]) =>
  put<void>('/system/gateway/routes', routes);
