import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type { CourseKnowledgePointSuggestResult } from '@/types/course/knowledge-point';

export const suggestCourseKnowledgePoints = (
  courseId: number,
  chapterId?: number,
  count = 4,
  config?: HttpRequestConfig
) =>
  post<CourseKnowledgePointSuggestResult>(
    '/ai/course-knowledge-points/suggest',
    {
      courseId,
      chapterId,
      count
    },
    config
  );
