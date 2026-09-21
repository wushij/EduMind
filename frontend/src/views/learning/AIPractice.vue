<template>
  <div class="ai-practice-page" v-loading="generating && !isPracticing">
    <PageHeroBanner
      title="AI 专项靶向练习 · 智能自适应训练"
      subtitle="结合布鲁姆认知分层与学情薄弱点，动态生成定制习题集，实时 AI 判题与前驱考点归因"
      background-variant="ai"
    >
      <template #extra>
        <AIPracticeModeRow :practice-mode="practiceMode" :on-switch-mode="switchMode" />
        <div v-if="!isPracticing && !isFinished" class="hero-stat-row">
          <div class="hero-stat-pill">
            <span class="label">薄弱考点</span>
            <strong>{{ sessionMeta.weakKnowledgePointCount }}</strong>
          </div>
          <div class="hero-stat-pill">
            <span class="label">待练错题</span>
            <strong>{{ sessionMeta.pendingWrongQuestionCount }}</strong>
          </div>
          <div class="hero-stat-pill">
            <span class="label">预计时长</span>
            <strong>{{ sessionMeta.estimatedMinutes }} 分钟</strong>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <AIPracticePanel
      :teacher-course-id="teacherCourseId"
      :course-options="courseOptions"
      :kp-options="kpOptions"
      :selected-kp-id="selectedKpId"
      :cognitive-level="cognitiveLevel"
      :question-count="questionCount"
      :instant-feedback="instantFeedback"
      :generating="generating"
      :grading="grading"
      :submitting="submitting"
      :snapshot-loading="snapshotLoading"
      :load-error="loadError"
      :is-practicing="isPracticing"
      :is-finished="isFinished"
      :session-meta="sessionMeta"
      :current-index="currentIndex"
      :questions="questions"
      :user-answers="userAnswers"
      :answers-state="answersState"
      :used-seconds="usedSeconds"
      :current-mode-name="currentModeName"
      :current-question="currentQuestion"
      :submitted-current="submittedCurrent"
      :correct-count="correctCount"
      :final-score="finalScore"
      :accuracy-rate="accuracyRate"
      :ai-summary="aiSummary"
      :submit-result="submitResult"
      :get-difficulty-tag="getDifficultyTag"
      :display-analysis="displayAnalysis"
      :display-reference-answer="displayReferenceAnswer"
      :on-teacher-course-id-change="handleCourseChange"
      :on-selected-kp-id-change="(id) => { selectedKpId = id }"
      :on-cognitive-level-change="(v) => { cognitiveLevel = v }"
      :on-question-count-change="(v) => { questionCount = v }"
      :on-instant-feedback-change="(v) => { instantFeedback = v }"
      :on-start-practice="startPractice"
      :on-select-option="selectOption"
      :on-text-answer-change="setTextAnswer"
      :on-submit-current-question="submitCurrentQuestion"
      :on-abort-grading="abortGrading"
      :on-jump-to-question="jumpToQuestion"
      :on-finish-practice="finishPractice"
      :on-confirm-exit="handleConfirmExit"
      :on-reset-to-setup="handleReset"
      :on-go-to-wrong-questions="goToWrongQuestions"
      :on-go-to-report="goToReport"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import AIPracticeModeRow from '@/components/learning/AIPracticeModeRow.vue';
import AIPracticePanel from '@/components/learning/AIPracticePanel.vue';
import { useLearningCourseOptions } from '@/composables/learning/useLearningCourseOptions';
import { useAIPractice } from '@/composables/learning/useAIPractice';

const router = useRouter();
const route = useRoute();
const initialCourseId = Number(route.query.courseId) || 102;

const { courseOptions, courseId: teacherCourseId } = useLearningCourseOptions(initialCourseId);

const {
  courseId,
  practiceMode,
  selectedKpId,
  cognitiveLevel,
  questionCount,
  instantFeedback,
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
  displayReferenceAnswer
} = useAIPractice(initialCourseId);

applyRouteQuery(route.query as Record<string, unknown>);

watch(teacherCourseId, (id) => {
  courseId.value = id;
  loadSetupSnapshot();
});

watch(
  () => route.query,
  (q) => {
    applyRouteQuery(q as Record<string, unknown>);
  }
);

function handleCourseChange(id: number) {
  teacherCourseId.value = id;
  courseId.value = id;
  loadSetupSnapshot();
}

const handleConfirmExit = () => {
  ElMessageBox.confirm('当前练习尚未完成，确认退出吗？作答进度将不会被记录。', '提示', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '继续作答'
  }).then(() => {
    confirmExit(() => undefined);
  });
};

const handleReset = () => {
  resetToSetup();
  loadSetupSnapshot();
};

const goToWrongQuestions = () => {
  router.push({
    path: '/learning/wrong-questions',
    query: { courseId: String(courseId.value) }
  });
};

const goToReport = () => {
  router.push({
    path: '/learning/report',
    query: { courseId: String(courseId.value) }
  });
};

onMounted(async () => {
  // 路由显式带入课程时以路由为准（错题本跳转会带 courseId），避免被课程选择器默认值覆盖
  const routeCourseId = Number(route.query.courseId);
  if (routeCourseId) {
    teacherCourseId.value = routeCourseId;
  }
  courseId.value = teacherCourseId.value;
  await loadSetupSnapshot();
  const mode = typeof route.query.mode === 'string' ? route.query.mode : '';
  // 错题变式 / 单题自测等带题入口需直接开练，不能停在配置页导致"题目没带上"
  const autoStart =
    route.query.autoStart === '1' ||
    mode === 'WRONG_BATCH' ||
    mode === 'VARIANT' ||
    mode === 'SINGLE_VARIANT';
  if (autoStart && !isPracticing.value) {
    await startPractice();
  }
});
</script>

<style scoped lang="scss">
.ai-practice-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;
}

.hero-stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;

  .hero-stat-pill {
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid rgba(22, 119, 255, 0.12);
    border-radius: 9999px;
    padding: 6px 16px;
    display: flex;
    align-items: center;
    gap: 8px;

    .label {
      font-size: 12px;
      color: #64748b;
    }

    strong {
      font-size: 14px;
      color: #1677ff;
    }
  }
}
</style>
