import { get, post, put, del } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import { Course, CourseCreateRequest, CourseQuery } from '@/types/course/course';
import { PageResult } from '@/types/common/api';

export const getCourseList = (params?: CourseQuery, config?: HttpRequestConfig) =>
  get<PageResult<Course>>('/courses', params, config);

export const getCourseDetail = (id: number) => get<Course>(`/courses/${id}`);

export const createCourse = (data: CourseCreateRequest) => post<number>('/courses', data);

export const updateCourse = (id: number, data: Partial<CourseCreateRequest>) =>
  put<void>(`/courses/${id}`, data);

export const deleteCourse = (id: number) => del<void>(`/courses/${id}`);

export const archiveCourse = (id: number) => put<void>(`/courses/${id}/archive`);

export const unarchiveCourse = (id: number) => put<void>(`/courses/${id}/unarchive`);

export const joinCourseByCode = (code: string) => post<number>('/courses/join', { code });

export const getPublicCourses = () => get<Course[]>('/courses/public');

