<template>
  <div class="course-list-page">
    <CourseListHero
      :total="allCoursesTotal"
      :active-course-count="activeCourseCount"
      :archived-course-count="archivedCourseCount"
      @join-course="handleJoinCourse"
    />

    <CourseListFilterBar
      v-model:search-keyword="searchKeyword"
      v-model:selected-semester="selectedSemester"
      :status-tabs="statusTabs"
      :current-status-tab="currentStatusTab"
      @status-tab-change="handleStatusTabChange"
      @filter-change="handleFilterChange"
      @clear-keyword="clearKeyword"
    />

    <div class="course-list-panel system-table-card">
      <CourseListGrid
        :loading="loading"
        :courses="courses"
        @reset-filters="resetFilters"
      />

      <AppPagination
        v-model:page-num="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadData"
      />
    </div>

    <CourseJoinDialog
      v-model:visible="showJoinDialog"
      v-model:join-tab="joinTab"
      v-model:course-code-input="courseCodeInput"
      :joining="joining"
      :public-courses-list="publicCoursesList"
      @join-by-code="handleJoinByCode"
      @quick-join="handleQuickJoin"
    />
  </div>
</template>

<script setup lang="ts">
import AppPagination from '@/components/common/AppPagination.vue';
import CourseListHero from '@/components/course/CourseListHero.vue';
import CourseListFilterBar from '@/components/course/CourseListFilterBar.vue';
import CourseListGrid from '@/components/course/CourseListGrid.vue';
import CourseJoinDialog from '@/components/course/CourseJoinDialog.vue';
import { useCourseList } from '@/composables/course/useCourseList';

const {
  courses,
  loading,
  total,
  allCoursesTotal,
  searchKeyword,
  currentStatusTab,
  selectedSemester,
  currentPage,
  pageSize,
  showJoinDialog,
  joinTab,
  courseCodeInput,
  joining,
  activeCourseCount,
  archivedCourseCount,
  statusTabs,
  publicCoursesList,
  loadData,
  handleStatusTabChange,
  handleFilterChange,
  clearKeyword,
  resetFilters,
  handleJoinCourse,
  handleQuickJoin,
  handleJoinByCode
} = useCourseList();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.course-list-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  min-height: 0;
  padding-bottom: 8px;
  box-sizing: border-box;

  > * {
    flex: 0 0 auto;
  }
}

.course-list-panel {
  padding: 20px 24px 12px;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;

  :deep(.pagination-bar) {
    margin-top: 16px;
    padding-top: 12px;
  }
}
</style>
