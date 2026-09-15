import { get, post, del } from '@/core/http/request';

export interface CourseMemberItem {
  id: number;
  courseId: number;
  userId: number;
  username?: string;
  realName?: string;
  memberRole: string;
  progress?: number;
  joinTime?: string;
  status?: string;
}

export const getCourseMembers = (courseId: number) => get<CourseMemberItem[]>(`/courses/${courseId}/members`);

export const addCourseMember = (courseId: number, userId: number, role: string) =>
  post<number>(`/courses/${courseId}/members`, { userId, role });

export const removeCourseMember = (courseId: number, userId: number) =>
  del<void>(`/courses/${courseId}/members/${userId}`);

export const joinCourse = (courseId: number) =>
  post<number>(`/courses/${courseId}/members/join`);

