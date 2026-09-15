import { get, post } from '@/core/http/request';
import { Chapter } from '@/types/course/chapter';

export const getChapters = (courseId: number) => get<Chapter[]>(`/courses/${courseId}/chapters`);

export const createChapterApi = (courseId: number, data: { title: string; parentId?: number; sortOrder?: number }) =>
  post<number>(`/courses/${courseId}/chapters`, data);
