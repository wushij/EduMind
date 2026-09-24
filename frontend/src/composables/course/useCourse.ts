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
  archiveCourse as archiveCourseApi,
  unarchiveCourse as unarchiveCourseApi,
  joinCourseByCode
} from '@/api/course/course';
import {
  createChapterApi,
  getChapters,
  updateChapterApi,
  deleteChapterApi
} from '@/api/course/chapter';
import { getCourseLessonProgressSummary } from '@/api/course/lesson';
import { mapCourse, normalizeCourseStatus } from '@/utils/course/map-course';

// 模块级单例响应式状态，确保跨组件、跨子路由切换时数据秒开不闪烁
const courses = ref<Course[]>([]);
const currentCourse = ref<Course | null>(null);
const chapters = ref<Chapter[]>([]);
const lessonProgressSummary = ref({ totalLessonCount: 0, completedLessonCount: 0 });
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

  function mapLessonType(lessonType?: string) {
    const t = (lessonType || 'LECTURE').toUpperCase();
    if (t === 'QUIZ') return 'quiz';
    if (t === 'PRACTICE') return 'practice';
    return 'lecture';
  }

  function normalizeChapterTree(tree: any[]): Chapter[] {
    return (tree || []).map((item, idx) => {
      const subItems = item.children && item.children.length > 0 ? item.children : (item.sections || []);
      const sections = subItems.map((child: any) => {
        const meta = child.lessonMeta || {};
        const durationMinutes = meta.durationMinutes ?? child.durationMinutes;
        return {
          id: child.id,
          title: child.title || child.name || '微课节',
          description: child.description,
          completed: Boolean(meta.completed ?? child.completed),
          duration: durationMinutes ? `${durationMinutes}分钟` : undefined,
          knowledgePointCount: meta.knowledgePointCount ?? child.knowledgePointCount,
          type: mapLessonType(meta.lessonType ?? child.lessonType ?? child.type),
          contentStatus: meta.contentStatus ?? child.contentStatus,
          hasContent: meta.hasContent ?? child.hasContent
        };
      });
      return {
        id: item.id,
        courseId: item.courseId,
        title: item.title,
        sort: item.sort ?? idx + 1,
        description: item.description,
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
      const summaryRes = await getCourseLessonProgressSummary(numId).catch(() => null);
      if (summaryRes?.data) {
        lessonProgressSummary.value = summaryRes.data;
      }
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
    payload: {
      title: string;
      sortOrder?: number;
      description?: string;
      durationMinutes?: number;
      lessonType?: string;
    }
  ) {
    const numId = Number(courseId);
    const res = await createChapterApi(numId, {
      title: payload.title,
      parentId: parentChapterId,
      sortOrder: payload.sortOrder ?? 1,
      description: payload.description,
      durationMinutes: payload.durationMinutes,
      lessonType: payload.lessonType
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
    await archiveCourseApi(id);
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

  async function unarchiveCourse(id: number) {
    await unarchiveCourseApi(id);
    const activeStatus = normalizeCourseStatus('ACTIVE');
    const patchStatus = (c: Course) =>
      c.id === id ? { ...c, status: activeStatus } : c;
    courses.value = courses.value.map(patchStatus);
    if (currentCourse.value?.id === id) {
      currentCourse.value = { ...currentCourse.value, status: activeStatus };
      courseCache.set(id, currentCourse.value);
    } else if (courseCache.has(id)) {
      courseCache.set(id, { ...courseCache.get(id)!, status: activeStatus });
    }
  }

  async function removeCourse(id: number) {
    await deleteCourse(id);
    courses.value = courses.value.filter((c) => c.id !== id);
    courseCache.delete(id);
    if (currentCourse.value?.id === id) {
      currentCourse.value = null;
    }
    total.value = Math.max(0, total.value - 1);
  }

  async function confirmArchiveCourse(course: { id: number; title?: string }, onSuccess?: () => void) {
    const title = course.title || '当前课程';
    try {
      await ElMessageBox.confirm(
        `确定将课程「${title}」结课归档吗？归档后学员将转入结课修读，您可随时在「已结课」列表中管理或彻底清理。`,
        '归档结课确认',
        {
          confirmButtonText: '确定归档',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await archiveCourse(course.id);
      ElMessage.success('课程已结课归档，可在课程列表「已结课」中查看');
      onSuccess?.();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '归档课程失败');
    }
  }

  async function confirmDeleteCourse(course: { id: number; title?: string }, onSuccess?: () => void) {
    const title = course.title || '当前课程';
    try {
      await ElMessageBox.confirm(
        `确定彻底删除课程「${title}」吗？\n该操作将同时清理课程空章节、知识点与资源。已有学生修读的课程将被系统安全拦截保护。此操作不可恢复！`,
        '彻底删除课程确认',
        {
          confirmButtonText: '确定彻底删除',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger',
          type: 'error'
        }
      );
      await removeCourse(course.id);
      ElMessage.success(`课程「${title}」已彻底删除`);
      onSuccess?.();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除课程失败');
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
    lessonProgressSummary,
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
    unarchiveCourse,
    confirmArchiveCourse,
    confirmDeleteCourse,
    saveCourse,
    enrollCourseByCode,
    setCurrentCourse
  };
}
