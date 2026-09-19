<template>
  <div class="bank-detail-container question-module-page">
    <BankDetailHeader
      :router="router"
      :loading="loading"
      :bank-info="bankInfo"
      :question-count="bankQuestions.length"
      :total-score="totalScore"
      :questions="bankQuestions"
      :on-open-add-drawer="openAddDrawer"
      :on-create-new-question="handleCreateNewQuestion"
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
      :drawer-visible="drawerVisible"
      :drawer-search="drawerSearch"
      :drawer-type="drawerType"
      :selected-candidate-ids="selectedCandidateIds"
      :adding-loading="addingLoading"
      :filtered-questions="filteredQuestions"
      :candidate-questions="candidateQuestions"
      :on-search-keyword-change="(v) => { searchKeyword = v }"
      :on-filter-type-change="(v) => { filterType = v }"
      :on-filter-difficulty-change="(v) => { filterDifficulty = v }"
      :on-toggle-select-row="toggleSelectRow"
      :on-remove-question="handleRemoveQuestion"
      :on-batch-remove="handleBatchRemove"
      :on-open-add-drawer="openAddDrawer"
      :on-drawer-visible-change="(v) => { drawerVisible = v }"
      :on-drawer-search-change="(v) => { drawerSearch = v }"
      :on-drawer-type-change="(v) => { drawerType = v }"
      :on-toggle-candidate-select="toggleCandidateSelect"
      :on-confirm-add-questions="confirmAddQuestions"
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
  drawerVisible,
  drawerSearch,
  drawerType,
  selectedCandidateIds,
  addingLoading,
  totalScore,
  filteredQuestions,
  candidateQuestions,
  toggleSelectRow,
  handleRemoveQuestion,
  handleBatchRemove,
  openAddDrawer,
  toggleCandidateSelect,
  confirmAddQuestions,
  handleFastComposeExam,
  handleCreateNewQuestion,
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
@use '@/styles/question/module-page-shell.scss';
</style>
