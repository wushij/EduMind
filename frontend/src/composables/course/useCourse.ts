import { ref } from 'vue';
import { Course, CourseQuery, CourseCreateRequest } from '@/types/course/course';
import { Chapter } from '@/types/course/chapter';
import { getCourseList, getCourseDetail, createCourse as createCourseApi, updateCourse, deleteCourse } from '@/api/course/course';
import { getChapters } from '@/api/course/chapter';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';
import { MOCK_CHAPTERS } from '@/mock/chapters';
import { mapCourse } from '@/utils/course/map-course';

export function useCourse() {
  const courses = ref<Course[]>([]);
  const currentCourse = ref<Course | null>(null);
  const chapters = ref<Chapter[]>([]);
  const loading = ref(false);
  const total = ref(0);

  function filterCourses(source: Course[], query: CourseQuery): Course[] {
    let result = [...source];
    if (query.keyword?.trim()) {
      const kw = query.keyword.trim().toLowerCase();
      result = result.filter(
        c =>
          c.title?.toLowerCase().includes(kw) ||
          c.name?.toLowerCase().includes(kw) ||
          c.teacherName?.toLowerCase().includes(kw) ||
          c.code?.toLowerCase().includes(kw)
      );
    }
    if (query.status && query.status !== 'ALL') {
      result = result.filter(c => String(c.status) === String(query.status));
    }
    if (query.semester && query.semester !== 'ALL') {
      result = result.filter(c => c.semester === query.semester);
    }
    return result;
  }

  async function fetchCourses(query: CourseQuery = {}) {
    loading.value = true;
    try {
      const res = await getCourseList(query);
      const list = (res.data?.list || []).map(mapCourse);
      courses.value = filterCourses(list, query);
      total.value = res.data?.total ?? courses.value.length;
    } catch {
      if (USE_MOCK) {
        courses.value = filterCourses(MOCK_COURSES, query);
        total.value = courses.value.length;
      } else {
        courses.value = [];
        total.value = 0;
      }
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
    } catch {
      if (USE_MOCK) {
        currentCourse.value = MOCK_COURSES.find(c => c.id === numId) || MOCK_COURSES[0];
      } else {
        currentCourse.value = null;
      }
    } finally {
      loading.value = false;
    }
    return currentCourse.value;
  }

  async function fetchChapters(courseId: number | string) {
    const numId = Number(courseId);
    try {
      const res = await getChapters(numId);
      chapters.value = (res.data || []) as Chapter[];
    } catch {
      chapters.value = USE_MOCK ? ((MOCK_CHAPTERS[numId] || MOCK_CHAPTERS[101] || []) as Chapter[]) : [];
    }
    return chapters.value;
  }

  async function createCourse(data: CourseCreateRequest): Promise<Course | null> {
    try {
      const res = await createCourseApi(data);
      const id = res.data;
      if (id) {
        await fetchCourseDetail(id);
        return currentCourse.value;
      }
    } catch {
      if (USE_MOCK) {
        const newCourse: Course = {
          id: Date.now(),
          title: data.name,
          name: data.name,
          code: data.code || `EDU${Math.floor(1000 + Math.random() * 9000)}`,
          teacherName: '当前教师',
          semester: data.semester || '2026秋季学期',
          studentCount: 0,
          chapterCount: 0,
          status: 'ACTIVE',
          coverUrl: data.coverUrl,
          cover: data.coverUrl,
          description: data.description
        };
        courses.value.unshift(newCourse);
        total.value = courses.value.length;
        return newCourse;
      }
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
    saveCourse
  };
}
