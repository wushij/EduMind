import { post } from '@/core/http/request';

export interface LessonContentGenerateRequest {
  courseId: number;
  lessonChapterId: number;
  depth?: string;
  language?: string;
}

export interface LessonContentGenerateResult {
  contentJson: string;
}

export const generateLessonContent = (data: LessonContentGenerateRequest) =>
  post<LessonContentGenerateResult>('/ai/lesson-content/generate', data);
