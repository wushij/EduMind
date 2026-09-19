<template>
  <div class="wrong-book-page" v-loading="loading || coursesLoading">
    <el-empty
      v-if="!coursesLoading && !hasEnrolledCourses"
      description="你还没有加入任何课程，先去课程中心选课吧"
    >
      <el-button type="primary" @click="handleGoToCourseCenter">前往课程中心</el-button>
    </el-empty>

    <template v-else>
      <WrongBookHero
        :course-options="courseOptions"
        :selected-course-id="teacherCourseId"
        :overview="overview"
        @course-change="onHeroCourseChange"
        @batch-practice="handleLaunchBatchPractice"
      />

      <div class="main-content-layout">
        <WrongBookFilterBar
          :selected-error-type="selectedErrorType"
          @update:selected-error-type="onErrorTypeChange"
          @batch-practice="handleLaunchBatchPractice"
        />

        <div v-if="wrongList.length > 0" class="questions-grid">
          <WrongQuestionCard
            v-for="item in wrongList"
            :key="item.id"
            :item="item"
            :format-question-type="formatQuestionType"
            :get-difficulty-type="getDifficultyType"
            :parsed-options="parsedOptions"
            :display-error-tags="displayErrorTags"
            @open-diagnosis="openDiagnosisDrawer"
            @mark-mastered="handleMarkMastered"
            @start-variant="handleStartVariantPractice"
          />
        </div>

        <div v-else-if="!loading" class="empty-container">
          <el-empty
            :description="emptyDescription"
            :image-size="160"
          >
            <el-button type="primary" @click="handleGoToPractice">去自适应题库巩固</el-button>
          </el-empty>
        </div>

        <div v-if="totalWrongQuestions > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="totalWrongQuestions"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="fetchList"
            @size-change="fetchList"
          />
        </div>
      </div>

      <WrongBookDiagnosisDrawer
        v-model:visible="drawerVisible"
        :loading="detailLoading"
        :item="activeItem"
        :prerequisite-nodes="detailExtra.prerequisiteNodes ?? []"
        :variant-questions="detailExtra.variantQuestions ?? []"
        :display-error-tags="displayErrorTags"
        @practice-variant="handlePracticeSingleVariant"
        @start-practice="handleLaunchPracticeFromDrawer"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import WrongBookHero from '@/components/learning/wrong-book/WrongBookHero.vue';
import WrongBookFilterBar from '@/components/learning/wrong-book/WrongBookFilterBar.vue';
import WrongQuestionCard from '@/components/learning/wrong-book/WrongQuestionCard.vue';
import WrongBookDiagnosisDrawer from '@/components/learning/wrong-book/WrongBookDiagnosisDrawer.vue';
import { useWrongQuestionsPage } from '@/composables/learning/useWrongQuestionsPage';

const {
  courseOptions,
  teacherCourseId,
  hasEnrolledCourses,
  coursesLoading,
  loading,
  page,
  pageSize,
  totalWrongQuestions,
  selectedErrorType,
  wrongList,
  overview,
  drawerVisible,
  activeItem,
  detailLoading,
  detailExtra,
  fetchList,
  formatQuestionType,
  getDifficultyType,
  parsedOptions,
  displayErrorTags,
  handleCourseChange,
  openDiagnosisDrawer,
  handleMarkMastered,
  handleStartVariantPractice,
  handleLaunchBatchPractice,
  handlePracticeSingleVariant,
  handleLaunchPracticeFromDrawer,
  handleGoToPractice,
  handleGoToCourseCenter
} = useWrongQuestionsPage();

const emptyDescription = computed(() =>
  selectedErrorType.value
    ? '当前错因筛选下暂无记录，试试切换「全部错误类型」'
    : '太棒了！当前所选维度暂无错题记录，保持精熟状态！'
);

function onHeroCourseChange(courseId: number) {
  teacherCourseId.value = courseId;
  handleCourseChange();
}

function onErrorTypeChange(code: string) {
  selectedErrorType.value = code;
  page.value = 1;
  void fetchList();
}
</script>

<style scoped lang="scss">
.wrong-book-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;
}

.main-content-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.questions-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-container {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  border: 1px solid #e2e8f0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
