import { get, post, put, del } from '@/core/http/request';
import { Course, CourseCreateRequest, CourseQuery } from '@/types/course/course';
import { PageResult } from '@/types/common/api';

export const getCourseList = (params?: CourseQuery) =>
  get<PageResult<Course>>('/courses', params);

export const getCourseDetail = (id: number) => get<Course>(`/courses/${id}`);

export const createCourse = (data: CourseCreateRequest) => post<number>('/courses', data);

export const updateCourse = (id: number, data: Partial<CourseCreateRequest>) =>
  put<void>(`/courses/${id}`, data);

export const deleteCourse = (id: number) => del<void>(`/courses/${id}`);

export const joinCourseByCode = (code: string) => post<number>('/courses/join', { code });
