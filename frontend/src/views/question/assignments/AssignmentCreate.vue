<template>
  <div class="assignment-create-container question-module-page">
    <AssignmentCreateForm
      :set-form-ref="setFormRef"
      :form-data="formData"
      :rules="rules"
      :courses="courses"
      :exam-options="examOptions"
      v-model:source-mode="sourceMode"
      :selected-questions="selectedQuestions"
      :total-calculated-score="totalCalculatedScore"
      :submitting="submitting"
      :on-course-change="handleCourseChange"
      :on-exam-selected="handleExamSelected"
      :get-type-label="getTypeLabel"
      :get-type-tag-type="getTypeTagType"
      @cancel="handleCancel"
      @publish="handlePublishAssignment"
    />
  </div>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus';
import { useAssignmentCreate } from '@/composables/question/useAssignment';
import AssignmentCreateForm from '@/components/question/assignment/AssignmentCreateForm.vue';

const {
  formRef,
  submitting,
  courses,
  examOptions,
  sourceMode,
  formData,
  rules,
  selectedQuestions,
  totalCalculatedScore,
  handleCourseChange,
  handleExamSelected,
  handlePublishAssignment,
  handleCancel,
  getTypeLabel,
  getTypeTagType
} = useAssignmentCreate();

function setFormRef(el: FormInstance | undefined) {
  formRef.value = el;
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.assignment-create-container {
  width: 100%;
}
</style>
