import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import type { TeachingReportVO } from '@/types/analytics/report';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { canAccessRoute } from '@/utils/router/route-access';

export interface KnowledgeMasteryItem {
  id: number | string;
  index: number;
  name: string;
  course: string;
  rate: number;
  status: 'good' | 'normal' | 'warning' | 'danger';
  statusLabel: string;
  questionId?: number | string;
  questionStem?: string;
  errorType?: string;
  errorTypeName?: string;
  errorReason?: string;
  suggestion?: string;
}

const errorColorMap: Record<string, string> = {
  CONCEPT: '#EF4444',
  LOGIC: '#3B82F6',
  CALC: '#F59E0B'
};

export function useTeachingReport(defaultCourseId?: number) {
  const router = useRouter();
  const { courseOptions, courseId, loading: coursesLoading, courseIdCorrected } = useTeacherCourses(defaultCourseId);
  const { fetchTeachingReport, fetchTeachingAdvice, stopTeachingAdvice, adviceLoading, teachingAdvice } = useLearningAnalytics();

  const loading = ref(false);
  const currentRange = ref<'7d' | '30d' | 'semester'>('7d');
  const reportData = ref<TeachingReportVO | null>(null);
  const passRate = ref(0);
  const masteryRate = ref(0);
  const aiCallCount = ref(0);
  const studentCount = ref(0);
  const syllabusProgress = ref(0);

  // 题目与错解下钻抽屉
  const questionDrawerVisible = ref(false);
  const selectedQuestion = ref<KnowledgeMasteryItem | null>(null);

  // AI 智能诊断推演弹窗
  const aiThinkingModalVisible = ref(false);

  const courseName = computed(() => {
    if (reportData.value?.courseName) return reportData.value.courseName;
    const found = courseOptions.value.find((c) => Number(c.id) === Number(courseId.value));
    return found?.name || (courseId.value ? `课程 #${courseId.value}` : '全校教学质量监控');
  });

  const courseCode = computed(() => reportData.value?.courseCode || '');
  const teacherName = computed(() => reportData.value?.teacherName || '任课教师');

  const savedHours = computed(() => {
    if (!reportData.value) return 0;
    const calculated = Math.round((aiCallCount.value * 0.05 + (passRate.value ? 4.5 : 0)) * 10) / 10;
    return Math.max(0.5, calculated);
  });

  const evaluationPeriod = computed(() => {
    const now = new Date();
    const month = now.getMonth() + 1;
    const semester = month >= 2 && month <= 7 ? '春季学期' : '秋季学期';
    const rangeLabel = currentRange.value === '30d' ? '近 30 天' : currentRange.value === 'semester' ? '本学期' : '近 7 天';
    return `${now.getFullYear()}${semester} · 教学质量评估 · 周期: ${rangeLabel}`;
  });

  const lastUpdatedTime = ref('刚刚');

  const topWeakPointNames = computed(() => {
    if (!reportData.value?.weakPoints?.length) return '';
    return reportData.value.weakPoints
      .slice(0, 2)
      .map((w) => w.knowledgePointName || w.title)
      .join('》与《');
  });

  const knowledgeMasteryList = ref<KnowledgeMasteryItem[]>([]);
  const weeklyActivity = ref<Array<{ date: string; count: number }>>([]);
  const errorCategories = ref<Array<{ type: string; name: string; percent: number; color: string; desc: string }>>([]);

  async function loadReport() {
    if (!courseId.value) return;
    loading.value = true;
    try {
      reportData.value = await fetchTeachingReport(courseId.value, currentRange.value);
      if (reportData.value) {
        passRate.value = Math.round((reportData.value.avgSubmissionRate ?? 0) * 10) / 10;
        masteryRate.value = Math.round((reportData.value.knowledgeMasteryAvg ?? 0) * 100);
        aiCallCount.value = reportData.value.aiCallCount ?? 0;
        studentCount.value = reportData.value.studentCount ?? 0;
        syllabusProgress.value = reportData.value.syllabusProgress ?? 65;

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

        const cName = courseName.value;
        knowledgeMasteryList.value = (reportData.value.weakPoints ?? []).map((item, index) => {
          const rateVal = item.masteryRate ?? Math.max(32, 100 - (item.wrongCount ?? 1) * 12);
          const stat = item.status || (rateVal < 50 ? 'danger' : rateVal < 70 ? 'warning' : rateVal < 82 ? 'normal' : 'good');
          const sLabel = item.statusLabel || (stat === 'danger' ? '急需攻坚' : stat === 'warning' ? '待巩固强化' : '稳步提升中');

          return {
            id: item.questionId || index + 1,
            index: index + 1,
            name: item.knowledgePointName || item.title,
            course: cName,
            rate: rateVal,
            status: stat,
            statusLabel: sLabel,
            questionId: item.questionId,
            questionStem: item.questionStem,
            errorType: item.errorType,
            errorTypeName: item.errorTypeName || '典型错因',
            errorReason: item.errorReason,
            suggestion: item.suggestion || '建议进行考点变式练习与专项讲评'
          };
        });

        const now = new Date();
        lastUpdatedTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
      }
    } catch {
      ElMessage.error('加载教学报告失败');
    } finally {
      loading.value = false;
    }
  }

  function handleCourseChange(newCourseId: number) {
    courseId.value = newCourseId;
    localStorage.setItem('edumind_last_course_id', String(newCourseId));
    loadReport();
  }

  function handleRangeChange(range: '7d' | '30d' | 'semester') {
    currentRange.value = range;
    loadReport();
  }

  function handleOpenAiAdvice() {
    aiThinkingModalVisible.value = true;
    const focusKpIds = knowledgeMasteryList.value
      .map((k) => Number(k.questionId || k.id))
      .filter((id) => !Number.isNaN(id) && id > 0);

    fetchTeachingAdvice({
      courseId: courseId.value,
      focusKnowledgePointIds: focusKpIds.length > 0 ? focusKpIds : undefined
    });
  }

  function handleStopAiAdvice() {
    stopTeachingAdvice();
    aiThinkingModalVisible.value = false;
  }

  function handleViewQuestion(kp: KnowledgeMasteryItem) {
    selectedQuestion.value = kp;
    questionDrawerVisible.value = true;
  }

  function handleExportReport() {
    if (!reportData.value) {
      ElMessage.warning('暂无可导出的报告数据');
      return;
    }

    // 导出美化版 HTML 周报，支持浏览器直接预览或打印保存为 PDF
    const htmlContent = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>智教云 · 教学质量评估周报 - ${courseName.value}</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; padding: 40px; color: #1e293b; background: #f8fafc; }
    .report-card { max-width: 860px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
    h1 { color: #0f172a; margin-bottom: 8px; font-size: 24px; }
    .meta { color: #64748b; font-size: 14px; margin-bottom: 24px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
    .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 30px; }
    .kpi-item { background: #f1f5f9; padding: 16px; border-radius: 12px; text-align: center; }
    .kpi-num { font-size: 22px; font-weight: 700; color: #1677ff; }
    .kpi-label { font-size: 12px; color: #64748b; margin-top: 4px; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; }
    th { background: #f8fafc; color: #475569; }
    .tag { display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 11px; }
    .tag-danger { background: #fee2e2; color: #dc2626; }
    .tag-warning { background: #fef3c7; color: #d97706; }
    .tag-normal { background: #e0f2fe; color: #0284c7; }
  </style>
</head>
<body>
  <div class="report-card">
    <h1>智教云 · 教学质量评估周报</h1>
    <div class="meta">${courseName.value}（${courseCode.value || '通用'}）· 任课教师: ${teacherName.value} · 评估周期: ${evaluationPeriod.value}</div>
    <div class="kpi-grid">
      <div class="kpi-item"><div class="kpi-num">${passRate.value}%</div><div class="kpi-label">班级及格预测率</div></div>
      <div class="kpi-item"><div class="kpi-num">${masteryRate.value}%</div><div class="kpi-label">知识点全班掌握度</div></div>
      <div class="kpi-item"><div class="kpi-num">${aiCallCount.value} 次</div><div class="kpi-label">AI 助教答疑频次</div></div>
      <div class="kpi-item"><div class="kpi-num">${savedHours.value} 小时</div><div class="kpi-label">智能辅助节约工时</div></div>
    </div>
    <h3>核心薄弱考点掌握度榜单</h3>
    <table>
      <thead>
        <tr><th>排名</th><th>考点知识点</th><th>班级掌握度</th><th>状态</th><th>诊断与建议</th></tr>
      </thead>
      <tbody>
        ${knowledgeMasteryList.value.map(k => `
          <tr>
            <td><strong>#${k.index}</strong></td>
            <td>${k.name}</td>
            <td><strong>${k.rate}%</strong></td>
            <td><span class="tag tag-${k.status}">${k.statusLabel}</span></td>
            <td>${k.suggestion}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>
</body>
</html>`;

    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `教学质量评估周报-${courseName.value}-${new Date().toISOString().slice(0, 10)}.html`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('教学质量评估周报已生成并下载（HTML 格式，支持直接浏览器打印）');
  }

  function handleQuickQuiz(kp: KnowledgeMasteryItem) {
    if (!canAccessRoute(router, '/ai/question/generate')) {
      ElMessage.warning('快速组卷为教师专属功能');
      return;
    }
    router.push({
      path: '/ai/question/generate',
      query: {
        subject: courseName.value,
        knowledgePoint: kp.name,
        questionId: kp.questionId ? String(kp.questionId) : undefined
      }
    });
  }

  watch(courseId, () => {
    loadReport();
  });

  watch(courseIdCorrected, (corrected) => {
    if (corrected) {
      loadReport();
    }
  });

  onMounted(() => {
    if (courseId.value) {
      loadReport();
    }
  });

  return {
    router,
    loading,
    coursesLoading,
    courseOptions,
    courseId,
    courseName,
    courseCode,
    teacherName,
    studentCount,
    syllabusProgress,
    currentRange,
    reportData,
    passRate,
    masteryRate,
    aiCallCount,
    savedHours,
    evaluationPeriod,
    lastUpdatedTime,
    topWeakPointNames,
    knowledgeMasteryList,
    weeklyActivity,
    errorCategories,
    questionDrawerVisible,
    selectedQuestion,
    aiThinkingModalVisible,
    adviceLoading,
    teachingAdvice,
    loadReport,
    handleCourseChange,
    handleRangeChange,
    handleOpenAiAdvice,
    handleStopAiAdvice,
    handleViewQuestion,
    handleExportReport,
    handleQuickQuiz
  };
}

