import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import type { TeachingReportVO } from '@/types/analytics/report';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { canAccessRoute } from '@/utils/router/route-access';

export interface KnowledgeMasteryItem {
  id: number;
  index: number;
  name: string;
  course: string;
  rate: number;
  status: 'good' | 'normal' | 'warning' | 'danger';
  statusLabel: string;
}

const errorColorMap: Record<string, string> = {
  CONCEPT: '#EF4444',
  LOGIC: '#3B82F6',
  CALC: '#F59E0B'
};

export function useTeachingReport(defaultCourseId = 102) {
  const router = useRouter();
  const { courseId } = useTeacherCourses(defaultCourseId);
  const { fetchTeachingReport } = useLearningAnalytics();

  const reportData = ref<TeachingReportVO | null>(null);
  const passRate = ref(0);
  const masteryRate = ref(0);
  const aiCallCount = ref(0);

  const savedHours = computed(() => {
    if (!reportData.value) return 0;
    const calculated = Math.round((aiCallCount.value * 0.05 + (passRate.value ? 4.5 : 0)) * 10) / 10;
    return Math.max(0.5, calculated);
  });

  const evaluationPeriod = computed(() => {
    const now = new Date();
    const month = now.getMonth() + 1;
    const semester = month >= 2 && month <= 7 ? '春季学期' : '秋季学期';
    const rangeText = reportData.value?.range ? ` · 统计周期: ${reportData.value.range}` : '';
    return `${now.getFullYear()}${semester} · 教学质量评估${rangeText}`;
  });

  const topWeakPointNames = computed(() => {
    if (!reportData.value?.weakPoints?.length) return '';
    return reportData.value.weakPoints
      .slice(0, 2)
      .map((w) => w.title)
      .join('》与《');
  });

  const knowledgeMasteryList = ref<KnowledgeMasteryItem[]>([]);
  const weeklyActivity = ref<Array<{ date: string; count: number }>>([]);
  const errorCategories = ref<Array<{ type: string; name: string; percent: number; color: string; desc: string }>>([]);

  async function loadReport() {
    try {
      reportData.value = await fetchTeachingReport(courseId.value);
      if (reportData.value) {
        passRate.value = reportData.value.avgSubmissionRate ?? 0;
        masteryRate.value = Math.round((reportData.value.knowledgeMasteryAvg ?? 0) * 100);
        aiCallCount.value = reportData.value.aiCallCount ?? 0;
        weeklyActivity.value = (reportData.value.weeklyActivity ?? []).map((item) => ({
          date: item.date,
          count: item.count
        }));
        errorCategories.value = (reportData.value.errorCategories ?? []).map((item) => ({
          type: item.type,
          name: item.name,
          percent: item.percent,
          color: errorColorMap[item.type] ?? '#94A3B8',
          desc: item.name
        }));
        knowledgeMasteryList.value = (reportData.value.weakPoints ?? []).map((item, index) => ({
          id: index + 1,
          index: index + 1,
          name: item.title,
          course: `课程 #${courseId.value}`,
          rate: Math.max(30, 100 - (item.wrongCount ?? 1) * 12),
          status: (item.wrongCount ?? 1) >= 4 ? 'danger' : (item.wrongCount ?? 1) >= 2 ? 'warning' : 'normal',
          statusLabel: item.suggestion || '建议巩固强化'
        }));
      }
    } catch {
      ElMessage.error('加载教学报告失败');
    }
  }

  function handleExportReport() {
    if (!reportData.value) {
      ElMessage.warning('暂无可导出的报告数据');
      return;
    }
    const blob = new Blob([JSON.stringify(reportData.value, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `teaching-report-${courseId.value}.json`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('教学报告已导出为 JSON');
  }

  function handleQuickQuiz(kp: KnowledgeMasteryItem) {
    // 快速组卷复用 AI 出题工作台（教师模块），非教师角色前置拦截并说明原因
    if (!canAccessRoute(router, '/ai/question/generate')) {
      ElMessage.warning('快速组卷为教师专属功能');
      return;
    }
    router.push({
      path: '/ai/question/generate',
      query: {
        subject: kp.course,
        knowledgePoint: kp.name
      }
    });
  }

  onMounted(loadReport);

  return {
    router,
    courseId,
    reportData,
    passRate,
    masteryRate,
    aiCallCount,
    savedHours,
    evaluationPeriod,
    topWeakPointNames,
    knowledgeMasteryList,
    weeklyActivity,
    errorCategories,
    handleExportReport,
    handleQuickQuiz
  };
}
