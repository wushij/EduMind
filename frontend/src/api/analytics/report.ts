import { get } from '@/core/http/request';
import type { TeachingReportVO } from '@/types/analytics/report';

export const getTeachingReport = (courseId: number, range = '7d') =>
  get<TeachingReportVO>('/analytics/teaching-report', { courseId, range });
