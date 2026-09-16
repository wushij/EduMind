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
.course-list-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding-bottom: 40px;
}
</style>
