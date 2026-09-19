import { get } from '@/core/http/request';
import type { LearningHomeOverviewVO } from '@/types/learning/home';

export const getLearningHomeOverview = (params?: { primaryCourseId?: number }) =>
  get<LearningHomeOverviewVO>('/learning/home/overview', params);
