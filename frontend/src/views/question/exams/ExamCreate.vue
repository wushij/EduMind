<template>
  <div class="exam-create-container question-module-page">
    <ExamCreateNavBar
      :current-step="currentStep"
      @cancel="handleCancel"
    />

    <ExamCreateBasicForm
      v-show="currentStep === 0"
      :set-step1-form-ref="setStep1FormRef"
      :exam-form="examForm"
      :step1-rules="step1Rules"
      :courses="courses"
      :on-course-change="handleCourseChange"
      @next="goToStep2"
    />

    <ExamCreateSectionEditor
      v-show="currentStep === 1"
      v-model:current-step="currentStep"
      v-model:picker-visible="pickerVisible"
      v-model:picker-keyword="pickerKeyword"
      v-model:picker-difficulty="pickerDifficulty"
      :sections="sections"
      :exam-form="examForm"
      :current-total-score="currentTotalScore"
      :current-total-questions="currentTotalQuestions"
      :active-section="activeSection"
      :selected-picker-ids="selectedPickerIds"
      :filtered-candidate-questions="filteredCandidateQuestions"
      :handle-add-section="handleAddSection"
      :update-section-default-score="updateSectionDefaultScore"
      :calculate-scores="calculateScores"
      :remove-section="removeSection"
      :remove-question-from-section="removeQuestionFromSection"
      :open-question-picker="openQuestionPicker"
      :toggle-picker-item="togglePickerItem"
      :confirm-add-picked-questions="confirmAddPickedQuestions"
      :get-section-score="getSectionScore"
      :get-type-label="getTypeLabel"
      :get-type-tag-type="getTypeTagType"
      :get-difficulty-label="getDifficultyLabel"
      :get-difficulty-tag-type="getDifficultyTagType"
      @next="goToStep3"
    />

    <ExamCreateReviewPanel
      v-show="currentStep === 2"
      v-model:current-step="currentStep"
      v-model:publish-status="publishStatus"
      :exam-form="examForm"
      :sections="sections"
      :current-total-score="currentTotalScore"
      :saving="saving"
      :get-course-name="getCourseName"
      :get-section-score="getSectionScore"
      :get-chinese-number="getChineseNumber"
      @save="handleSaveExam"
    />
  </div>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus';
import { useExamCreate } from '@/composables/question/useExam';
import ExamCreateNavBar from '@/components/question/exam/ExamCreateNavBar.vue';
import ExamCreateBasicForm from '@/components/question/exam/ExamCreateBasicForm.vue';
import ExamCreateSectionEditor from '@/components/question/exam/ExamCreateSectionEditor.vue';
import ExamCreateReviewPanel from '@/components/question/exam/ExamCreateReviewPanel.vue';

const {
  currentStep,
  saving,
  courses,
  publishStatus,
  step1FormRef,
  examForm,
  step1Rules,
  sections,
  pickerVisible,
  activeSection,
  pickerKeyword,
  pickerDifficulty,
  selectedPickerIds,
  currentTotalScore,
  currentTotalQuestions,
  filteredCandidateQuestions,
  handleCourseChange,
  getCourseName,
  updateSectionDefaultScore,
  calculateScores,
  handleAddSection,
  removeSection,
  removeQuestionFromSection,
  openQuestionPicker,
  togglePickerItem,
  confirmAddPickedQuestions,
  goToStep2,
  goToStep3,
  handleSaveExam,
  handleCancel,
  getSectionScore,
  getChineseNumber,
  getTypeLabel,
  getTypeTagType,
  getDifficultyLabel,
  getDifficultyTagType
} = useExamCreate();

function setStep1FormRef(el: FormInstance | undefined) {
  step1FormRef.value = el;
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
