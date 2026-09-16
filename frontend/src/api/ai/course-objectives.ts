import { post } from '@/core/http/request';

export interface CourseObjectiveSuggestItem {
  title: string;
  description?: string;
}

export interface CourseObjectiveSuggestResult {
  objectives: CourseObjectiveSuggestItem[];
  aiGenerated: boolean;
  sourceLabel?: string;
}

export const suggestCourseObjectives = (courseId: number, count = 4) =>
  post<CourseObjectiveSuggestResult>('/ai/course-objectives/suggest', { courseId, count });
