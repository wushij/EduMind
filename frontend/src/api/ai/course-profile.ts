import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';

export interface CourseDescriptionSuggestResult {
  text: string;
  aiGenerated: boolean;
  sourceLabel?: string;
}

export const suggestCourseDescription = (courseId: number, config?: HttpRequestConfig) =>
  post<CourseDescriptionSuggestResult>('/ai/course-profile/suggest-description', { courseId }, config);
