import { get } from '@/core/http/request';
import { DashboardStatistics } from '@/types/analytics/statistics';

export const getDashboardSummary = () => get<DashboardStatistics>('/dashboard/summary');
