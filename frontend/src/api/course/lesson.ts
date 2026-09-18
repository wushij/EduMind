import { get, put, post } from '@/core/http/request';
import type { KnowledgePoint } from '@/types/course/knowledge-point';

export interface LessonResourceSummary {
  id: number;
  resourceId: number;
  title: string;
  resourceType?: string;
  downloadUrl?: string;
}

export interface LessonProgress {
  status: string;
  progressPercent: number;
  lastBlockId?: string;
  lastStudyAt?: string;
  completedAt?: string;
}

export interface LessonDetail {
  id: number;
  courseId: number;
  parentChapterId?: number;
  parentChapterTitle?: string;
  title: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  contentStatus?: string;
  contentJson?: string;
  publishedAt?: string;
  knowledgePoints?: KnowledgePoint[];
  resources?: LessonResourceSummary[];
  progress?: LessonProgress;
}

export interface CourseLessonProgressSummary {
  totalLessonCount: number;
  completedLessonCount: number;
}

export interface LessonUpdateRequest {
  title?: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  contentJson?: string;
  knowledgePointIds?: number[];
}

export interface LessonProgressUpdateRequest {
  status?: string;
  progressPercent?: number;
  lastBlockId?: string;
  durationMinutes?: number;
}

export const getLessonDetail = (courseId: number, lessonId: number, preview = false) =>
  get<LessonDetail>(`/courses/${courseId}/lessons/${lessonId}`, preview ? { preview: true } : undefined);

export const updateLesson = (courseId: number, lessonId: number, data: LessonUpdateRequest) =>
  put<void>(`/courses/${courseId}/lessons/${lessonId}`, data);

export const publishLesson = (courseId: number, lessonId: number) =>
  post<void>(`/courses/${courseId}/lessons/${lessonId}/publish`);

export const unpublishLesson = (courseId: number, lessonId: number) =>
  post<void>(`/courses/${courseId}/lessons/${lessonId}/unpublish`);

export const getLessonProgress = (courseId: number, lessonId: number) =>
  get<LessonProgress>(`/courses/${courseId}/lessons/${lessonId}/progress`);

export const updateLessonProgress = (courseId: number, lessonId: number, data: LessonProgressUpdateRequest) =>
  put<LessonProgress>(`/courses/${courseId}/lessons/${lessonId}/progress`, data);

export const getCourseLessonProgressSummary = (courseId: number) =>
  get<CourseLessonProgressSummary>(`/courses/${courseId}/progress/summary`);
