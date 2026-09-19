import { get } from '@/core/http/request';
import type { LearningPathDetailVO, LearningPathStudentItem, LearningPathVO } from '@/types/learning/learning-path';

export const getLearningPath = (courseId: number, studentId?: number) =>
  get<LearningPathVO>('/learning/adaptive-path', { courseId, studentId });

export const getLearningPathDetail = (courseId: number, studentId?: number) =>
  get<LearningPathDetailVO>('/learning/adaptive-path/detail', { courseId, studentId });

export const listLearningPathStudents = (courseId: number) =>
  get<LearningPathStudentItem[]>('/learning/adaptive-path/students', { courseId });
