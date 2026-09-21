import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getLearningHomeOverview } from '@/api/learning/home';
import { getCourseList } from '@/api/course/course';
import { getCourseDisplayName } from '@/utils/course/course-display';
import type {
  LearningHomeOverviewVO,
  LearningHomeTaskUI,
  LearningHomeWeakPointUI
} from '@/types/learning/home';
import type { Course } from '@/types/course/course';

export function useLearningHome() {
  const loading = ref(false);
  const overview = ref<LearningHomeOverviewVO | null>(null);
  const selectedCourseId = ref<number | null>(null);
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);

  const heroStats = computed(() => {
    const s = overview.value?.summary;
    const studyHours =
      s?.totalStudyMinutes != null ? (Math.round((s.totalStudyMinutes / 60) * 10) / 10).toFixed(1) : '0';
    return {
      progressPercent: s?.avgCourseProgressPercent ?? 0,
      studyHours,
      completedTasks: s?.completedTasks ?? 0,
      totalTasks: s?.totalTasks ?? 0,
      masteryPercent: s?.overallMasteryPercent ?? 0
    };
  });

  const weakPoints = computed<LearningHomeWeakPointUI[]>(() =>
    (overview.value?.weakPoints ?? []).map((wp) => ({
      id: `wp-${wp.courseId}-${wp.knowledgePointId}`,
      name: wp.title,
      course: wp.courseName,
      courseId: wp.courseId,
      knowledgePointId: wp.knowledgePointId,
      mastery: wp.mastery,
      level: wp.level,
      reason: wp.suggestion
    }))
  );

  const todayTasks = computed<LearningHomeTaskUI[]>(() =>
    (overview.value?.todayTasks ?? []).map((t) => ({
      id: t.id,
      title: t.title,
      course: t.courseName,
      courseId: t.courseId,
      type: t.type,
      estimatedMinutes: t.estimatedMinutes,
      completed: t.status === 'COMPLETED',
      targetUrl: t.targetUrl
    }))
  );

  const completionRate = computed(() => {
    const { completedTasks, totalTasks } = heroStats.value;
    if (!totalTasks) return '0/0';
    return `${completedTasks}/${totalTasks}`;
  });

  const primaryCourseId = computed(() =>
    selectedCourseId.value ?? overview.value?.primaryCourseId ?? courseOptions.value[0]?.id ?? null
  );

  const hasEnrolledCourses = computed(() => (overview.value?.courses?.length ?? 0) > 0);

  async function loadCourseOptions() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = (res.data?.list ?? []) as Course[];
      courseOptions.value = list.map((c) => ({
        id: c.id,
        name: getCourseDisplayName(c)
      }));
    } catch {
      courseOptions.value = [];
    }
  }

  async function loadOverview(primaryCourseId?: number) {
    loading.value = true;
    try {
      const res = await getLearningHomeOverview(
        primaryCourseId != null ? { primaryCourseId } : undefined
      );
      overview.value = res.data ?? null;
      if (overview.value?.courses && overview.value.courses.length > 0) {
        courseOptions.value = overview.value.courses.map((c) => ({
          id: c.courseId,
          name: c.courseName || `课程 #${c.courseId}`
        }));
      }
      if (overview.value?.primaryCourseId != null) {
        selectedCourseId.value = overview.value.primaryCourseId;
      }
    } catch (err: unknown) {
      overview.value = null;
      ElMessage.error(err instanceof Error ? err.message : '加载学习总览失败');
    } finally {
      loading.value = false;
    }
  }

  async function refresh(primaryCourseId?: number) {
    await loadCourseOptions();
    await loadOverview(primaryCourseId ?? selectedCourseId.value ?? undefined);
  }

  async function changePrimaryCourse(courseId: number) {
    selectedCourseId.value = courseId;
    await loadOverview(courseId);
  }

  return {
    loading,
    overview,
    selectedCourseId,
    courseOptions,
    primaryCourseId,
    heroStats,
    weakPoints,
    todayTasks,
    completionRate,
    hasEnrolledCourses,
    loadCourseOptions,
    loadOverview,
    refresh,
    changePrimaryCourse
  };
}
