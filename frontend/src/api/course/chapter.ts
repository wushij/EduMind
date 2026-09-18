import { get, post, put, del } from '@/core/http/request';
import { Chapter } from '@/types/course/chapter';

export const getChapters = (courseId: number) => get<Chapter[]>(`/courses/${courseId}/chapters`);

export const createChapterApi = (
  courseId: number,
  data: {
    title: string;
    parentId?: number;
    sortOrder?: number;
    description?: string;
    durationMinutes?: number;
    lessonType?: string;
    type?: string;
    duration?: string;
  }
) => post<number>(`/courses/${courseId}/chapters`, data);

export const updateChapterApi = (
  courseId: number,
  chapterId: number,
  data: { title: string; sortOrder?: number }
) => put<void>(`/courses/${courseId}/chapters/${chapterId}`, data);

export const deleteChapterApi = (courseId: number, chapterId: number) =>
  del<void>(`/courses/${courseId}/chapters/${chapterId}`);
