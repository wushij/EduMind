import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCourse } from '@/composables/course/useCourse';
import { getPublicCourses } from '@/api/course/course';
import type { Course } from '@/types/course/course';
import { isActiveCourseStatus, isArchivedCourseStatus } from '@/utils/course/map-course';

export type PublicCourseItem = {
  id?: number;
  title: string;
  code?: string;
  teacherName?: string;
  semester?: string;
  studentCount?: number;
};

export function countActiveCourses(courses: Course[]): number {
  return courses.filter(c => isActiveCourseStatus(c.status)).length;
}

export function countArchivedCourses(courses: Course[]): number {
  return courses.filter(c => isArchivedCourseStatus(c.status)).length;
}

export type CourseStatusSummary = {
  total: number;
  active: number;
  archived: number;
};

/** 在「全部课程」列表加载完成后，根据分页 total 汇总 Tab 角标（避免随 Tab 筛选变化） */
export function buildCourseStatusSummary(
  courses: Course[],
  total: number
): CourseStatusSummary {
  const safeTotal = Math.max(0, total);
  if (courses.length === 0 && safeTotal === 0) {
    return { total: 0, active: 0, archived: 0 };
  }
  if (courses.length >= safeTotal) {
    const active = countActiveCourses(courses);
    return { total: safeTotal, active, archived: safeTotal - active };
  }
  const activeOnPage = countActiveCourses(courses);
  const archivedOnPage = countArchivedCourses(courses);
  return {
    total: safeTotal,
    active: activeOnPage,
    archived: Math.max(archivedOnPage, safeTotal - activeOnPage)
  };
}

export function buildStatusTabs(total: number, activeCount: number, archivedCount: number) {
  return [
    { label: '全部课程', value: 'ALL', count: total },
    { label: '进行中', value: 'ACTIVE', count: activeCount },
    { label: '已结课', value: 'ARCHIVED', count: archivedCount }
  ];
}

// 模块级全局单例状态，保持上一次统计结果，避免跨路由切换时 Hero 与 Tab 数字频繁从 0 闪烁跳动
const globalCourseStatusSummary = ref<CourseStatusSummary>({ total: 0, active: 0, archived: 0 });

export function useCourseList() {
  const router = useRouter();
  const { courses, loading, total, fetchCourses, enrollCourseByCode } = useCourse();

  const searchKeyword = ref('');
  const currentStatusTab = ref('ALL');
  const selectedSemester = ref('ALL');
  const currentPage = ref(1);
  const pageSize = ref(10);

  const showJoinDialog = ref(false);
  const joinTab = ref<'code' | 'browse'>('code');
  const courseCodeInput = ref('');
  const joining = ref(false);

  // 若已有缓存课程且汇总尚未初始化，先就地初始化，达成 0 延迟秒开
  if (globalCourseStatusSummary.value.total === 0 && courses.value.length > 0) {
    globalCourseStatusSummary.value = buildCourseStatusSummary(courses.value, total.value || courses.value.length);
  }
  const courseStatusSummary = globalCourseStatusSummary;

  const allCoursesTotal = computed(() => courseStatusSummary.value.total);
  const activeCourseCount = computed(() => courseStatusSummary.value.active);
  const archivedCourseCount = computed(() => courseStatusSummary.value.archived);

  const statusTabs = computed(() =>
    buildStatusTabs(
      courseStatusSummary.value.total,
      courseStatusSummary.value.active,
      courseStatusSummary.value.archived
    )
  );

  async function loadData() {
    try {
      await fetchCourses({
        keyword: searchKeyword.value,
        status: currentStatusTab.value,
        page: currentPage.value,
        pageSize: pageSize.value
      });
      if (currentStatusTab.value === 'ALL') {
        courseStatusSummary.value = buildCourseStatusSummary(courses.value, total.value);
      }
    } catch {
      // axios 拦截器已弹出错误提示
    }
  }

  function handleStatusTabChange(tabVal: string) {
    currentStatusTab.value = tabVal;
    currentPage.value = 1;
    loadData();
  }

  function handleFilterChange() {
    currentPage.value = 1;
    loadData();
  }

  function clearKeyword() {
    searchKeyword.value = '';
    loadData();
  }

  function resetFilters() {
    searchKeyword.value = '';
    currentStatusTab.value = 'ALL';
    selectedSemester.value = 'ALL';
    currentPage.value = 1;
    loadData();
  }

  const realPublicCourses = ref<PublicCourseItem[]>([]);

  async function loadPublicCourses() {
    try {
      const res = await getPublicCourses();
      realPublicCourses.value = (res.data ?? []).map(c => ({
        id: c.id,
        title: c.title || c.name || '',
        code: c.code,
        teacherName: c.teacherName,
        semester: c.semester,
        studentCount: c.studentCount
      }));
    } catch {
      realPublicCourses.value = [];
      ElMessage.warning('公开课程列表加载失败，请稍后重试');
    }
  }

  function handleJoinCourse() {
    courseCodeInput.value = '';
    showJoinDialog.value = true;
    loadPublicCourses();
  }

  const publicCoursesList = computed(() => realPublicCourses.value);

  async function handleQuickJoin(code?: string) {
    if (!code) return;
    courseCodeInput.value = code;
    await handleJoinByCode();
  }

  async function handleJoinByCode() {
    if (!courseCodeInput.value.trim()) {
      ElMessage.warning('请输入课程代号或邀请码');
      return;
    }
    joining.value = true;
    try {
      const courseId = await enrollCourseByCode(courseCodeInput.value.trim());
      ElMessage.success('选课成功！已成功加入该课程空间！');
      showJoinDialog.value = false;
      courseCodeInput.value = '';
      loadData();
      if (courseId) {
        router.push(`/course/${courseId}/overview`);
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '加入课程失败，请检查课程代号是否存在');
    } finally {
      joining.value = false;
    }
  }

  onMounted(() => {
    loadData();
  });

  return {
    courses,
    loading,
    total,
    allCoursesTotal,
    searchKeyword,
    currentStatusTab,
    selectedSemester,
    currentPage,
    pageSize,
    showJoinDialog,
    joinTab,
    courseCodeInput,
    joining,
    activeCourseCount,
    archivedCourseCount,
    statusTabs,
    publicCoursesList,
    loadData,
    handleStatusTabChange,
    handleFilterChange,
    clearKeyword,
    resetFilters,
    handleJoinCourse,
    handleQuickJoin,
    handleJoinByCode
  };
}
