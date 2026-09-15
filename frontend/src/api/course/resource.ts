import { get, post, del } from '@/core/http/request';
import type { CourseResourceItem } from '@/types/course/resource';

export const getCourseResources = (courseId: number) =>
  get<CourseResourceItem[]>(`/courses/${courseId}/resources`);

export const createCourseResource = (courseId: number, data: {
  title: string;
  resourceType?: string;
  chapterId?: number;
  resourceId?: number;
  documentId?: number;
}) => post<number>(`/courses/${courseId}/resources`, data);

export const deleteCourseResource = (courseId: number, resourceId: number) =>
  del<void>(`/courses/${courseId}/resources/${resourceId}`);
