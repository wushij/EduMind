import { get, post, put, del } from '@/core/http/request';
import type { KnowledgePoint, KnowledgePointSaveRequest } from '@/types/course/knowledge-point';

export const getCourseKnowledgePoints = (courseId: number, chapterId?: number) =>
  get<KnowledgePoint[]>(`/courses/${courseId}/knowledge-points`, chapterId ? { chapterId } : undefined);

export const getKnowledgePoints = (courseIdOrChapterId: number, chapterId?: number) =>
  getCourseKnowledgePoints(courseIdOrChapterId, chapterId);

export const getKnowledgePoint = (courseId: number, kpId: number) =>
  get<KnowledgePoint>(`/courses/${courseId}/knowledge-points/${kpId}`);

export const createKnowledgePoint = (courseId: number, data: KnowledgePointSaveRequest) =>
  post<KnowledgePoint>(`/courses/${courseId}/knowledge-points`, data);

export const updateKnowledgePoint = (courseId: number, kpId: number, data: KnowledgePointSaveRequest) =>
  put<KnowledgePoint>(`/courses/${courseId}/knowledge-points/${kpId}`, data);

export const deleteKnowledgePoint = (courseId: number, kpId: number) =>
  del<void>(`/courses/${courseId}/knowledge-points/${kpId}`);
