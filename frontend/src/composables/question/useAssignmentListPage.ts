import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
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

  const aiThinkingVisible = ref(false);
  const activeAssignmentTitle = ref('');
  let abortController: AbortController | null = null;

  async function handleFastAIGrade(assignmentId: number) {
    const target = assignments.value.find(a => a.id === assignmentId);
    activeAssignmentTitle.value = target?.title ? `AI 智能阅卷引擎 · ${target.title}` : 'AI 智能阅卷推演引擎';

    abortController = new AbortController();
    aiThinkingVisible.value = true;

    try {
      let pendingCount = await gradePendingSubmissions(assignmentId, {
        signal: abortController.signal
      });

      aiThinkingVisible.value = false;

      if (pendingCount > 0) {
        ElMessage.success(`AI 智能预评完成！已成功批改 ${pendingCount} 份答卷`);
        await refresh();
      } else {
        // 如果没有新提交的待评答卷，询问是否进行重新评估
        try {
          await ElMessageBox.confirm(
            `该作业下暂无新提交的待评答卷（已有学生答卷均已完成初次批改）。\n\n是否对已有答卷【重新执行一轮 AI 智能复评】？`,
            'AI 复评确认',
            {
              confirmButtonText: '重新评阅答卷',
              cancelButtonText: '取消',
              type: 'info'
            }
          );
          abortController = new AbortController();
          aiThinkingVisible.value = true;
          pendingCount = await gradePendingSubmissions(assignmentId, {
            signal: abortController.signal,
            forceRegrade: true
          });
          aiThinkingVisible.value = false;
          if (pendingCount > 0) {
            ElMessage.success(`已成功为 ${pendingCount} 份答卷重新完成 AI 智能复评！`);
            await refresh();
          } else {
            ElMessage.info('当前暂无可批改的学生答卷');
          }
        } catch (confirmErr) {
          if (confirmErr === 'cancel' || confirmErr === 'close') {
            return;
          }
          throw confirmErr;
        }
      }
    } catch (err: unknown) {
      aiThinkingVisible.value = false;
      if (err instanceof DOMException && err.name === 'AbortError') {
        ElMessage.info('已中止本次 AI 阅卷推演');
        await refresh();
        return;
      }
      ElMessage.error(err instanceof Error ? err.message : 'AI 批改请求失败');
    } finally {
      abortController = null;
    }
  }

  function handleAbortAIGrade() {
    if (abortController) {
      abortController.abort();
    }
    aiThinkingVisible.value = false;
  }

  onMounted(async () => {
    await Promise.all([loadCourseOptions(), refresh()]);
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
    aiThinkingVisible,
    activeAssignmentTitle,
    loadAssignments: refresh,
    handleSearch,
    getCourseName,
    progressPercent,
    progressLabel,
    formatSubmissionRate,
    handleFastAIGrade,
    handleAbortAIGrade,
    removeAssignment
  };
}
