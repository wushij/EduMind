import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';

export interface CourseObjectiveSuggestItem {
  title: string;
  description?: string;
}

export interface CourseObjectiveSuggestResult {
  objectives: CourseObjectiveSuggestItem[];
  aiGenerated: boolean;
  sourceLabel?: string;
}

export const suggestCourseObjectives = (
  courseId: number,
  count = 4,
  config?: HttpRequestConfig
) => post<CourseObjectiveSuggestResult>('/ai/course-objectives/suggest', { courseId, count }, config);
