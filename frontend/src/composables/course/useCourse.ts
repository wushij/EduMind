import { ref } from 'vue';
import { Course, CourseQuery, CourseCreateRequest } from '@/types/course/course';
import { Chapter } from '@/types/course/chapter';
import {
  getCourseList,
  getCourseDetail,
  createCourse as createCourseApi,
  updateCourse,
  deleteCourse,
  joinCourseByCode
} from '@/api/course/course';
import { getChapters } from '@/api/course/chapter';
import { mapCourse } from '@/utils/course/map-course';

export function useCourse() {
  const courses = ref<Course[]>([]);
  const currentCourse = ref<Course | null>(null);
  const chapters = ref<Chapter[]>([]);
  const loading = ref(false);
  const total = ref(0);

  async function fetchCourses(query: CourseQuery = {}) {
    loading.value = true;
    try {
      const res = await getCourseList({
        keyword: query.keyword,
        status: query.status && query.status !== 'ALL' ? query.status : undefined,
        page: query.page ?? 1,
        pageSize: query.pageSize ?? 10
      });
      courses.value = (res.data?.list || []).map(mapCourse);
      total.value = res.data?.total ?? courses.value.length;
    } catch (err) {
      courses.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchCourseDetail(id: number | string) {
    loading.value = true;
    const numId = Number(id);
    try {
      const res = await getCourseDetail(numId);
      currentCourse.value = mapCourse(res.data || {});
    } catch (err) {
      currentCourse.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentCourse.value;
  }

  function normalizeChapterTree(tree: any[]): Chapter[] {
    return (tree || []).map((item, idx) => {
      const subItems = item.children && item.children.length > 0 ? item.children : (item.sections || []);
      const sections = subItems.map((child: any, sIdx: number) => ({
        id: child.id || (item.id * 100 + sIdx + 1),
        title: child.title || child.name || `第 ${sIdx + 1} 课时`,
        description: child.description,
        completed: Boolean(child.completed),
        duration: child.duration || `${30 + (sIdx * 15) % 30}分钟`,
        knowledgePointCount: child.knowledgePointCount || (sIdx % 3 + 1),
        type: child.type || (sIdx % 2 === 1 ? 'quiz' : 'lecture')
      }));
      return {
        id: item.id,
        courseId: item.courseId,
        title: item.title,
        sort: item.sort ?? idx + 1,
        description: item.description || `本章涵盖学科核心理论基础与典型案例解析。`,
        sections,
        children: item.children
      };
    });
  }

  async function fetchChapters(courseId: number | string) {
    const numId = Number(courseId);
    try {
      const res = await getChapters(numId);
      chapters.value = normalizeChapterTree(res.data || []);
    } catch (err) {
      chapters.value = [];
      throw err;
    }
    return chapters.value;
  }

  async function createCourse(data: CourseCreateRequest): Promise<Course | null> {
    const res = await createCourseApi(data);
    const id = res.data;
    if (id) {
      await fetchCourseDetail(id);
      return currentCourse.value;
    }
    return null;
  }

  async function removeCourse(id: number) {
    await deleteCourse(id);
    courses.value = courses.value.filter(c => c.id !== id);
    total.value = courses.value.length;
  }

  async function saveCourse(id: number, data: Partial<CourseCreateRequest>) {
    await updateCourse(id, data);
    await fetchCourseDetail(id);
  }

  async function enrollCourseByCode(code: string): Promise<number | null> {
    const res = await joinCourseByCode(code);
    await fetchCourses();
    return res.data ?? null;
  }

  return {
    courses,
    currentCourse,
    chapters,
    loading,
    total,
    fetchCourses,
    fetchCourseDetail,
    fetchChapters,
    createCourse,
    removeCourse,
    saveCourse,
    enrollCourseByCode
  };
}
