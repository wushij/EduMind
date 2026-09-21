import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import {
  startAiPractice,
  gradeAiPracticeAnswer,
  submitAiPractice
} from '@/api/learning/ai-practice';
import { getKnowledgePoints } from '@/api/course/knowledge-point';
import { getStudentPortrait } from '@/api/analytics/learning';
import { getWrongBook } from '@/api/learning/wrong-book';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import { parseQuestionOptionsAsVal } from '@/utils/format/question-options';
import { isObjectiveQuestionType } from '@/utils/format/question-answer';
import type { PracticeQuestionVO } from '@/types/learning/ai-practice';
import type { AiPracticeSubmitVO, AiPracticeQuestionResult } from '@/types/learning/ai-practice';
import { usePracticeTimer } from '@/composables/learning/usePracticeTimer';

export interface PracticeQuestion {
  id: number;
  stem: string;
  type: string;
  typeText: string;
  difficulty: string;
  difficultyNum?: number;
  options?: Array<{ key: string; val: string }>;
  answer: string;
  analysis: string;
  kpTitle: string;
  knowledgePointId?: number;
}

const DIFFICULTY_LABEL: Record<number, string> = {
  1: 'EASY',
  2: 'MEDIUM',
  3: 'HARD'
};

const TYPE_LABEL: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  JUDGE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  CALCULATION: '计算题',
  ESSAY: '解答题'
};

function mapQuestionVo(q: PracticeQuestionVO): PracticeQuestion {
  const diffNum = Number(q.difficulty ?? 2);
  const diffLabel = DIFFICULTY_LABEL[diffNum] || 'MEDIUM';
  const options = parseQuestionOptionsAsVal(q.options);
  return {
    id: q.id,
    stem: q.stem || '暂无题干',
    type: q.type || 'SINGLE_CHOICE',
    typeText: TYPE_LABEL[q.type] || '练习题',
    difficulty: diffLabel,
    difficultyNum: diffNum,
    options: options.length ? options : undefined,
    answer: q.answer || '',
    analysis: q.analysis || '',
    kpTitle: q.knowledgePointName || '综合考点',
    knowledgePointId: q.knowledgePointId
  };
}

