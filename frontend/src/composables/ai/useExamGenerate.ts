import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Exam, ExamRule } from '@/types/question/exam';
import {
  composeSmartPaper,
  cancelSmartPaperCompose,
  swapPaperQuestion
} from '@/api/ai/paper-compose';
import { loadGenerationCourseOptions } from '@/services/ai/generation-service';
import { getChapters } from '@/api/course/chapter';
import { getKnowledgePoints } from '@/api/course/knowledge-point';
import { getQuestions } from '@/api/question/question';
import { createExam } from '@/api/question/exam';
import { normalizeQuestion } from '@/utils/question/normalize-question';
import type { SmartPaperComposeRequest, SmartPaperComposeVO } from '@/types/ai/paper-compose';

const currentExam = ref<Exam>({
  id: 0,
  courseId: 0,
  courseName: '',
  title: '',
  semester: '2025-2026学年第二学期',
  totalScore: 100,
  durationMinutes: 90,
  passScore: 60,
  rules: [],
  questions: [],
  createdAt: ''
});

const examQualityAssessment = ref<string>('');
const aiGeneratedCount = ref<number>(0);
const bankExtractedCount = ref<number>(0);

export function useExamGenerate() {
  const router = useRouter();
  const generating = ref(false);
  const composing = ref(false);
  const courses = ref<any[]>([]);
  const courseChapters = ref<any[]>([]);
  const courseKnowledgePoints = ref<any[]>([]);
  const courseBankQuestions = ref<any[]>([]);
  const loadingChapters = ref(false);
  const loadingKps = ref(false);
  const loadingBank = ref(false);

  const examForm = reactive({
    courseId: 101,
    title: '期末专业综合能力测评',
    totalScore: 100,
    durationMinutes: 90,
    chapterIds: [] as number[],
    knowledgePointIds: [] as number[],
    promptDirective: '',
    aiGenerateFillShortfall: true,
    rules: [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 },
      { type: 'MULTIPLE_CHOICE', label: '多项选择题', count: 5, scoreEach: 4 },
      { type: 'JUDGE', label: '判断题', count: 5, scoreEach: 2 },
      { type: 'SHORT_ANSWER', label: '综合解答题', count: 4, scoreEach: 10 }
    ] as ExamRule[]
  });

  const calculatedTotalScore = computed(() =>
    examForm.rules.reduce((acc, cur) => acc + cur.count * cur.scoreEach, 0)
  );

  const isScoreMatched = computed(() => calculatedTotalScore.value === examForm.totalScore);

  const displayCourses = computed(() => courses.value);

  const composeMode = ref<'quick' | 'full'>('quick');
  const quickCount = ref(10);
  const quickTotalScore = ref(100);
  const difficultyModel = ref<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('NORMAL');
  const composePreview = ref<SmartPaperComposeVO | null>(null);

  /** 真实题库各题型存量统计 */
  const bankTypeStats = computed(() => {
    const stats: Record<string, number> = {
      SINGLE_CHOICE: 0,
      MULTIPLE_CHOICE: 0,
      JUDGE: 0,
      COMPLETION: 0,
      SHORT_ANSWER: 0
    };
    for (const q of courseBankQuestions.value) {
      const t = q.type || 'SINGLE_CHOICE';
      stats[t] = (stats[t] || 0) + 1;
    }
    return stats;
  });

  const cognitiveLevelOptions = [
    { key: 'REMEMBER', label: '识记 (Remember)' },
    { key: 'UNDERSTAND', label: '理解 (Understand)' },
    { key: 'APPLY', label: '应用 (Apply)' },
    { key: 'ANALYZE', label: '分析 (Analyze)' },
    { key: 'EVALUATE', label: '评价 (Evaluate)' }
  ];

  const cognitiveLevels = ref<Record<string, number>>({
    REMEMBER: 15,
    UNDERSTAND: 25,
    APPLY: 30,
    ANALYZE: 20,
    EVALUATE: 10
  });

  const cognitiveLevelTotal = computed(() =>
    Object.values(cognitiveLevels.value).reduce((sum, val) => sum + val, 0)
  );

  function getDifficultyDistribution() {
    if (difficultyModel.value === 'FOUNDATION') {
      return { EASY: 0.5, MEDIUM: 0.4, HARD: 0.1 };
    }
    if (difficultyModel.value === 'ADVANCED') {
      return { EASY: 0.1, MEDIUM: 0.4, HARD: 0.5 };
    }
    return { EASY: 0.3, MEDIUM: 0.5, HARD: 0.2 };
  }

  function buildTypeRatios() {
    const totalCount = examForm.rules.reduce((sum, rule) => sum + rule.count, 0);
    if (totalCount <= 0) return {};
    const ratios: Record<string, number> = {};
    examForm.rules.forEach((rule) => {
      if (rule.count > 0) {
        ratios[rule.type] = rule.count / totalCount;
      }
    });
    return ratios;
  }

  function buildCognitiveLevelRatios() {
    const total = cognitiveLevelTotal.value || 1;
    const ratios: Record<string, number> = {};
    Object.entries(cognitiveLevels.value).forEach(([key, val]) => {
      if (val > 0) ratios[key] = val / total;
    });
    return ratios;
  }

  /**
   * 一键根据目标总分平衡题型分值配比
   */
  function autoBalanceRules() {
    const target = examForm.totalScore || 100;
    examForm.rules = [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 }, // 30
      { type: 'MULTIPLE_CHOICE', label: '多项选择题', count: 5, scoreEach: 4 },  // 20
      { type: 'JUDGE', label: '判断题', count: 5, scoreEach: 2 },            // 10
      { type: 'SHORT_ANSWER', label: '综合解答题', count: 4, scoreEach: 10 }   // 40
    ];
    if (target !== 100) {
      examForm.totalScore = 100;
    }
    ElMessage.success('已自动按经典 100 分题型矩阵均衡配比');
  }

  /**
   * 添加题型规则
   */
  function addRule(type: string, label: string) {
    if (examForm.rules.some((r) => r.type === type)) {
      ElMessage.info('该题型已在规划清单中');
      return;
    }
    examForm.rules.push({
      type: type as any,
      label,
      count: 2,
      scoreEach: 5
    });
  }

  /**
   * 移除题型规则
   */
  function removeRule(type: string) {
    if (examForm.rules.length <= 1) {
      ElMessage.warning('试卷至少需要保留一种题型');
      return;
    }
    examForm.rules = examForm.rules.filter((r) => r.type !== type);
  }

  /**
   * 中止当前正在运行的 AI 组卷或命题计算
   */
  async function abortGeneration() {
    try {
      await cancelSmartPaperCompose();
    } catch {
      // 容错
    } finally {
      composing.value = false;
      generating.value = false;
      ElMessage.info('已中止当前 AI 智能组卷推演计算');
    }
  }

  /**
   * 快速智能组卷执行
   */
  async function handleQuickCompose() {
    composing.value = true;
    try {
      const res = await composeSmartPaper({
        courseId: examForm.courseId,
        chapterIds: examForm.chapterIds,
        knowledgePointIds: examForm.knowledgePointIds,
        totalCount: quickCount.value,
        totalScore: quickTotalScore.value,
        difficultyModel: difficultyModel.value,
        difficultyDistribution: getDifficultyDistribution(),
        promptDirective: examForm.promptDirective,
        aiGenerateFillShortfall: examForm.aiGenerateFillShortfall,
        typeRatios: {
          SINGLE_CHOICE: 0.4,
          MULTIPLE_CHOICE: 0.2,
          JUDGE: 0.2,
          SHORT_ANSWER: 0.2
        }
      });

      const preview = res.data;
      if (!preview?.questions?.length) {
        ElMessage.warning('未能生成或调取到有效题目，请重试');
        return;
      }

      composePreview.value = preview;
      examQualityAssessment.value = preview.examQualityAssessment || '';
      aiGeneratedCount.value = preview.aiGeneratedCount || 0;
      bankExtractedCount.value = preview.bankExtractedCount || preview.questions.length;

      ElMessage.success(
        `AI 智能组卷计算完成！覆盖考点 ${preview.distinctKnowledgePointCount || 0} 个，覆盖率 ${((preview.coverageRate || 0.85) * 100).toFixed(1)}%`
      );

      handleProceedToPreview();
    } catch (err: any) {
      ElMessage.error(err?.message || '智能组卷运算失败，请重试');
    } finally {
      composing.value = false;
    }
  }

  /**
   * 完整编排模式生成
   */
  async function handleGenerateExam() {
    if (!isScoreMatched.value) {
      ElMessage.warning('请先调整各题型分值使与目标总分完全匹配');
      return;
    }
    generating.value = true;
    try {
      const totalCount = examForm.rules.reduce((sum, rule) => sum + rule.count, 0);
      const res = await composeSmartPaper({
        courseId: examForm.courseId,
        chapterIds: examForm.chapterIds,
        knowledgePointIds: examForm.knowledgePointIds,
        totalCount,
        totalScore: examForm.totalScore,
        difficultyModel: difficultyModel.value,
        difficultyDistribution: getDifficultyDistribution(),
        typeRatios: buildTypeRatios(),
        cognitiveLevels: buildCognitiveLevelRatios(),
        promptDirective: examForm.promptDirective,
        aiGenerateFillShortfall: examForm.aiGenerateFillShortfall
      });

      const preview = res.data;
      if (!preview?.questions?.length) {
        ElMessage.warning('未抽到合适题目，请检查题库与考点');
        return;
      }

      examQualityAssessment.value = preview.examQualityAssessment || '';
      aiGeneratedCount.value = preview.aiGeneratedCount || 0;
      bankExtractedCount.value = preview.bankExtractedCount || preview.questions.length;

      const selectedCourse = displayCourses.value.find((c) => c.id === examForm.courseId);
      const courseTitle = selectedCourse?.name || selectedCourse?.title || '目标专业课程';

      currentExam.value = {
        id: Date.now(),
        courseId: examForm.courseId,
        courseName: courseTitle,
        title: examForm.title || `${courseTitle} · 课程水平期末考核试卷`,
        semester: '2025-2026学年第二学期',
        totalScore: preview.totalScore || examForm.totalScore,
        durationMinutes: examForm.durationMinutes,
        passScore: Math.round((preview.totalScore || examForm.totalScore) * 0.6),
        rules: [...examForm.rules],
        questions: preview.questions.map((q: any) =>
          normalizeQuestion({
            ...q,
            courseId: examForm.courseId,
            score: q.score ?? Math.round(examForm.totalScore / totalCount)
          })
        ),
        createdAt: new Date().toISOString().split('T')[0]
      };

      ElMessage.success(
        `完整试卷编排完成！考点覆盖率 ${((preview.coverageRate || 0.85) * 100).toFixed(1)}%`
      );
      router.push('/ai/exam/preview');
    } catch (err: any) {
      ElMessage.error(err?.message || '完整模式组卷运算失败');
    } finally {
      generating.value = false;
    }
  }

  function handleProceedToPreview() {
    if (!composePreview.value?.questions?.length) return;
    const selectedCourse = displayCourses.value.find((c) => c.id === examForm.courseId);
    const courseTitle = selectedCourse?.name || selectedCourse?.title || '目标专业课程';

    currentExam.value = {
      id: Date.now(),
      courseId: examForm.courseId,
      courseName: courseTitle,
      title:
        examForm.title ||
        `${courseTitle} · 期末综合水平测试（${difficultyModel.value === 'FOUNDATION' ? '基础巩固' : difficultyModel.value === 'ADVANCED' ? '综合拔高' : '标准正态'}梯度）`,
      semester: '2025-2026学年第二学期',
      totalScore: composePreview.value.totalScore || quickTotalScore.value,
      durationMinutes: examForm.durationMinutes,
      passScore: Math.round((composePreview.value.totalScore || quickTotalScore.value) * 0.6),
      rules: [],
      questions: composePreview.value.questions.map((q: any) =>
        normalizeQuestion({
          ...q,
          courseId: examForm.courseId,
          score: q.score ?? 5
        })
      ),
      createdAt: new Date().toISOString().split('T')[0]
    };
    router.push('/ai/exam/preview');
  }

  /**
   * 真实换题
   */
  async function swapQuestion(questionId: number | string) {
    const questions = currentExam.value.questions;
    const index = questions.findIndex((q) => String(q.id) === String(questionId));
    if (index === -1) {
      ElMessage.warning('未找到待替换题目');
      return;
    }

    const oldQ = questions[index];
    const loadingMsg = ElMessage({
      message: 'AI 命题引擎正在检索题库或定向命题替换...',
      type: 'info',
      duration: 0
    });

    try {
      const diffVal = typeof oldQ.difficulty === 'string'
        ? (oldQ.difficulty === 'HARD' ? 3 : oldQ.difficulty === 'EASY' ? 1 : 2)
        : Number(oldQ.difficulty) || 2;
      const res = await swapPaperQuestion({
        courseId: currentExam.value.courseId,
        oldQuestionId: oldQ.id,
        type: oldQ.type,
        difficulty: diffVal,
        score: oldQ.score,
        knowledgePointId: oldQ.knowledgePointId,
        knowledgePointName: oldQ.knowledgePointNames?.[0] || '核心考点',
        excludeQuestionIds: questions.map((q) => q.id),
        promptDirective: examForm.promptDirective
      });

      loadingMsg.close();
      if (res.data) {
        const replacement = normalizeQuestion({
          ...res.data,
          courseId: currentExam.value.courseId,
          score: oldQ.score
        });
        questions[index] = replacement;
        ElMessage.success('已成功为您调换 1 道高质量同考点试题！');
      } else {
        ElMessage.warning('换题结果为空，请重试');
      }
    } catch (err: any) {
      loadingMsg.close();
      ElMessage.error(err?.message || '智能换题失败，请稍后重试');
    }
  }

  /**
   * 保存试卷归档入库
   */
  async function saveExam() {
    if (!currentExam.value?.questions?.length) {
      ElMessage.warning('试卷题目为空，无法保存');
      return;
    }

    const examPayload: any = {
      courseId: currentExam.value.courseId,
      title: currentExam.value.title,
      totalScore: currentExam.value.totalScore,
      passScore: currentExam.value.passScore || Math.round(currentExam.value.totalScore * 0.6),
      durationMinutes: currentExam.value.durationMinutes || 90,
      questions: currentExam.value.questions.map((q, idx) => ({
        questionId: typeof q.id === 'number' ? q.id : (1000 + idx),
        score: q.score || 5,
        sortOrder: idx + 1
      }))
    };

    try {
      await createExam(examPayload);
      ElMessage.success(`试卷【${currentExam.value.title}】已成功归档入库！`);
      router.push('/question/exams');
    } catch (err: any) {
      ElMessage.error(err?.message || '保存试卷失败，请检查网络或权限');
    }
  }

  /**
   * 加载课程、章节、知识点与真实题库题目
   */
  async function loadCourseOptions() {
    const list = await loadGenerationCourseOptions();
    courses.value = list;
    if (list.length && !courses.value.some((c) => c.id === examForm.courseId)) {
      examForm.courseId = courses.value[0].id;
    }
    if (examForm.courseId) {
      await onCourseChange(examForm.courseId);
    }
    return courses.value;
  }

  async function onCourseChange(courseId: number) {
    loadingChapters.value = true;
    loadingKps.value = true;
    loadingBank.value = true;
    try {
      const [chapRes, kpRes, qRes] = await Promise.allSettled([
        getChapters(courseId),
        getKnowledgePoints(courseId),
        getQuestions({ courseId, pageSize: 100 })
      ]);
      if (chapRes.status === 'fulfilled' && chapRes.value?.data) {
        courseChapters.value = chapRes.value.data;
      } else {
        courseChapters.value = [];
      }
      if (kpRes.status === 'fulfilled' && kpRes.value?.data) {
        courseKnowledgePoints.value = kpRes.value.data;
      } else {
        courseKnowledgePoints.value = [];
      }
      if (qRes.status === 'fulfilled' && (qRes.value?.data as any)?.list) {
        courseBankQuestions.value = (qRes.value.data as any).list;
      } else {
        courseBankQuestions.value = [];
      }
    } finally {
      loadingChapters.value = false;
      loadingKps.value = false;
      loadingBank.value = false;
    }
  }

  return {
    examForm,
    currentExam,
    generating,
    composing,
    courses,
    displayCourses,
    courseChapters,
    courseKnowledgePoints,
    courseBankQuestions,
    bankTypeStats,
    loadingChapters,
    loadingKps,
    loadingBank,
    calculatedTotalScore,
    isScoreMatched,
    composeMode,
    quickCount,
    quickTotalScore,
    difficultyModel,
    composePreview,
    examQualityAssessment,
    aiGeneratedCount,
    bankExtractedCount,
    cognitiveLevelOptions,
    cognitiveLevels,
    cognitiveLevelTotal,
    loadCourseOptions,
    onCourseChange,
    autoBalanceRules,
    addRule,
    removeRule,
    handleGenerateExam,
    handleQuickCompose,
    handleProceedToPreview,
    abortGeneration,
    swapQuestion,
    saveExam
  };
}
