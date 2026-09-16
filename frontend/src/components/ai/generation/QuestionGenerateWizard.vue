<template>
  <div class="ai-teaching-page-shell">
    <QuestionGenerateBanner
      :steps="steps"
      :current-step="currentStep"
      @back="$emit('back')"
      @go-to-step="$emit('go-to-step', $event)"
    />

    <div class="wizard-body-card ai-teaching-surface-card">
      <QuestionGenerateStepCourse
        v-show="currentStep === 1"
        :display-courses="displayCourses"
        :form-state="formState"
      />

      <QuestionGenerateStepScope
        v-show="currentStep === 2"
        :form-state="formState"
        :current-course-chapters="currentCourseChapters"
        :available-knowledge-points="availableKnowledgePoints"
        @toggle-chapter="$emit('toggle-chapter', $event)"
        @toggle-kp="$emit('toggle-kp', $event)"
      />

      <QuestionGenerateStepTypeDifficulty
        v-show="currentStep === 3"
        :form-state="formState"
        :type-options="typeOptions"
        :difficulty-options="difficultyOptions"
        @toggle-type="$emit('toggle-type', $event)"
      />

      <QuestionGenerateStepCount
        v-show="currentStep === 4"
        :form-state="formState"
      />

      <QuestionGenerateStepConfirm
        v-show="currentStep === 5"
        :form-state="formState"
        :selected-course-name="selectedCourseName"
        :difficulty-label="difficultyLabel"
        :generating="generating"
        @generate="$emit('generate')"
      />

      <QuestionGenerateFooterNav
        :current-step="currentStep"
        @prev="$emit('prev')"
        @next="$emit('next')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import QuestionGenerateBanner from './QuestionGenerateBanner.vue';
import QuestionGenerateStepCourse from './QuestionGenerateStepCourse.vue';
import QuestionGenerateStepScope from './QuestionGenerateStepScope.vue';
import QuestionGenerateStepTypeDifficulty from './QuestionGenerateStepTypeDifficulty.vue';
import QuestionGenerateStepCount from './QuestionGenerateStepCount.vue';
import QuestionGenerateStepConfirm from './QuestionGenerateStepConfirm.vue';
import QuestionGenerateFooterNav from './QuestionGenerateFooterNav.vue';
import type { DifficultyOption, QuestionGenerateFormState, QuestionTypeOption } from './question-generate-types';
import type { QuestionType } from '@/mock/questions';

defineProps<{
  steps: { index: number; name: string }[];
  currentStep: number;
  generating: boolean;
  displayCourses: any[];
  formState: QuestionGenerateFormState;
  currentCourseChapters: any[];
  availableKnowledgePoints: string[];
  typeOptions: QuestionTypeOption[];
  difficultyOptions: DifficultyOption[];
  selectedCourseName: string;
  difficultyLabel: string;
}>();

defineEmits<{
  back: [];
  'go-to-step': [index: number];
  'toggle-chapter': [id: number];
  'toggle-kp': [kp: string];
  'toggle-type': [type: QuestionType];
  prev: [];
  next: [];
  generate: [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.wizard-body-card {
  /* surface from ai-teaching-surface-card */
}
</style>
