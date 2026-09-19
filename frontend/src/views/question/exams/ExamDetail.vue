<template>
  <div class="exam-detail-container question-module-page">
    <ExamDetailHeroSection
      :router="router"
      :loading="loading"
      :exam-data="examData"
      :get-status-label="getStatusLabel"
      :get-status-tag-type="getStatusTagType"
      @publish="handlePublishAsAssignment"
      @export="handleExportPaper"
      @delete="handleDeleteExam"
    />

    <div class="main-content-layout">
      <ExamDetailPaperDisplay
        v-model:view-mode="viewMode"
        :exam-data="examData"
        :grouped-sections="groupedSections"
        :total-questions-count="totalQuestionsCount"
        :get-chinese-number="getChineseNumber"
        @print="printPaper"
      />
      <ExamDetailStatsSidebar
        :exam-data="examData"
        :grouped-sections="groupedSections"
        :total-questions-count="totalQuestionsCount"
        :difficulty-counts="difficultyCounts"
        :difficulty-percentages="difficultyPercentages"
        :covered-knowledge-points="coveredKnowledgePoints"
      />
    </div>

  </div>
</template>

<script setup lang="ts">
import ExamDetailHeroSection from '@/components/question/exam/ExamDetailHeroSection.vue';
import ExamDetailPaperDisplay from '@/components/question/exam/ExamDetailPaperDisplay.vue';
import ExamDetailStatsSidebar from '@/components/question/exam/ExamDetailStatsSidebar.vue';
import { useExamDetail } from '@/composables/question/useExam';

const {
  router,
  loading,
  examData,
  viewMode,
  groupedSections,
  totalQuestionsCount,
  difficultyCounts,
  difficultyPercentages,
  coveredKnowledgePoints,
  handlePublishAsAssignment,
  handleExportPaper,
  printPaper,
  getChineseNumber,
  getStatusLabel,
  getStatusTagType,
  handleDeleteExam
} = useExamDetail();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.exam-detail-container {
  .main-content-layout {
    display: flex;
    gap: 24px;
    align-items: flex-start;
  }

}
</style>

<style lang="scss">
@media print {
  body * {
    visibility: hidden;
  }
  #printable-exam-paper,
  #printable-exam-paper * {
    visibility: visible;
  }
  #printable-exam-paper {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    box-shadow: none !important;
    border: none !important;
  }
}
</style>
