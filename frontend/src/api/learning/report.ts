import { get } from '@/core/http/request';
import type { LearningReportQuery, LearningReportVO } from '@/types/learning/report';

export const getLearningReport = (params?: LearningReportQuery) =>
  get<LearningReportVO>('/learning/report', params);
