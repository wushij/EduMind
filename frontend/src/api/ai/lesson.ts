import { post } from '@/core/http/request';

export interface LessonPlanRequest {
  courseId: number;
  topic: string;
  hours?: number;
  objectives?: string;
}

export const generateLessonPlan = (data: LessonPlanRequest) =>
  post<{ content: string }>('/ai/lesson-plan', data);
