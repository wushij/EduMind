<template>
  <div class="assignment-list-page">
    <AssignmentListHeader :page-ready="pageReady" :total="total" />
    <AssignmentListStatsRow
      :page-ready="pageReady"
      :stats="stats"
      :avg-submission-rate-label="avgSubmissionRateLabel"
    />
    <AssignmentListFilterBar
      v-model:keyword="searchKeyword"
      v-model:selected-course-id="selectedCourseId"
      v-model:selected-status="selectedStatus"
      :courses="courses"
      @search="handleSearch"
    />
    <AssignmentListCards
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :assignments="assignments"
      :loading="loading"
      :total="total"
      :get-course-name="getCourseName"
      :progress-percent="progressPercent"
      :progress-label="progressLabel"
      @page-change="loadAssignments"
      @ai-grade="handleFastAIGrade"
      @delete="(id, title) => removeAssignment(id, title).then(() => loadAssignments())"
    />
  </div>
</template>

<script setup lang="ts">
import AssignmentListHeader from '@/components/question/assignment/AssignmentListHeader.vue';
import AssignmentListStatsRow from '@/components/question/assignment/AssignmentListStatsRow.vue';
import AssignmentListFilterBar from '@/components/question/assignment/AssignmentListFilterBar.vue';
import AssignmentListCards from '@/components/question/assignment/AssignmentListCards.vue';
import { useAssignmentListPage } from '@/composables/question/useAssignmentListPage';

const {
  assignments,
  loading,
  total,
  stats,
  pageReady,
  pageNum,
  pageSize,
  courses,
  searchKeyword,
  selectedCourseId,
  selectedStatus,
  avgSubmissionRateLabel,
  loadAssignments,
  handleSearch,
  getCourseName,
  progressPercent,
  progressLabel,
  handleFastAIGrade,
  removeAssignment
} = useAssignmentListPage();
</script>

<style scoped lang="scss">
.assignment-list-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  padding-bottom: 4px;
  box-sizing: border-box;
}
</style>
