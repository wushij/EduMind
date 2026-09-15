import { get, post, del } from '@/core/http/request';

export interface CourseResourceItem {
  id: number;
  courseId: number;
  resourceId?: number;
  documentId?: number;
  title: string;
  resourceType: string;
  createTime?: string;
  size?: string;
  downloadUrl?: string;
}

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

