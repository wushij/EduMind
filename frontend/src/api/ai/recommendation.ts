import { get } from '@/core/http/request';

export const getRecommendations = (courseId?: number) => get<any>('/ai/recommendations', { courseId });
