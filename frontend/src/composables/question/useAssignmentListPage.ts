import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAssignmentStats } from '@/api/question/assignment';
import { useAssignment } from '@/composables/question/useAssignment';
import type { Assignment, AssignmentStats } from '@/types/question/assignment';
import type { Course } from '@/types/course/course';
import { formatSubmissionRate } from '@/constants/question/assignment';

export function useAssignmentListPage() {
  const router = useRouter();
  const {
    assignments,
    loading,
    total,
    fetchAssignments,
    loadCourses,
    gradePendingSubmissions,
    removeAssignment
  } = useAssignment();

  const stats = ref<AssignmentStats | null>(null);
  const pageReady = ref(false);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const courses = ref<Course[]>([]);
  const searchKeyword = ref('');
  const selectedCourseId = ref<number | null>(null);
  const selectedStatus = ref('');

  const avgSubmissionRateLabel = computed(() => {
    const rate = stats.value?.avgSubmissionRate;
    if (rate === undefined || rate === null || Number.isNaN(rate)) {
      return '—';
    }
    return `${Math.round(rate * 10) / 10}%`;
  });

  async function loadCourseOptions() {
    courses.value = await loadCourses();
  }

  async function loadStats() {
    try {
      const res = await getAssignmentStats({
        courseId: selectedCourseId.value || undefined
      });
      stats.value = res.data ?? null;
    } catch {
      stats.value = null;
    }
  }

  async function refresh() {
    pageReady.value = false;
    try {
      await fetchAssignments({
        page: pageNum.value,
        pageSize: pageSize.value,
        courseId: selectedCourseId.value || undefined,
        status: selectedStatus.value || undefined,
        keyword: searchKeyword.value.trim() || undefined
      });
      await loadStats();
    } finally {
      pageReady.value = true;
    }
  }

  function handleSearch() {
    pageNum.value = 1;
    refresh();
  }

  function getCourseName(courseId?: number) {
    const c = courses.value.find(item => item.id === courseId);
    return c?.title || c?.name || '—';
  }

  function progressPercent(assignment: Assignment) {
    const totalStudents = assignment.studentCount || 0;
    const submitted = assignment.submissionCount || 0;
    if (!totalStudents) return 0;
    return Math.min(100, Math.round((submitted / totalStudents) * 100));
  }

  function progressLabel(assignment: Assignment) {
    const totalStudents = assignment.studentCount || 0;
    const submitted = assignment.submissionCount || 0;
    return `${submitted} / ${totalStudents || '—'} 人`;
  }

  async function handleFastAIGrade(assignmentId: number) {
    try {
      const pendingCount = await gradePendingSubmissions(assignmentId);
      if (pendingCount > 0) {
        ElMessage.success(`已为 ${pendingCount} 份答卷触发 AI 智能预评`);
      } else {
        ElMessage.info('当前暂无待预评的答卷');
      }
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : 'AI 批改请求失败');
    }
    router.push(`/ai/grading?assignmentId=${assignmentId}`);
  }

  onMounted(async () => {
    await loadCourseOptions();
    await refresh();
  });

  return {
    router,
    assignments,
    loading,
    total,
    stats,
    pageReady,
    pageNum,
    pageSize,
    courses,
    searchKeyword,
    selectedCourseId,
    selectedStatus,
    avgSubmissionRateLabel,
    loadAssignments: refresh,
    handleSearch,
    getCourseName,
    progressPercent,
    progressLabel,
    formatSubmissionRate,
    handleFastAIGrade,
    removeAssignment
  };
}
