import { post } from '@/core/http/request';

export const generateLessonPlan = (data: any) => post<any>('/ai/lesson-plan', data);
