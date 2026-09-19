<template>
  <div class="bank-detail-container">
    <BankDetailHeader
      :router="router"
      :loading="loading"
      :bank-info="bankInfo"
      :question-count="bankQuestions.length"
      :total-score="totalScore"
      :on-open-add-drawer="openAddDrawer"
      :on-fast-compose-exam="handleFastComposeExam"
      :on-export-markdown="exportBankMarkdown"
      :on-ai-expand="handleAiExpand"
      :on-delete-bank="handleDeleteBank"
    />

    <BankQuestionTable
      :search-keyword="searchKeyword"
      :filter-type="filterType"
      :filter-difficulty="filterDifficulty"
      :selected-row-keys="selectedRowKeys"
      :expanded-analyses="expandedAnalyses"
      :drawer-visible="drawerVisible"
      :drawer-search="drawerSearch"
      :drawer-type="drawerType"
      :selected-candidate-ids="selectedCandidateIds"
      :adding-loading="addingLoading"
      :detail-modal-visible="detailModalVisible"
      :active-question="activeQuestion"
      :filtered-questions="filteredQuestions"
      :candidate-questions="candidateQuestions"
      :on-search-keyword-change="(v) => { searchKeyword = v }"
      :on-filter-type-change="(v) => { filterType = v }"
      :on-filter-difficulty-change="(v) => { filterDifficulty = v }"
      :on-toggle-select-row="toggleSelectRow"
      :on-toggle-expand-analysis="toggleExpandAnalysis"
      :on-view-detail-dialog="viewDetailDialog"
      :on-remove-question="handleRemoveQuestion"
      :on-batch-remove="handleBatchRemove"
      :on-open-add-drawer="openAddDrawer"
      :on-drawer-visible-change="(v) => { drawerVisible = v }"
      :on-drawer-search-change="(v) => { drawerSearch = v }"
      :on-drawer-type-change="(v) => { drawerType = v }"
      :on-toggle-candidate-select="toggleCandidateSelect"
      :on-confirm-add-questions="confirmAddQuestions"
      :on-detail-modal-visible-change="(v) => { detailModalVisible = v }"
      :get-type-label="getTypeLabel"
      :get-type-tag-type="getTypeTagType"
      :get-difficulty-label="getDifficultyLabel"
      :get-difficulty-tag-type="getDifficultyTagType"
    />
  </div>
</template>

<script setup lang="ts">
import BankDetailHeader from '@/components/question/bank/BankDetailHeader.vue';
import BankQuestionTable from '@/components/question/bank/BankQuestionTable.vue';
import { useBank } from '@/composables/question/useBank';

const {
  router,
  loading,
  bankInfo,
  bankQuestions,
  searchKeyword,
  filterType,
  filterDifficulty,
  selectedRowKeys,
  expandedAnalyses,
  drawerVisible,
  drawerSearch,
  drawerType,
  selectedCandidateIds,
  addingLoading,
  detailModalVisible,
  activeQuestion,
  totalScore,
  filteredQuestions,
  candidateQuestions,
  toggleSelectRow,
  toggleExpandAnalysis,
  viewDetailDialog,
  handleRemoveQuestion,
  handleBatchRemove,
  openAddDrawer,
  toggleCandidateSelect,
  confirmAddQuestions,
  handleFastComposeExam,
  handleDeleteBank,
  exportBankMarkdown,
  handleAiExpand,
  getTypeLabel,
  getTypeTagType,
  getDifficultyLabel,
  getDifficultyTagType
} = useBank();
</script>

<style scoped lang="scss">
.bank-detail-container {
  padding: 24px;
  background-color: #f8fafc;
}
</style>
