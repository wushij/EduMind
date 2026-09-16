import { post } from '@/core/http/request';

export interface CourseDescriptionSuggestResult {
  text: string;
  aiGenerated: boolean;
  sourceLabel?: string;
}

export const suggestCourseDescription = (courseId: number) =>
  post<CourseDescriptionSuggestResult>('/ai/course-profile/suggest-description', { courseId });