export function useAIPractice(initialCourseId = 102) {
  const courseId = ref(initialCourseId);
  const practiceMode = ref('WEAK_POINT');
  const selectedKpId = ref(0);
  const cognitiveLevel = ref('ALL');
  const questionCount = ref(5);
  const instantFeedback = ref(true);
  /** 种子题目 ID：雪花 ID 用字符串承载，避免 Number() 丢精度 */
  const seedQuestionIds = ref<Array<number | string>>([]);

  const sessionId = ref('');
  const generating = ref(false);
  const grading = ref(false);
  /** AI 批改在途请求的中止控制器（推演面板「中止」按钮用它真正取消本次等待） */
  let gradingAbortController: AbortController | null = null;
  const submitting = ref(false);
  const isPracticing = ref(false);
  const isFinished = ref(false);
  const loadError = ref<string | null>(null);

  const sessionMeta = ref({
    weakPointHint: '',
    estimatedMinutes: 12,
    weakKnowledgePointCount: 0,
    pendingWrongQuestionCount: 0
  });

  const kpOptions = ref<Array<{ id: number; name: string; mastery?: number }>>([]);
  const snapshotLoading = ref(false);

  const currentIndex = ref(0);
  const questions = ref<PracticeQuestion[]>([]);
  const userAnswers = ref<Record<number, string>>({});
  const answersState = ref<Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>>({});
  const submittedTracker = ref<Record<number, boolean>>({});
  const gradedAnalysis = ref<Record<number, { analysis: string; referenceAnswer: string }>>({});

  const submitResult = ref<AiPracticeSubmitVO | null>(null);

  const { usedSeconds, start: startTimer, stop: stopTimer } = usePracticeTimer();

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

  const correctCount = computed(() => {
    if (submitResult.value) {
      return submitResult.value.correctCount;
    }
    return Object.values(answersState.value).filter((s) => s === 'CORRECT').length;
  });

  const finalScore = computed(() => {
    if (submitResult.value) {
      return Math.round(submitResult.value.accuracyRate);
    }
    if (!questions.value.length) return 0;
    return Math.round((correctCount.value / questions.value.length) * 100);
  });

  const accuracyRate = computed(() => finalScore.value);

  const aiSummary = computed(() => submitResult.value?.aiSummary || '');

  function switchMode(mode: string) {
    practiceMode.value = mode;
  }

  function getDifficultyTag(diff: string) {
    if (diff === 'EASY') return 'success';
    if (diff === 'HARD') return 'danger';
    return 'warning';
  }

  function applyRouteQuery(query: Record<string, unknown>) {
    const mode = query.mode;
    if (typeof mode === 'string' && mode) {
      if (mode === 'VARIANT' || mode === 'WRONG_BATCH' || mode === 'SINGLE_VARIANT') {
        practiceMode.value = 'WEAK_POINT';
      } else {
        practiceMode.value = mode;
      }
    }
    // ⚠️ 题目 ID 是雪花 ID（19 位），超过 JS 安全整数范围：
    // Number('2102008935144140801') === 2102008935144140800，转换即丢精度，后端必然查不到题目。
    // 因此这里一律保留字符串原样传给后端（后端 Long 反序列化时可接受字符串）。
    const qid = query.questionId;
    if (typeof qid === 'string' && qid.trim()) {
      seedQuestionIds.value = [qid.trim()];
    }
    // 多题入口（错题本「练习同类变式题」会带上生成的变式题 ID 列表）
    const qids = query.questionIds;
    if (typeof qids === 'string' && qids.trim()) {
      const ids = qids
        .split(',')
        .map((v) => v.trim())
        .filter((v) => /^\d+$/.test(v));
      if (ids.length) {
        seedQuestionIds.value = ids;
      }
    }
    // 由外部指定题目构成练习集时，题量即这些题目本身，避免再掺入无关题目
    if (seedQuestionIds.value.length > 0) {
      questionCount.value = seedQuestionIds.value.length;
    }
    const cid = query.courseId;
    if (cid != null && cid !== '') {
      courseId.value = Number(cid);
    }
  }

  async function loadSetupSnapshot() {
    snapshotLoading.value = true;
    try {
      const [kpRes, portraitRes, wrongRes] = await Promise.all([
        getKnowledgePoints(courseId.value),
        getStudentPortrait({ courseId: courseId.value }).catch(() => null),
        getWrongBook({ courseId: courseId.value, page: 1, pageSize: 1 }).catch(() => null)
      ]);
      const list = kpRes.data ?? [];
      kpOptions.value = [
        { id: 0, name: '全课程薄弱考点自适应聚合 (AI 推荐)' },
        ...list.map((kp) => ({
          id: kp.id,
          name: kp.title || kp.name || `考点 #${kp.id}`,
          mastery: kp.masteryRate
        }))
      ];
      sessionMeta.value.pendingWrongQuestionCount = wrongRes?.data?.total ?? 0;
      const weakFromKp = list.filter((kp) => (kp.masteryRate ?? 100) < 70).length;
      const weakFromPortrait = portraitRes?.data?.weakPoints?.length ?? 0;
      sessionMeta.value.weakKnowledgePointCount = weakFromKp || weakFromPortrait;
    } catch {
      kpOptions.value = [{ id: 0, name: '全课程薄弱考点自适应聚合 (AI 推荐)' }];
    } finally {
      snapshotLoading.value = false;
    }
  }

  async function startPractice() {
    generating.value = true;
    loadError.value = null;
    try {
      const res = await startAiPractice({
        courseId: courseId.value,
        count: questionCount.value,
        mode: practiceMode.value,
        knowledgePointId: selectedKpId.value > 0 ? selectedKpId.value : undefined,
        cognitiveLevel: cognitiveLevel.value,
        instantFeedback: instantFeedback.value,
        seedQuestionIds: seedQuestionIds.value.length ? seedQuestionIds.value : undefined
      });
      const data = res?.data;
      if (!data?.questions?.length) {
        // 只在配置卡内展示，避免弹窗 + 卡片内提示重复报同一件事
        loadError.value = '暂无推荐练习题，请先完成作业或测验';
        return;
      }
      sessionId.value = data.sessionId;
      questions.value = data.questions.map(mapQuestionVo);
      sessionMeta.value = {
        weakPointHint: data.weakPointHint || '',
        estimatedMinutes: data.estimatedMinutes ?? 12,
        weakKnowledgePointCount: data.weakKnowledgePointCount ?? 0,
        pendingWrongQuestionCount: data.pendingWrongQuestionCount ?? 0
      };
      userAnswers.value = {};
      answersState.value = {};
      submittedTracker.value = {};
      gradedAnalysis.value = {};
      submitResult.value = null;
      currentIndex.value = 0;
      startTimer();
      isPracticing.value = true;
      isFinished.value = false;
    } catch (err: unknown) {
      // 优先展示后端业务提示（如「指定的练习题已不存在…」），只渲染在配置卡内，不再额外弹一条相同提示
      loadError.value = resolveApiErrorMessage(err, '生成练习集失败');
    } finally {
      generating.value = false;
    }
  }

  function selectOption(optKey: string) {
    userAnswers.value[currentIndex.value] = optKey;
  }

  function setTextAnswer(index: number, value: string) {
    userAnswers.value[index] = value;
  }

  async function submitCurrentQuestion() {
    const ans = userAnswers.value[currentIndex.value];
    if (ans === undefined || ans === '') return;
    if (!instantFeedback.value) {
      submittedTracker.value[currentIndex.value] = true;
      return;
    }
    if (!sessionId.value) return;
    abortGrading(false);
    const controller = new AbortController();
    gradingAbortController = controller;
    grading.value = true;
    try {
      const res = await gradeAiPracticeAnswer(
        {
          sessionId: sessionId.value,
          questionId: currentQuestion.value.id,
          studentAnswer: ans
        },
        { signal: controller.signal, silent: true }
      );
      if (controller.signal.aborted) return;
      const g = res?.data;
      submittedTracker.value[currentIndex.value] = true;
      answersState.value[currentIndex.value] = g?.correct ? 'CORRECT' : 'WRONG';
      if (g) {
        gradedAnalysis.value[currentIndex.value] = {
          analysis: g.analysis || currentQuestion.value.analysis,
          referenceAnswer: g.referenceAnswer || currentQuestion.value.answer
        };
      }
    } catch (err: unknown) {
      // 用户主动中止属于预期行为，不再弹出误导性提示
      if (controller.signal.aborted) return;
      ElMessage.error(resolveApiErrorMessage(err, '判题失败'));
    } finally {
      if (gradingAbortController === controller) {
        gradingAbortController = null;
        grading.value = false;
      }
    }
  }

  /** 中止在途的 AI 批改；notify=false 用于切换题目等静默场景 */
  function abortGrading(notify = true) {
    if (gradingAbortController) {
      gradingAbortController.abort();
      gradingAbortController = null;
    }
    if (grading.value) {
      grading.value = false;
      if (notify) {
        ElMessage.info('已中止本次 AI 批改');
      }
    }
  }

  function jumpToQuestion(idx: number) {
    if (idx >= 0 && idx < questions.value.length) {
      currentIndex.value = idx;
    }
  }

  async function finishPractice() {
    if (!sessionId.value) return;
    submitting.value = true;
    try {
      stopTimer();
      const answers = questions.value.map((q, idx) => ({
        questionId: q.id,
        knowledgePointId: q.knowledgePointId,
        answer: userAnswers.value[idx] ?? ''
      }));
      const res = await submitAiPractice({
        courseId: courseId.value,
        sessionId: sessionId.value,
        durationSeconds: usedSeconds.value,
        answers
      });
      submitResult.value = res?.data ?? null;
      if (submitResult.value?.questionResults) {
        applySubmitResults(submitResult.value.questionResults);
      }
      isPracticing.value = false;
      isFinished.value = true;
      ElMessage.success('自适应练习已完成，报告已生成');
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '提交练习失败');
    } finally {
      submitting.value = false;
    }
  }

  function applySubmitResults(results: AiPracticeQuestionResult[]) {
    results.forEach((r, idx) => {
      const index = questions.value.findIndex((q) => q.id === r.questionId);
      const i = index >= 0 ? index : idx;
      submittedTracker.value[i] = true;
      answersState.value[i] = r.correct ? 'CORRECT' : 'WRONG';
      gradedAnalysis.value[i] = {
        analysis: r.analysis || questions.value[i]?.analysis || '',
        referenceAnswer: r.referenceAnswer || questions.value[i]?.answer || ''
      };
    });
  }

  function resetToSetup() {
    stopTimer();
    isPracticing.value = false;
    isFinished.value = false;
    questions.value = [];
    sessionId.value = '';
    submitResult.value = null;
    seedQuestionIds.value = [];
  }

  function confirmExit(onConfirm: () => void) {
    onConfirm();
    stopTimer();
    isPracticing.value = false;
    isFinished.value = false;
    sessionId.value = '';
  }

  function displayAnalysis(index: number): string {
    return gradedAnalysis.value[index]?.analysis || questions.value[index]?.analysis || '';
  }

  function displayReferenceAnswer(index: number): string {
    return gradedAnalysis.value[index]?.referenceAnswer || questions.value[index]?.answer || '';
  }

  function isMultiChoice(type: string) {
    return type === 'MULTIPLE_CHOICE';
  }

  return {
    courseId,
    practiceMode,
    selectedKpId,
    cognitiveLevel,
    questionCount,
    instantFeedback,
    seedQuestionIds,
    sessionId,
    generating,
    grading,
    submitting,
    isPracticing,
    isFinished,
    loadError,
    sessionMeta,
    kpOptions,
    snapshotLoading,
    currentIndex,
    questions,
    userAnswers,
    answersState,
    submittedTracker,
    usedSeconds,
    submitResult,
    currentModeName,
    currentQuestion,
    submittedCurrent,
    correctCount,
    finalScore,
    accuracyRate,
    aiSummary,
    switchMode,
    getDifficultyTag,
    applyRouteQuery,
    loadSetupSnapshot,
    startPractice,
    selectOption,
    setTextAnswer,
    submitCurrentQuestion,
    abortGrading,
    jumpToQuestion,
    finishPractice,
    resetToSetup,
    confirmExit,
    displayAnalysis,
    displayReferenceAnswer,
    isMultiChoice,
    isObjectiveQuestionType
  };
}
