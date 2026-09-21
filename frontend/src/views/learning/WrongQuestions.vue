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
          :show-expand-actions="wrongList.length > 0"
          :body-expanded="listExpandAllBody"
          :full-expanded="listExpandAllFull"
          @update:selected-error-type="onErrorTypeChange"
          @toggle-body="toggleAllBody"
          @toggle-details="toggleAllDetails"
          @batch-practice="handleLaunchBatchPractice"
        />

        <div v-if="wrongList.length > 0" class="questions-grid">
          <WrongQuestionCard
            v-for="(item, index) in wrongList"
            :key="item.id"
            :item="item"
            :index="(page - 1) * pageSize + index"
            :format-question-type="formatQuestionType"
            :get-difficulty-type="getDifficultyType"
            :parsed-options="parsedOptions"
            :display-diagnosis="displayDiagnosis"
            :display-error-tags="displayErrorTags"
            :default-body-expanded="false"
            :default-analysis-expanded="false"
            :bulk-expand-body="listExpandAllBody"
            :bulk-expand-analysis="listExpandAllAnalysis"
            :expand-sync-key="expandSyncKey"
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

        <!-- 统一分页：与答卷列表等模块共用 AppPagination（默认 10 条/页） -->
        <div v-if="totalWrongQuestions > 0" class="pagination-footer-bar">
          <AppPagination
            v-model:page-num="page"
            v-model:page-size="pageSize"
            :total="totalWrongQuestions"
            :page-sizes="[10, 20, 50]"
            @change="onPageChange"
          />
        </div>
      </div>

      <WrongBookDiagnosisDrawer
        v-model:visible="drawerVisible"
        :loading="detailLoading"
        :diagnosing="diagnosing"
        :variants-loading="variantsLoading"
        :item="activeItem"
        :prerequisite-nodes="detailExtra.prerequisiteNodes ?? []"
        :variant-questions="detailExtra.variantQuestions ?? []"
        :display-diagnosis="displayDiagnosis"
        :display-error-tags="displayErrorTags"
        @regenerate-diagnosis="handleRegenerateDiagnosis"
        @abort-diagnosis="handleAbortDiagnosis"
        @generate-variants="handleGenerateVariants"
        @abort-variants="handleAbortVariants"
        @practice-variant="handlePracticeSingleVariant"
        @start-practice="handleLaunchPracticeFromDrawer"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import WrongBookHero from '@/components/learning/wrong-book/WrongBookHero.vue';
import WrongBookFilterBar from '@/components/learning/wrong-book/WrongBookFilterBar.vue';
import WrongQuestionCard from '@/components/learning/wrong-book/WrongQuestionCard.vue';
import WrongBookDiagnosisDrawer from '@/components/learning/wrong-book/WrongBookDiagnosisDrawer.vue';
import AppPagination from '@/components/common/AppPagination.vue';
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
  diagnosing,
  variantsLoading,
  detailExtra,
  fetchList,
  formatQuestionType,
  getDifficultyType,
  parsedOptions,
  displayDiagnosis,
  displayErrorTags,
  handleCourseChange,
  openDiagnosisDrawer,
  handleRegenerateDiagnosis,
  handleAbortDiagnosis,
  handleGenerateVariants,
  handleAbortVariants,
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

/** 列表级批量展开状态：与题库列表 QuestionCard 的 bulkExpand + syncKey 机制保持一致 */
const listExpandAllBody = ref(false);
const listExpandAllAnalysis = ref(false);
const expandSyncKey = ref(0);

const listExpandAllFull = computed(
  () => listExpandAllBody.value && listExpandAllAnalysis.value
);

function toggleAllBody() {
  listExpandAllBody.value = !listExpandAllBody.value;
  expandSyncKey.value += 1;
}

function toggleAllDetails() {
  const next = !listExpandAllFull.value;
  listExpandAllBody.value = next;
  listExpandAllAnalysis.value = next;
  expandSyncKey.value += 1;
}

function onPageChange() {
  void fetchList();
}

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
  gap: 18px;
}

.empty-container {
  background: #fff;
  border-radius: 22px;
  padding: 40px;
  text-align: center;
  border: 1px solid #e8eef7;
  box-shadow: 0 6px 20px rgba(30, 80, 150, 0.04);
}

.pagination-footer-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 22px;
  box-shadow: 0 6px 20px rgba(30, 80, 150, 0.04);

  :deep(.pagination-bar) {
    margin-top: 0;
    padding: 0;
    border-top: none;
    justify-content: flex-start !important;
    width: 100%;
  }
}
</style>
