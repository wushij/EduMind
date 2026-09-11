import { get } from '@/core/http/request';
import { TeachingReport } from '@/types/analytics/report';

export const getTeachingReport = () => get<TeachingReport>('/analytics/report');
