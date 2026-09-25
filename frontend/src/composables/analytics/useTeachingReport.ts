import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import type { TeachingReportMasteryStatus, TeachingReportVO } from '@/types/analytics/report';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { canAccessRoute } from '@/utils/router/route-access';

export interface KnowledgeMasteryItem {
  id: number | string;
  index: number;
  name: string;
  course: string;
  /** 班级掌握度 (0~100)；null 表示无实测掌握度记录，页面展示「暂无数据」 */
  rate: number | null;
  status: TeachingReportMasteryStatus;
  statusLabel: string;
  /** 知识点 ID：AI 诊断的聚焦参数必须使用它（传 questionId 会被后端判为无效 ID） */
  knowledgePointId?: number | string;
  /** 掌握度实测覆盖人数（有实测记录的学生数）；null 表示该考点无实测记录 */
  masterySampleCount?: number | null;
  /** 该考点累计测评次数；与覆盖人数成对展示，避免「人次」口径被误读 */
  masteryAssessmentCount?: number | null;
  /** 该考点聚合的错题条数（>1 表示多道错题已合并为一行） */
  wrongQuestionCount?: number;
  /** 错因类型是否为关键词推断（非库中显式标注） */
  errorTypeInferred?: boolean;
  questionId?: number | string;
  questionStem?: string;
  /** 原题题型：选择题（单选/多选/判断）才有选项可渲染 */
  questionType?: string;
  /** 原题选项 JSON 字符串（后端透出），由抽屉用 parseQuestionOptions 解析 */
  questionOptions?: string;
  /** 原题参考答案，用于标注正确选项 */
  questionAnswer?: string;
  errorType?: string;
  errorTypeName?: string;
  errorReason?: string;
  suggestion?: string;
}

/** 错因类型配色，与后端 ERROR_TYPE_LABELS 取值域保持一致 */
const errorColorMap: Record<string, string> = {
  CONCEPT: '#EF4444',
  LOGIC: '#3B82F6',
  CALC: '#F59E0B',
  READING: '#8B5CF6',
  TRANSFER: '#0EA5E9',
  MEMORY: '#64748B'
};

/** 掌握度 → 状态；无实测数据时为 unknown，避免用 0 分伪装成「急需攻坚」 */
function resolveStatus(rate: number | null): TeachingReportMasteryStatus {
  if (rate === null) return 'unknown';
  if (rate < 50) return 'danger';
  if (rate < 70) return 'warning';
  if (rate < 82) return 'normal';
  return 'good';
}

function resolveStatusLabel(status: TeachingReportMasteryStatus): string {
  switch (status) {
    case 'danger':
      return '急需攻坚';
    case 'warning':
      return '待巩固强化';
    case 'good':
      return '掌握良好';
    case 'unknown':
      return '暂无测评数据';
    default:
      return '稳步提升中';
  }
}

