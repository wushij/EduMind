<template>
  <div class="prompt-list-page">
    <PromptListHero
      :loading="loading"
      :prompt-list-length="promptList.length"
      :published-count="publishedCount"
      :rag-count="ragCount"
      :total-variables-count="totalVariablesCount"
      @create="goToEditor"
      @reload="handleReload"
    />

    <PromptListFilterBar
      :selected-category="selectedCategory"
      v-model:selected-status="selectedStatus"
      v-model:search-keyword="searchKeyword"
      :get-category-count="getCategoryCount"
      @change-category="changeCategory"
      @filter="filterPrompts"
    />

    <PromptListGrid
      :loading="loading"
      :filtered-prompts="filteredPrompts"
      :get-category-label="getCategoryLabel"
      :get-display-model-name="getDisplayModelName"
      :get-display-model-tooltip="getDisplayModelTooltip"
      :is-bound-model-unset="isBoundModelUnset"
      @copy-text="copyText"
      @open-drawer="openDrawer"
      @edit="goToEditorById"
      @delete="handleDeletePrompt"
      @reset-filters="resetFilters"
      @create="goToEditor"
    />

    <PromptListQuickDrawer
      v-model:drawer-visible="drawerVisible"
      v-model:drawer-active-tab="drawerActiveTab"
      :active-item="activeItem"
      :test-variables="testVariables"
      :test-result-output="testResultOutput"
      :test-duration="testDuration"
      :testing="testing"
      :get-category-label="getCategoryLabel"
      @update-test-variable="updateTestVariable"
      @copy-text="copyText"
      @populate-dummy="populateDummyVariables"
      @execute-test="executePlaygroundTest"
      @edit="goToEditorById"
    />
  </div>
</template>

<script setup lang="ts">
import { usePromptList } from '@/composables/system/usePromptList';
import PromptListHero from '@/components/system/prompt/PromptListHero.vue';
import PromptListFilterBar from '@/components/system/prompt/PromptListFilterBar.vue';
import PromptListGrid from '@/components/system/prompt/PromptListGrid.vue';
import PromptListQuickDrawer from '@/components/system/prompt/PromptListQuickDrawer.vue';

const {
  loading,
  testing,
  promptList,
  selectedCategory,
  selectedStatus,
  searchKeyword,
  publishedCount,
  ragCount,
  totalVariablesCount,
  filteredPrompts,
  drawerVisible,
  drawerActiveTab,
  activeItem,
  testVariables,
  testResultOutput,
  testDuration,
  getCategoryCount,
  getCategoryLabel,
  changeCategory,
  filterPrompts,
  resetFilters,
  handleReload,
  copyText,
  openDrawer,
  populateDummyVariables,
  executePlaygroundTest,
  getDisplayModelName,
  getDisplayModelTooltip,
  isBoundModelUnset,
  goToEditor,
  goToEditorById,
  updateTestVariable,
  handleDeletePrompt
} = usePromptList();
</script>

<style scoped lang="scss">
.prompt-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;
}

.tooltip-vars-list {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .tip-v {
    font-family: ui-monospace, monospace;
    font-size: 11px;
    color: #E2E8F0;
  }
}
</style>
