import { get } from '@/core/http/request';

export const getKnowledgeAnalytics = () => get<any>('/analytics/knowledge');
