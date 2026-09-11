import { get, post } from '@/core/http/request';
import { KnowledgePoint } from '@/types/course/knowledge-point';

export const getCourseKnowledgePoints = (courseId: number, chapterId?: number) =>
  get<KnowledgePoint[]>(`/courses/${courseId}/knowledge-points`, chapterId ? { chapterId } : undefined);

export const getKnowledgePoints = (courseIdOrChapterId: number, chapterId?: number) =>
  getCourseKnowledgePoints(courseIdOrChapterId, chapterId);

export const createKnowledgePoint = (
  courseId: number,
  data: { chapterId?: number; title: string; sortOrder?: number }
) => post<KnowledgePoint>(`/courses/${courseId}/knowledge-points`, data);