export function useTeachingReport(defaultCourseId?: number) {
  const router = useRouter();
  const { courseOptions, courseId, loading: coursesLoading, courseIdCorrected } = useTeacherCourses(defaultCourseId);
  const {
    fetchTeachingReport,
    fetchTeachingAdvice,
    stopTeachingAdvice,
    adviceLoading,
    teachingAdvice,
    loadStoredAdvice
  } = useLearningAnalytics();

  const loading = ref(false);
  const currentRange = ref<'7d' | '30d' | 'semester'>('7d');
  const reportData = ref<TeachingReportVO | null>(null);

  /** 班级测验及格率 (0~100)：后端基于已批改成绩计算，0 表示暂无样本 */
  const passRate = ref(0);
  /** 知识点全班平均掌握度 (0~100)；null 表示无数据，界面展示「--」 */
  const masteryRate = ref<number | null>(null);
  /** 掌握度是否含推算成分（无实测掌握度记录时后端会标记） */
  const masteryEstimated = ref(false);
  const aiCallCount = ref(0);
  const studentCount = ref(0);
  /** 教学大纲推进度 (0~100)：周期内有学习行为的章节数 / 总章节数 */
  const syllabusProgress = ref(0);
  const totalChapters = ref(0);
  /** AI 辅助批改估算节约工时（小时） */
  const savedHours = ref(0);
  const savedHoursEstimated = ref(true);
  /** 是否具备可用于分析的真实数据 */
  const hasRealData = ref(false);

  // 题目与错解下钻抽屉
  const questionDrawerVisible = ref(false);
  const selectedQuestion = ref<KnowledgeMasteryItem | null>(null);

  // AI 智能诊断推演弹窗
  const aiThinkingModalVisible = ref(false);
  /** 推演流水线最少展示时长（毫秒），保证罗盘波纹与秒表有可感知的运行过程 */
  const MIN_THINKING_MS = 1800;
  /** 是否处于推演中：用于区分「正常完成关闭」与「用户主动中止」 */
  let isThinkingInProgress = false;
  /** 用户是否点了中止：用于抑制中止后的失败提示 */
  let isThinkingAborted = false;
  /** 诊断成功生成计数：页面据此把视线定位到右栏的建议卡片 */
  const adviceGeneratedTick = ref(0);

  const courseName = computed(() => {
    if (reportData.value?.courseName) return reportData.value.courseName;
    const found = courseOptions.value.find((c) => Number(c.id) === Number(courseId.value));
    return found?.name || (courseId.value ? `课程 #${courseId.value}` : '全校教学质量监控');
  });

  const courseCode = computed(() => reportData.value?.courseCode || '');
  const teacherName = computed(() => reportData.value?.teacherName || '任课教师');

  const evaluationPeriod = computed(() => {
    const now = new Date();
    const month = now.getMonth() + 1;
    const semester = month >= 2 && month <= 7 ? '春季学期' : '秋季学期';
    const rangeLabel = currentRange.value === '30d' ? '近 30 天' : currentRange.value === 'semester' ? '本学期' : '近 7 天';
    return `${now.getFullYear()}${semester} · 教学质量评估 · 周期: ${rangeLabel}`;
  });

  /** 数据更新时间：取后端最近一次真实学习行为时间，无数据展示「暂无学习数据」 */
  const lastUpdatedTime = ref('暂无学习数据');

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
    // 切换/刷新课程后先还原该课程上一次已生成的教学诊断建议，避免面板空白
    loadStoredAdvice(courseId.value, null);
    loading.value = true;
    try {
      reportData.value = await fetchTeachingReport(courseId.value, currentRange.value);
      if (reportData.value) {
        passRate.value = reportData.value.passRate ?? 0;
        masteryRate.value = reportData.value.knowledgeMasteryAvg ?? null;
        masteryEstimated.value = reportData.value.masteryEstimated ?? false;
        aiCallCount.value = reportData.value.aiCallCount ?? 0;
        studentCount.value = reportData.value.studentCount ?? 0;
        syllabusProgress.value = reportData.value.syllabusProgress ?? 0;
        totalChapters.value = reportData.value.totalChapters ?? 0;
        savedHours.value = reportData.value.savedHours ?? 0;
        savedHoursEstimated.value = reportData.value.savedHoursEstimated ?? true;
        hasRealData.value = reportData.value.hasRealData ?? false;
        lastUpdatedTime.value = reportData.value.dataUpdatedAt || '暂无学习数据';

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
          // 掌握度只使用后端实测值，缺失即 null；不再用错误次数公式合成
          const rateVal = item.masteryRate ?? null;
          const stat = item.status ?? resolveStatus(rateVal);
          const sLabel = item.statusLabel || resolveStatusLabel(stat);

          return {
            id: item.questionId || index + 1,
            index: index + 1,
            name: item.knowledgePointName || item.title,
            course: cName,
            rate: rateVal,
            status: stat,
            statusLabel: sLabel,
            knowledgePointId: item.knowledgePointId,
            masterySampleCount: item.masterySampleCount ?? null,
            masteryAssessmentCount: item.masteryAssessmentCount ?? null,
            wrongQuestionCount: item.wrongQuestionCount ?? 1,
            errorTypeInferred: item.errorTypeInferred === true,
            questionId: item.questionId,
            questionStem: item.questionStem,
            questionType: item.questionType ?? undefined,
            questionOptions: item.questionOptions ?? undefined,
            questionAnswer: item.questionAnswer ?? undefined,
            errorType: item.errorType ?? undefined,
            errorTypeName: item.errorTypeName || '待归因',
            errorReason: item.errorReason ?? undefined,
            suggestion: item.suggestion ?? undefined
          };
        });
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

  async function handleOpenAiAdvice() {
    /*
     * 弹窗只是沉浸式推演外壳，必须等真实接口返回后主动收尾。
     * 旧实现只「开弹窗 + 发起请求」而不消费结果，导致后端无论成功、
     * 失败还是超时，界面都永远停在「推演中」，看起来像假诊断。
     */
    isThinkingAborted = false;
    isThinkingInProgress = true;
    aiThinkingModalVisible.value = true;

    /*
     * 聚焦参数必须是「知识点 ID」：后端按知识点查标题组装 Prompt。
     * 旧实现传的是 questionId（榜单条目的 id 来自题目），后端查不到就只能兜底成
     * 「考点#1001」这类占位编号，模型只能照抄，于是出现「按薄弱考点#1001分组」这种建议。
     * 这里改用 knowledgePointId，并去重、过滤无效值；全部无效时干脆不传，
     * 让后端回退到掌握度榜单里的真实薄弱考点。
     */
    const focusKpIds = Array.from(
      new Set(
        knowledgeMasteryList.value
          .map((k) => Number(k.knowledgePointId))
          .filter((id) => Number.isFinite(id) && id > 0)
      )
    );

    try {
      const [advice] = await Promise.all([
        fetchTeachingAdvice({
          courseId: courseId.value,
          focusKnowledgePointIds: focusKpIds.length > 0 ? focusKpIds : undefined
        }),
        new Promise((resolve) => setTimeout(resolve, MIN_THINKING_MS))
      ]);

      if (isThinkingAborted) {
        isThinkingInProgress = false;
        return;
      }

      isThinkingInProgress = false;
      aiThinkingModalVisible.value = false;

      if (!advice) {
        ElMessage.error('AI 教学诊断生成失败，请稍后重试');
        return;
      }

      adviceGeneratedTick.value += 1;
      ElMessage.success({
        message: '诊断已生成，请查看右侧「AI 教学策略改进建议」',
        duration: 3000
      });
    } catch {
      isThinkingInProgress = false;
      if (isThinkingAborted) return;
      aiThinkingModalVisible.value = false;
      ElMessage.error('生成教学诊断建议失败，请稍后重试');
    }
  }

  function handleStopAiAdvice() {
    // el-dialog 的 @close 同样会走到这里：仅推演中才提示中止，避免正常完成时误报
    if (!isThinkingInProgress) {
      aiThinkingModalVisible.value = false;
      return;
    }
    isThinkingInProgress = false;
    isThinkingAborted = true;
    stopTeachingAdvice();
    aiThinkingModalVisible.value = false;
    ElMessage.info('已中止本次 AI 教学推演');
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
    .kpi-note { font-size: 11px; color: #94a3b8; margin-top: 2px; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; }
    th { background: #f8fafc; color: #475569; }
    .tag { display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 11px; }
    .tag-danger { background: #fee2e2; color: #dc2626; }
    .tag-warning { background: #fef3c7; color: #d97706; }
    .tag-normal { background: #e0f2fe; color: #0284c7; }
    .tag-good { background: #d1fae5; color: #047857; }
    .tag-unknown { background: #f1f5f9; color: #64748b; }
    .footnote { margin-top: 20px; font-size: 12px; color: #94a3b8; line-height: 1.8; }
  </style>
</head>
<body>
  <div class="report-card">
    <h1>智教云 · 教学质量评估周报</h1>
    <div class="meta">${courseName.value}（${courseCode.value || '通用'}）· 任课教师: ${teacherName.value} · 评估周期: ${evaluationPeriod.value} · 数据更新: ${lastUpdatedTime.value}</div>
    <div class="kpi-grid">
      <div class="kpi-item"><div class="kpi-num">${hasRealData.value ? `${passRate.value}%` : '--'}</div><div class="kpi-label">班级测验及格率</div><div class="kpi-note">已批改成绩均分 ≥ 60 占比</div></div>
      <div class="kpi-item"><div class="kpi-num">${masteryRate.value === null ? '--' : `${masteryRate.value}%`}</div><div class="kpi-label">知识点全班掌握度</div><div class="kpi-note">${masteryEstimated.value ? '含作业分推算' : '实测掌握度聚合'}</div></div>
      <div class="kpi-item"><div class="kpi-num">${aiCallCount.value} 次</div><div class="kpi-label">AI 助教答疑频次</div><div class="kpi-note">来自 AI 调用日志</div></div>
      <div class="kpi-item"><div class="kpi-num">${savedHours.value} h</div><div class="kpi-label">AI 辅助批改节约工时</div><div class="kpi-note">按已批改 ${reportData.value.gradedCount ?? 0} 份估算</div></div>
    </div>
    <h3>核心薄弱考点掌握度榜单</h3>
    <table>
      <thead>
        <tr><th>排名</th><th>考点知识点</th><th>班级掌握度</th><th>状态</th><th>错因分类</th><th>诊断与建议</th></tr>
      </thead>
      <tbody>
        ${knowledgeMasteryList.value.map(k => `
          <tr>
            <td><strong>#${k.index}</strong></td>
            <td>${k.name}</td>
            <td><strong>${k.rate === null ? '暂无数据' : `${k.rate}%`}</strong></td>
            <td><span class="tag tag-${k.status}">${k.statusLabel}</span></td>
            <td>${k.errorTypeName || '—'}</td>
            <td>${k.suggestion || k.errorReason || '—'}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
    <div class="footnote">
      说明：本报告全部指标来自平台真实数据（作业提交与批改记录、AI 调用日志、知识点掌握度记录、错题诊断记录）。<br>
      测验及格率 = 已有批改成绩的学生中均分 ≥ 60 分的占比；大纲推进度 = 统计周期内有学习行为的章节占比；<br>
      AI 辅助批改节约工时按「已批改份数 × 单份人工批改均时 3 分钟」估算，仅用于效率参考。
    </div>
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
    totalChapters,
    currentRange,
    reportData,
    passRate,
    masteryRate,
    masteryEstimated,
    aiCallCount,
    savedHours,
    savedHoursEstimated,
    hasRealData,
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
    adviceGeneratedTick,
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
