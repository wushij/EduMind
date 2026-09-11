import { get } from '@/core/http/request';

export const getCourseResources = (courseId: number) => get<any[]>(`/courses/${courseId}/resources`);
