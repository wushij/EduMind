<template>
  <div class="main-practice-container">
    <AIPracticeSetupCard
      v-if="!isPracticing && !isFinished"
      :teacher-course-id="teacherCourseId"
      :course-options="courseOptions"
      :kp-options="kpOptions"
      :selected-kp-id="selectedKpId"
      :cognitive-level="cognitiveLevel"
      :question-count="questionCount"
      :instant-feedback="instantFeedback"
      :generating="generating"
      :snapshot-loading="snapshotLoading"
      :load-error="loadError"
      :current-mode-name="currentModeName"
      :session-meta="sessionMeta"
      :on-teacher-course-id-change="onTeacherCourseIdChange"
      :on-selected-kp-id-change="onSelectedKpIdChange"
      :on-cognitive-level-change="onCognitiveLevelChange"
      :on-question-count-change="onQuestionCountChange"
      :on-instant-feedback-change="onInstantFeedbackChange"
      :on-start-practice="onStartPractice"
    />

    <div v-else-if="isPracticing" class="practice-active-layout">
      <AIPracticeQuizHud
        :current-index="currentIndex"
        :questions="questions"
        :user-answers="userAnswers"
        :answers-state="answersState"
        :used-seconds="usedSeconds"
        :current-question="currentQuestion"
        :get-difficulty-tag="getDifficultyTag"
        :on-jump-to-question="onJumpToQuestion"
        :on-confirm-exit="onConfirmExit"
      />
      <AIPracticeQuizBody
        :current-index="currentIndex"
        :questions="questions"
        :current-question="currentQuestion"
        :user-answers="userAnswers"
        :answers-state="answersState"
        :submitted-current="submittedCurrent"
        :instant-feedback="instantFeedback"
        :grading="grading"
        :submitting="submitting"
        :show-feedback="instantFeedback"
        :display-analysis="displayAnalysis"
        :display-reference-answer="displayReferenceAnswer"
        :on-select-option="onSelectOption"
        :on-text-answer-change="onTextAnswerChange"
        :on-submit-current-question="onSubmitCurrentQuestion"
        :on-jump-to-question="onJumpToQuestion"
        :on-finish-practice="onFinishPractice"
      />
    </div>

    <div v-else-if="isFinished" class="result-layout">
      <AIPracticeResultCard
        :questions="questions"
        :correct-count="correctCount"
        :final-score="finalScore"
        :accuracy-rate="accuracyRate"
        :used-seconds="usedSeconds"
        :ai-summary="aiSummary"
        :wrong-question-ids="submitResult?.wrongQuestionIds"
        :on-reset-to-setup="onResetToSetup"
        :on-go-to-wrong-questions="onGoToWrongQuestions"
        :on-go-to-report="onGoToReport"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import AIPracticeSetupCard from '@/components/learning/AIPracticeSetupCard.vue';
import AIPracticeQuizHud from '@/components/learning/AIPracticeQuizHud.vue';
import AIPracticeQuizBody from '@/components/learning/AIPracticeQuizBody.vue';
import AIPracticeResultCard from '@/components/learning/AIPracticeResultCard.vue';
import type { PracticeQuestion } from '@/composables/learning/useAIPractice';
import type { AiPracticeSubmitVO } from '@/types/learning/ai-practice';

defineProps<{
  teacherCourseId: number;
  courseOptions: Array<{ id: number; name: string }>;
  kpOptions: Array<{ id: number; name: string; mastery?: number }>;
  selectedKpId: number;
  cognitiveLevel: string;
  questionCount: number;
  instantFeedback: boolean;
  generating: boolean;
  grading: boolean;
  submitting: boolean;
  snapshotLoading: boolean;
  loadError: string | null;
  isPracticing: boolean;
  isFinished: boolean;
  sessionMeta: {
    weakPointHint: string;
    estimatedMinutes: number;
    weakKnowledgePointCount: number;
    pendingWrongQuestionCount: number;
  };
  currentIndex: number;
  questions: PracticeQuestion[];
  userAnswers: Record<number, string>;
  answersState: Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>;
  usedSeconds: number;
  currentModeName: string;
  currentQuestion: PracticeQuestion;
  submittedCurrent: boolean;
  correctCount: number;
  finalScore: number;
  accuracyRate: number;
  aiSummary: string;
  submitResult: AiPracticeSubmitVO | null;
  getDifficultyTag: (difficulty: string) => string;
  displayAnalysis: (index: number) => string;
  displayReferenceAnswer: (index: number) => string;
  onTeacherCourseIdChange: (id: number) => void;
  onSelectedKpIdChange: (id: number) => void;
  onCognitiveLevelChange: (level: string) => void;
  onQuestionCountChange: (count: number) => void;
  onInstantFeedbackChange: (value: boolean) => void;
  onStartPractice: () => void;
  onSelectOption: (key: string) => void;
  onTextAnswerChange: (index: number, value: string) => void;
  onSubmitCurrentQuestion: () => void;
  onJumpToQuestion: (idx: number) => void;
  onFinishPractice: () => void;
  onConfirmExit: () => void;
  onResetToSetup: () => void;
  onGoToWrongQuestions: () => void;
  onGoToReport: () => void;
}>();
</script>

<style scoped lang="scss">
.main-practice-container {
  .practice-active-layout {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
