<template>
  <div class="exam-list-page-container">
    <ExamListHeader :page-ready="pageReady" :total="total" />

    <ExamListStatsBar
      :page-ready="pageReady"
      :total="total"
      :exam-count="exams.length"
    />

    <ExamListFilterBar
      v-model:keyword="keyword"
      :course-options="courseOptions"
      :selected-course-id="selectedCourseId"
      @course-filter="handleCourseFilter"
      @search="handleSearch"
      @clear-keyword="clearKeyword"
    />

    <ExamListTable
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :exams="exams"
      :loading="loading"
      :total="total"
      @page-change="loadExams"
      @preview="handlePreview"
      @export-pdf="handleExportPdf"
      @publish="handlePublish"
    />
  </div>
</template>

<script setup lang="ts">
import ExamListHeader from '@/components/question/exam/ExamListHeader.vue';
import ExamListStatsBar from '@/components/question/exam/ExamListStatsBar.vue';
import ExamListFilterBar from '@/components/question/exam/ExamListFilterBar.vue';
import ExamListTable from '@/components/question/exam/ExamListTable.vue';
import { useExamListPage } from '@/composables/question/useExamListPage';

const {
  exams,
  loading,
  total,
  pageNum,
  pageSize,
  selectedCourseId,
  keyword,
  courseOptions,
  pageReady,
  loadExams,
  handleCourseFilter,
  handleSearch,
  clearKeyword,
  handlePreview,
  handleExportPdf,
  handlePublish
} = useExamListPage();
</script>

<style scoped lang="scss">
.exam-list-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}
</style>
