import { post } from '@/core/http/request';
import type { LessonPlanRequest } from '@/types/ai/lesson';

export const generateLessonPlan = (data: LessonPlanRequest) =>
  post<{ content: string }>('/ai/lesson-plan', data);
