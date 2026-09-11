import { get } from '@/core/http/request';

export const getTeachingAnalytics = () => get<any>('/analytics/teaching');
