import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
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
import {
  createChapterApi,
  getChapters,
  updateChapterApi,
  deleteChapterApi
} from '@/api/course/chapter';
import { mapCourse, normalizeCourseStatus } from '@/utils/course/map-course';

// 模块级单例响应式状态，确保跨组件、跨子路由切换时数据秒开不闪烁
const courses = ref<Course[]>([]);
const currentCourse = ref<Course | null>(null);
const chapters = ref<Chapter[]>([]);
const loading = ref(false);
const total = ref(0);
const courseCache = new Map<number, Course>();

export function useCourse() {

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
    const numId = Number(id);
    // 1. 如果当前已有该课程且ID匹配，直接静默后台刷新，彻底避免前台白屏/占位闪烁
    if (currentCourse.value?.id === numId) {
      void getCourseDetail(numId).then(res => {
        if (res.data) {
          const mapped = mapCourse(res.data);
          currentCourse.value = mapped;
          courseCache.set(numId, mapped);
        }
      }).catch(() => {});
      return currentCourse.value;
    }

    // 2. 如果内存缓存有该课程，先立即呈现
    if (courseCache.has(numId)) {
      currentCourse.value = courseCache.get(numId)!;
    }

    loading.value = true;
    try {
      const res = await getCourseDetail(numId);
      const mapped = mapCourse(res.data || {});
      currentCourse.value = mapped;
      courseCache.set(numId, mapped);
    } catch (err) {
      if (!currentCourse.value) {
        currentCourse.value = null;
      }
      throw err;
    } finally {
      loading.value = false;
    }
    return currentCourse.value;
  }

  function setCurrentCourse(course: Course | null) {
    if (!course) return;
    currentCourse.value = course;
    if (course.id) {
      courseCache.set(course.id, course);
    }
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

  async function createChapter(
    courseId: number | string,
    payload: { title: string; parentId?: number; sortOrder?: number; description?: string }
  ) {
    const numId = Number(courseId);
    const res = await createChapterApi(numId, {
      title: payload.title,
      parentId: payload.parentId ?? 0,
      sortOrder: payload.sortOrder
    });
    await fetchChapters(numId);
    return res.data;
  }

  async function updateChapter(
    courseId: number | string,
    chapterId: number,
    payload: { title: string; sortOrder?: number }
  ) {
    const numId = Number(courseId);
    await updateChapterApi(numId, chapterId, payload);
    await fetchChapters(numId);
  }

  async function removeChapter(courseId: number | string, chapterId: number) {
    const numId = Number(courseId);
    await deleteChapterApi(numId, chapterId);
    await fetchChapters(numId);
  }

  async function createSection(
    courseId: number | string,
    parentChapterId: number,
    payload: { title: string; sortOrder?: number }
  ) {
    const numId = Number(courseId);
    const res = await createChapterApi(numId, {
      title: payload.title,
      parentId: parentChapterId,
      sortOrder: payload.sortOrder ?? 1
    });
    await fetchChapters(numId);
    return res.data;
  }

  async function removeSection(courseId: number | string, sectionId: number) {
    const numId = Number(courseId);
    await deleteChapterApi(numId, sectionId);
    await fetchChapters(numId);
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

  async function archiveCourse(id: number) {
    await deleteCourse(id);
    const archivedStatus = normalizeCourseStatus('ARCHIVED');
    const patchStatus = (c: Course) =>
      c.id === id ? { ...c, status: archivedStatus } : c;
    courses.value = courses.value.map(patchStatus);
    if (currentCourse.value?.id === id) {
      currentCourse.value = { ...currentCourse.value, status: archivedStatus };
      courseCache.set(id, currentCourse.value);
    } else if (courseCache.has(id)) {
      courseCache.set(id, { ...courseCache.get(id)!, status: archivedStatus });
    }
  }

  async function confirmArchiveCourse(course: { id: number; title?: string }, onSuccess?: () => void) {
    const title = course.title || '当前课程';
    try {
      await ElMessageBox.confirm(
        `确定归档课程「${title}」吗？归档后学员将无法继续访问该课程空间，且不可恢复为进行中状态。`,
        '归档课程确认',
        {
          confirmButtonText: '确定归档',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await archiveCourse(course.id);
      ElMessage.success('课程已归档，可在课程列表「已结课」中查看');
      onSuccess?.();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '归档课程失败');
    }
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
    createChapter,
    updateChapter,
    removeChapter,
    createSection,
    removeSection,
    createCourse,
    archiveCourse,
    confirmArchiveCourse,
    saveCourse,
    enrollCourseByCode,
    setCurrentCourse
  };
}
