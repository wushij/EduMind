<template>
  <div class="oper-log-container">
    <OperLogStatsBar :stats="stats" />

    <OperLogFilterBar
      v-model:date-range="dateRange"
      v-model:query-params="queryParams"
      :loading="loading"
      :date-range-shortcuts="dateRangeShortcuts"
      @query="handleQuery"
      @reset="resetQuery"
      @date-range-change="handleDateRangeChange"
    />

    <OperLogTable
      v-model:detail-visible="detailVisible"
      v-model:query-params="queryParams"
      :loading="loading"
      :total="total"
      :table-data="tableData"
      :selected-row-ids="selectedRowIds"
      :detail="detail"
      :parsed-action="parsedAction"
      :parsed-diff-items="parsedDiffItems"
      @selection-change="handleSelectionChange"
      @open-detail="openDetail"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @export-csv="exportCsv"
      @clean="handleClean"
      @load-data="loadData"
      @copy-text="copyText"
    />
  </div>
</template>

<script setup lang="ts">
import OperLogStatsBar from '@/components/system/oper-log/OperLogStatsBar.vue';
import OperLogFilterBar from '@/components/system/oper-log/OperLogFilterBar.vue';
import OperLogTable from '@/components/system/oper-log/OperLogTable.vue';
import { useOperLog } from '@/composables/system/useAudit';

const {
  loading,
  total,
  tableData,
  selectedRowIds,
  detailVisible,
  detail,
  dateRange,
  dateRangeShortcuts,
  stats,
  queryParams,
  parsedAction,
  parsedDiffItems,
  handleDateRangeChange,
  loadData,
  handleQuery,
  resetQuery,
  handleSelectionChange,
  openDetail,
  copyText,
  handleDelete,
  handleBatchDelete,
  handleClean,
  exportCsv
} = useOperLog();
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.oper-log-container {
  padding: 0 0 $page-bottom-spacing 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
