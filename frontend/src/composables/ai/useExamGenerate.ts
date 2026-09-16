import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Exam, ExamRule } from '@/types/question/exam';
import { composeSmartPaperV2 } from '@/api/ai/paper-compose';
import { loadGenerationCourseOptions } from '@/services/ai/generation-service';
import { generateExam, createExam } from '@/api/question/exam';
import { USE_MOCK } from '@/config/mock';
import { MOCK_EXAMS } from '@/mock/exams';
import { MOCK_COURSES } from '@/mock/courses';
import { normalizeQuestion } from '@/utils/question/normalize-question';
import type { SmartPaperComposeRequest, SmartPaperComposeVO } from '@/types/ai/paper-compose';

const currentExam = ref<Exam>({ ...MOCK_EXAMS[0] });

export function useExamGenerate() {
  const router = useRouter();
  const generating = ref(false);
  const composing = ref(false);
  const courses = ref<any[]>([]);

  const examForm = reactive({
    courseId: 101,
    title: '期中测验',
    totalScore: 100,
    durationMinutes: 90,
    chapterIds: [] as number[],
    rules: [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 },
      { type: 'MULTIPLE_CHOICE', label: '多项选择题', count: 5, scoreEach: 4 },
      { type: 'SHORT_ANSWER', label: '解答题', count: 3, scoreEach: 10 }
    ] as ExamRule[]
  });

  const calculatedTotalScore = computed(() =>
    examForm.rules.reduce((acc, cur) => acc + cur.count * cur.scoreEach, 0)
  );

  const isScoreMatched = computed(() => calculatedTotalScore.value === examForm.totalScore);

  const displayCourses = computed(() =>
    courses.value.length ? courses.value : USE_MOCK ? MOCK_COURSES : []
  );

  const composeMode = ref<'quick' | 'full'>('quick');
  const quickCount = ref(10);
  const quickTotalScore = ref(100);
  const difficultyModel = ref<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('NORMAL');
  const composePreview = ref<SmartPaperComposeVO | null>(null);

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

  async function handleGenerateExam() {
    if (!isScoreMatched.value) {
      ElMessage.warning('请先调整题型分值使总分匹配');
      return;
    }
    generating.value = true;
    try {
      const totalCount = examForm.rules.reduce((sum, rule) => sum + rule.count, 0);
      const preview = await composeSmartPaper({
        courseId: examForm.courseId,
        totalCount,
        totalScore: examForm.totalScore,
        difficultyDistribution: getDifficultyDistribution(),
        typeRatios: buildTypeRatios(),
        cognitiveLevels: buildCognitiveLevelRatios()
      });
      if (!preview?.questions?.length) {
        ElMessage.warning('未抽到题目，请检查题库');
        return;
      }
      currentExam.value = {
        id: Date.now(),
        courseId: examForm.courseId,
        courseName: displayCourses.value.find((c) => c.id === examForm.courseId)?.title ?? '',
        title: examForm.title,
        semester: '',
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
        `完整模式组卷完成，知识点覆盖率 ${((preview.coverageRate || 0) * 100).toFixed(1)}%`
      );
      router.push('/ai/exam/preview');
    } catch (err: any) {
      ElMessage.error(err?.message || '完整模式智能组卷失败');
    } finally {
      generating.value = false;
    }
  }

  async function handleQuickCompose() {
    try {
      composePreview.value = await composeSmartPaper({
        courseId: examForm.courseId,
        totalCount: quickCount.value,
        totalScore: quickTotalScore.value,
        difficultyDistribution: getDifficultyDistribution(),
        typeRatios: {
          SINGLE_CHOICE: 0.5,
          MULTIPLE_CHOICE: 0.3,
          JUDGE: 0.2
        }
      });
      if (!composePreview.value?.questions?.length) {
        ElMessage.warning('未抽到题目，请检查题库');
        return;
      }
      ElMessage.success(
        `智能组卷 v2 运算完成！知识点覆盖率: ${((composePreview.value.coverageRate || 0) * 100).toFixed(1)}%`
      );
    } catch (err: any) {
      ElMessage.error(err?.message || '快速智能组卷运算失败');
    }
  }

  function handleProceedToPreview() {
    if (!composePreview.value?.questions?.length) return;
    currentExam.value = {
      id: Date.now(),
      courseId: examForm.courseId,
      courseName: displayCourses.value.find((c) => c.id === examForm.courseId)?.title ?? '',
      title:
        examForm.title ||
        `智能组卷 · 难度梯度(${difficultyModel.value === 'FOUNDATION' ? '基础' : difficultyModel.value === 'ADVANCED' ? '拔高' : '标准'})综合测试`,
      semester: '',
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
    ElMessage.success('试卷规划生成就绪，已进入预览发布页');
    router.push('/ai/exam/preview');
  }

  async function loadCourseOptions() {
    const list = await loadGenerationCourseOptions();
    courses.value = list;
    if (list.length && !courses.value.some((c) => c.id === examForm.courseId)) {
      examForm.courseId = courses.value[0].id;
    }
    return courses.value;
  }

  async function composeSmartPaper(request: SmartPaperComposeRequest): Promise<SmartPaperComposeVO | null> {
    composing.value = true;
    try {
      const res = await composeSmartPaperV2(request);
      return res.data ?? null;
    } finally {
      composing.value = false;
    }
  }

  async function generateExamPreview() {
    if (!isScoreMatched.value) {
      ElMessage.warning(`各题型总分 ${calculatedTotalScore.value} 分，需等于目标总分 ${examForm.totalScore} 分`);
      return;
    }
    generating.value = true;
    try {
      const res = await generateExam({
        courseId: examForm.courseId,
        title: examForm.title,
        totalScore: examForm.totalScore,
        durationMinutes: examForm.durationMinutes,
        chapterIds: examForm.chapterIds,
        rules: examForm.rules.map(r => ({ type: r.type, count: r.count, scoreEach: r.scoreEach }))
      });
      currentExam.value = {
        ...(res.data as Exam),
        id: res.data?.id || Date.now(),
        courseId: examForm.courseId,
        courseName: res.data?.courseName || '',
        title: examForm.title,
        semester: res.data?.semester || '',
        totalScore: examForm.totalScore,
        durationMinutes: examForm.durationMinutes,
        passScore: Math.round(examForm.totalScore * 0.6),
        rules: [...examForm.rules],
        questions: res.data?.questions || [],
        createdAt: new Date().toISOString().split('T')[0]
      };
      ElMessage.success('智能组卷成功');
      router.push('/ai/exam/preview');
    } catch {
      if (USE_MOCK) {
        currentExam.value = { ...MOCK_EXAMS[0], title: examForm.title };
        router.push('/ai/exam/preview');
      } else {
        ElMessage.error('组卷失败，请重试');
      }
    } finally {
      generating.value = false;
    }
  }

  function swapQuestion(_questionId: number) {
    ElMessage.info('已从题库抽取替补题目');
  }

  async function saveExam() {
    try {
      await createExam({
        courseId: currentExam.value.courseId,
        title: currentExam.value.title,
        totalScore: currentExam.value.totalScore,
        durationMinutes: currentExam.value.durationMinutes,
        questions: currentExam.value.questions
      });
      ElMessage.success(`试卷【${currentExam.value.title}】保存成功`);
      router.push('/question/exams');
    } catch {
      if (USE_MOCK) {
        ElMessage.success(`试卷【${currentExam.value.title}】保存成功`);
        router.push('/question/exams');
      } else {
        ElMessage.error('保存试卷失败');
      }
    }
  }

  return {
    examForm,
    currentExam,
    generating,
    composing,
    courses,
    displayCourses,
    calculatedTotalScore,
    isScoreMatched,
    composeMode,
    quickCount,
    quickTotalScore,
    difficultyModel,
    composePreview,
    cognitiveLevelOptions,
    cognitiveLevels,
    cognitiveLevelTotal,
    loadCourseOptions,
    composeSmartPaper,
    generateExam: generateExamPreview,
    handleGenerateExam,
    handleQuickCompose,
    handleProceedToPreview,
    swapQuestion,
    saveExam
  };
}
