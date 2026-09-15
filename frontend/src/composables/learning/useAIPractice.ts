import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getQuestionRecommendations } from '@/api/learning/recommendation';
import type { RecommendationQuestion } from '@/types/learning/recommendation';

export interface PracticeQuestion {
  id: number;
  stem: string;
  type: string;
  typeText: string;
  difficulty: string;
  options?: Array<{ key: string; val: string }>;
  answer: string;
  analysis: string;
  kpTitle: string;
}

const DIFFICULTY_LABEL: Record<number, string> = {
  1: 'EASY',
  2: 'MEDIUM',
  3: 'HARD'
};

const TYPE_LABEL: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  JUDGE: '判断题',
  CALCULATION: '计算题',
  ESSAY: '解答题'
};

function mapRecommendationToPractice(q: RecommendationQuestion, index: number): PracticeQuestion {
  const diffNum = Number(q.difficulty);
  const diffLabel = DIFFICULTY_LABEL[diffNum] || String(q.difficulty || 'MEDIUM');
  return {
    id: q.questionId || index + 1,
    stem: q.stem || '暂无题干',
    type: q.type || 'SINGLE_CHOICE',
    typeText: TYPE_LABEL[q.type] || '练习题',
    difficulty: diffLabel,
    answer: 'A',
    analysis: q.reason || '请结合课程讲义与课堂笔记完成本题，关注核心概念与解题步骤。',
    kpTitle: q.knowledgePointName || '综合考点'
  };
}

export function useAIPractice(initialCourseId = 102) {
  const courseId = ref(initialCourseId);
  const practiceMode = ref('WEAK_POINT');
  const selectedKpId = ref(0);
  const cognitiveLevel = ref('ALL');
  const questionCount = ref(5);
  const instantFeedback = ref(true);

  const generating = ref(false);
  const isPracticing = ref(false);
  const isFinished = ref(false);
  const loadError = ref<string | null>(null);

  const currentIndex = ref(0);
  const questions = ref<PracticeQuestion[]>([]);
  const userAnswers = ref<Record<number, string>>({});
  const answersState = ref<Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>>({});
  const submittedTracker = ref<Record<number, boolean>>({});

  const startTime = ref(0);
  const usedSeconds = ref(0);

  const currentModeName = computed(() => {
    if (practiceMode.value === 'WEAK_POINT') return '错题变式攻坚';
    if (practiceMode.value === 'KNOWLEDGE_TIER') return '考点认知进阶';
    return '全真自适应冲刺';
  });

  const currentQuestion = computed(
    () =>
      questions.value[currentIndex.value] || {
        id: 0,
        stem: '',
        type: 'SINGLE_CHOICE',
        typeText: '单选题',
        difficulty: 'MEDIUM',
        options: [],
        answer: '',
        analysis: '',
        kpTitle: ''
      }
  );

  const submittedCurrent = computed(() => !!submittedTracker.value[currentIndex.value]);

  const correctCount = computed(
    () => Object.values(answersState.value).filter((s) => s === 'CORRECT').length
  );

  const finalScore = computed(() => {
    if (!questions.value.length) return 0;
    return Math.round((correctCount.value / questions.value.length) * 100);
  });

  const accuracyRate = computed(() => finalScore.value);

  function switchMode(mode: string) {
    practiceMode.value = mode;
  }

  function getDifficultyTag(diff: string) {
    if (diff === 'EASY') return 'success';
    if (diff === 'HARD') return 'danger';
    return 'warning';
  }

  async function startPractice() {
    generating.value = true;
    loadError.value = null;
    try {
      const res = await getQuestionRecommendations({
        courseId: courseId.value,
        limit: questionCount.value
      });
      const list = res?.data ?? [];
      if (!list.length) {
        loadError.value = '暂无推荐练习题，请先完成作业或测验';
        ElMessage.warning(loadError.value);
        return;
      }
      questions.value = list.map(mapRecommendationToPractice);
      userAnswers.value = {};
      answersState.value = {};
      submittedTracker.value = {};
      currentIndex.value = 0;
      startTime.value = Date.now();
      isPracticing.value = true;
      isFinished.value = false;
    } catch (err: unknown) {
      loadError.value = err instanceof Error ? err.message : '生成练习集失败';
      ElMessage.error(loadError.value);
    } finally {
      generating.value = false;
    }
  }

  function selectOption(optKey: string) {
    userAnswers.value[currentIndex.value] = optKey;
  }

  function submitCurrentQuestion() {
    const current = currentQuestion.value;
    const ans = userAnswers.value[currentIndex.value];
    if (!ans) return;
    submittedTracker.value[currentIndex.value] = true;
    answersState.value[currentIndex.value] = ans === current.answer ? 'CORRECT' : 'WRONG';
  }

  function jumpToQuestion(idx: number) {
    if (idx >= 0 && idx < questions.value.length) {
      currentIndex.value = idx;
    }
  }

  function finishPractice() {
    usedSeconds.value = Math.max(1, Math.round((Date.now() - startTime.value) / 1000));
    isPracticing.value = false;
    isFinished.value = true;
    ElMessage.success('自适应练习已完成，报告已生成！');
  }

  function resetToSetup() {
    isPracticing.value = false;
    isFinished.value = false;
    questions.value = [];
  }

  function confirmExit(onConfirm: () => void) {
    onConfirm();
    isPracticing.value = false;
    isFinished.value = false;
  }

  return {
    courseId,
    practiceMode,
    selectedKpId,
    cognitiveLevel,
    questionCount,
    instantFeedback,
    generating,
    isPracticing,
    isFinished,
    loadError,
    currentIndex,
    questions,
    userAnswers,
    answersState,
    submittedTracker,
    usedSeconds,
    currentModeName,
    currentQuestion,
    submittedCurrent,
    correctCount,
    finalScore,
    accuracyRate,
    switchMode,
    getDifficultyTag,
    startPractice,
    selectOption,
    submitCurrentQuestion,
    jumpToQuestion,
    finishPractice,
    resetToSetup,
    confirmExit
  };
}
