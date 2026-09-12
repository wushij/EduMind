import { get } from '@/core/http/request';
import type { LearningPathVO } from '@/types/learning/learning-path';

export const getLearningPath = (courseId: number, studentId?: number) =>
  get<LearningPathVO>('/learning/adaptive-path', { courseId, studentId });
