import { get } from '@/core/http/request';
import type { LearningPathVO } from '@/types/learning/learning-path';

export const getLearningPath = (courseId: number) =>
  get<LearningPathVO>('/learning/path', { courseId });
