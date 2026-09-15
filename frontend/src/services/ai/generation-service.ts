import { getCourseList } from '@/api/course/course';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';

export async function loadGenerationCourseOptions(): Promise<any[]> {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    if (res.data?.list?.length) {
      return res.data.list;
    }
    if (USE_MOCK) {
      return [...MOCK_COURSES];
    }
    return [];
  } catch {
    if (USE_MOCK) {
      return [...MOCK_COURSES];
    }
    return [];
  }
}
