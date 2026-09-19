import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getLearningReport } from '@/api/learning/report';
import type { LearningReportVO } from '@/types/learning/report';
import type { StudentPortraitVO } from '@/types/analytics/learning';

export function useLearningReport() {
  const route = useRoute();
  const loading = ref(false);
  const report = ref<LearningReportVO | null>(null);
  const courseId = ref<number>(0);
  const timeRange = ref('30d');
  const notEnrolled = ref(false);

  const courseOptions = computed(() =>
    (report.value?.enrolledCourses ?? []).map((c) => ({
      id: c.courseId,
      name: c.courseName
    }))
  );

  const portrait = computed<StudentPortraitVO | null>(() => report.value?.portrait ?? null);

  const reportTrends = computed(() => report.value?.trends ?? null);

  const reportCode = computed(() => {
    const id = courseId.value || report.value?.courseId || 0;
    const d = new Date();
    const ymd = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`;
    return `EM-RPT-${id}-${ymd}`;
  });

  function syncCourseFromRoute() {
    const q = Number(route.query.courseId);
    if (q && !Number.isNaN(q)) {
      courseId.value = q;
    }
  }

  async function loadReport() {
    loading.value = true;
    notEnrolled.value = false;
    try {
      const params: { courseId?: number; range: string } = { range: timeRange.value };
      if (courseId.value) {
        params.courseId = courseId.value;
      }
      const res = await getLearningReport(params);
      report.value = res.data ?? null;
      const enrolled = report.value?.enrolledCourses ?? [];
      if (enrolled.length === 0) {
        notEnrolled.value = true;
        return;
      }
      if (report.value?.courseId) {
        courseId.value = report.value.courseId;
      } else if (!courseId.value && enrolled.length) {
        courseId.value = enrolled[0].courseId;
      }
    } catch (err: unknown) {
      report.value = null;
      const msg = err instanceof Error ? err.message : '加载学情报告失败';
      if (/未加入|尚未加入|无法查看/.test(msg)) {
        notEnrolled.value = true;
      } else {
        ElMessage.error(msg);
      }
    } finally {
      loading.value = false;
    }
  }

  async function init() {
    syncCourseFromRoute();
    const qRange = route.query.range as string | undefined;
    if (qRange) {
      timeRange.value = qRange;
    }
    await loadReport();
  }

  watch(
    () => route.query.courseId,
    async () => {
      syncCourseFromRoute();
      await loadReport();
    }
  );

  return {
    loading,
    report,
    courseId,
    courseOptions,
    timeRange,
    portrait,
    reportTrends,
    notEnrolled,
    reportCode,
    loadReport,
    init
  };
}
