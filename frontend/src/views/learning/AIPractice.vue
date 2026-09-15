<template>
  <div class="ai-practice-page" v-loading="generating">
    <PageHeroBanner
      title="AI 专项靶向练习 · 智能自适应训练"
      subtitle="结合布鲁姆认知分层与学情薄弱点，动态生成定制习题集，实时 AI 判题与前驱考点归因"
      background-variant="ai"
    >
      <template #extra>
        <AIPracticeModeRow
          :practice-mode="practiceMode"
          :on-switch-mode="switchMode"
        />
      </template>
    </PageHeroBanner>

    <AIPracticePanel
      :teacher-course-id="teacherCourseId"
      :course-options="courseOptions"
      :selected-kp-id="selectedKpId"
      :cognitive-level="cognitiveLevel"
      :question-count="questionCount"
      :instant-feedback="instantFeedback"
      :generating="generating"
      :is-practicing="isPracticing"
      :is-finished="isFinished"
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
      :get-difficulty-tag="getDifficultyTag"
      :on-teacher-course-id-change="(id) => { teacherCourseId = id }"
      :on-selected-kp-id-change="(id) => { selectedKpId = id }"
      :on-cognitive-level-change="(v) => { cognitiveLevel = v }"
      :on-question-count-change="(v) => { questionCount = v }"
      :on-instant-feedback-change="(v) => { instantFeedback = v }"
      :on-start-practice="startPractice"
      :on-select-option="selectOption"
      :on-text-answer-change="(index, v) => { userAnswers[index] = v }"
      :on-submit-current-question="submitCurrentQuestion"
      :on-jump-to-question="jumpToQuestion"
      :on-finish-practice="finishPractice"
      :on-confirm-exit="handleConfirmExit"
      :on-reset-to-setup="resetToSetup"
      :on-go-to-wrong-questions="goToWrongQuestions"
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
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { useAIPractice } from '@/composables/learning/useAIPractice';

const router = useRouter();
const route = useRoute();
const { courseOptions, courseId: teacherCourseId } = useTeacherCourses(Number(route.query.courseId) || 102);

const {
  courseId,
  practiceMode,
  selectedKpId,
  cognitiveLevel,
  questionCount,
  instantFeedback,
  generating,
  isPracticing,
  isFinished,
  currentIndex,
  questions,
  userAnswers,
  answersState,
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
} = useAIPractice(Number(route.query.courseId) || 102);

practiceMode.value = String(route.query.mode || 'WEAK_POINT');

watch(teacherCourseId, (id) => {
  courseId.value = id;
});

const handleConfirmExit = () => {
  ElMessageBox.confirm('当前练习尚未完成，确认退出吗？作答进度将不会被记录。', '提示', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '继续作答'
  }).then(() => {
    confirmExit(() => undefined);
  });
};

const goToWrongQuestions = () => {
  router.push({
    path: '/learning/wrong-questions',
    query: { courseId: courseId.value }
  });
};

onMounted(() => {
  courseId.value = teacherCourseId.value;
});
</script>

<style scoped lang="scss">
.ai-practice-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;
}
</style>
