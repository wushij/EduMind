<template>
  <div class="exam-generate-page">
    <ExamGenerateWizard
      :banner-img="examBannerImg"
      v-model:compose-mode="composeMode"
      v-model:quick-count="quickCount"
      v-model:quick-total-score="quickTotalScore"
      v-model:difficulty-model="difficultyModel"
      :exam-form="examForm"
      :display-courses="displayCourses"
      :composing="composing"
      :compose-preview="composePreview"
      :cognitive-level-options="cognitiveLevelOptions"
      :cognitive-levels="cognitiveLevels"
      :cognitive-level-total="cognitiveLevelTotal"
      :calculated-total-score="calculatedTotalScore"
      :is-score-matched="isScoreMatched"
      :generating="generating"
      @back="router.push('/ai/marketplace')"
      @quick-compose="handleQuickCompose"
      @proceed-preview="handleProceedToPreview"
      @generate="handleGenerateExam"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useExamGenerate } from '@/composables/ai/useExamGenerate';
import ExamGenerateWizard from '@/components/ai/generation/ExamGenerateWizard.vue';
import examBannerImg from '@/assets/images/ai组卷.png';

const router = useRouter();

const {
  examForm,
  generating,
  composing,
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
  handleGenerateExam,
  handleQuickCompose,
  handleProceedToPreview
} = useExamGenerate();

onMounted(() => {
  loadCourseOptions();
});
</script>

<style scoped lang="scss">
.exam-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;
}
</style>
