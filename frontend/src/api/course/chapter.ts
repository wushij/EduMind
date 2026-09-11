import { get } from '@/core/http/request';
import { Chapter } from '@/types/course/chapter';

export const getChapters = (courseId: number) => get<Chapter[]>(`/courses/${courseId}/chapters`);
