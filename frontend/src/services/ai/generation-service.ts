import { getCourseList } from '@/api/course/course';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';

export async function loadGenerationCourseOptions(): Promise<any[]> {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    if (res.data?.list?.length) {
      return res.data.list.map((c: any) => ({
        ...c,
        title: c.name || c.title || `课程 ${c.id}`,
        name: c.name || c.title || `课程 ${c.id}`
      }));
    }
    if (USE_MOCK) {
      return MOCK_COURSES.map((c: any) => ({
        ...c,
        title: c.name || c.title || `课程 ${c.id}`,
        name: c.name || c.title || `课程 ${c.id}`
      }));
    }
    return [];
  } catch {
    if (USE_MOCK) {
      return MOCK_COURSES.map((c: any) => ({
        ...c,
        title: c.name || c.title || `课程 ${c.id}`,
        name: c.name || c.title || `课程 ${c.id}`
      }));
    }
    return [];
  }
}
