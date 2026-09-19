import { get, post, del } from '@/core/http/request';
import { axiosInstance } from '@/core/http/axios';
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

export const uploadCourseResource = async (
  courseId: number,
  file: File,
  data: { title: string; resourceType?: string; chapterId?: number; syncToKnowledgeBase?: boolean }
) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', data.title);
  if (data.resourceType) {
    formData.append('resourceType', data.resourceType);
  }
  if (data.chapterId != null) {
    formData.append('chapterId', String(data.chapterId));
  }
  if (data.syncToKnowledgeBase != null) {
    formData.append('syncToKnowledgeBase', String(data.syncToKnowledgeBase));
  }
  const res = await axiosInstance.post(`/courses/${courseId}/resources/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const deleteCourseResource = (courseId: number, resourceId: number) =>
  del<void>(`/courses/${courseId}/resources/${resourceId}`);

/** 将 /api/storage/... 转为 axios 相对路径 */
export function toStorageApiPath(downloadUrl: string): string {
  if (downloadUrl.startsWith('/api/')) {
    return downloadUrl.slice(4);
  }
  return downloadUrl;
}
