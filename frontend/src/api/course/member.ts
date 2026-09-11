import { get, post } from '@/core/http/request';

export const getCourseMembers = (courseId: number) => get<any[]>(`/courses/${courseId}/members`);

export const addCourseMember = (courseId: number, userId: number, role: string) =>
  post<void>(`/courses/${courseId}/members`, { userId, role });
