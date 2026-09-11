import { get } from '@/core/http/request';

export const getLearningAnalytics = () => get<any>('/analytics/learning');
