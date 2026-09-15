import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCourse } from '@/composables/course/useCourse';
import { getPublicCourses } from '@/api/course/course';
import type { Course } from '@/types/course/course';

export const DEFAULT_PUBLIC_COURSES = [
  { id: 1, title: '数据结构与算法', code: 'CS201', teacherName: '张老师', semester: '2026秋季学期', studentCount: 38 },
  { id: 2, title: 'Java企业级架构设计', code: 'CS101', teacherName: '李教授', semester: '2026秋季学期', studentCount: 45 },
  { id: 3, title: '高等数学与工程代数', code: 'MATH101', teacherName: '王老师', semester: '2026秋季学期', studentCount: 62 },
  { id: 4, title: '人工智能与知识图谱', code: 'AI102', teacherName: '陈博士', semester: '2026秋季学期', studentCount: 50 }
] as const;

export type PublicCourseItem = {
  id?: number;
  title: string;
  code?: string;
  teacherName?: string;
  semester?: string;
  studentCount?: number;
};

export function countActiveCourses(courses: Course[]): number {
  return courses.filter(c => c.status === 'ACTIVE' || c.status === 1).length;
}

export function countArchivedCourses(courses: Course[]): number {
  return courses.filter(c => c.status === 'ARCHIVED' || c.status === 2).length;
}

export function buildStatusTabs(total: number, activeCount: number, archivedCount: number) {
  return [
    { label: '全部课程', value: 'ALL', count: total },
    { label: '进行中', value: 'ACTIVE', count: activeCount },
    { label: '已结课', value: 'ARCHIVED', count: archivedCount }
  ];
}

export function resolvePublicCoursesList(courses: Course[]): PublicCourseItem[] {
  if (courses && courses.length > 0) {
    return courses.slice(0, 4);
  }
  return [...DEFAULT_PUBLIC_COURSES];
}

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

  const activeCourseCount = computed(() => countActiveCourses(courses.value));
  const archivedCourseCount = computed(() => countArchivedCourses(courses.value));

  const statusTabs = computed(() =>
    buildStatusTabs(total.value, activeCourseCount.value, archivedCourseCount.value)
  );

  async function loadData() {
    try {
      await fetchCourses({
        keyword: searchKeyword.value,
        status: currentStatusTab.value,
        page: currentPage.value,
        pageSize: pageSize.value
      });
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
      if (res.data && res.data.length > 0) {
        realPublicCourses.value = res.data.map(c => ({
          id: c.id,
          title: c.title || c.name || '',
          code: c.code,
          teacherName: c.teacherName,
          semester: c.semester,
          studentCount: c.studentCount
        }));
      }
    } catch {
      // 保持降级
    }
  }

  function handleJoinCourse() {
    courseCodeInput.value = '';
    showJoinDialog.value = true;
    loadPublicCourses();
  }

  const publicCoursesList = computed(() => {
    if (realPublicCourses.value.length > 0) {
      return realPublicCourses.value;
    }
    return resolvePublicCoursesList(courses.value);
  });

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
