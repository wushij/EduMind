<template>
  <div class="audit-log-page">
    <AuditMetricsGrid :summary="summary" />

    <AuditTrendChart
      :trend-days="trendDays"
      :trend-loading="trendLoading"
      :bind-chart-el="bindTrendChartEl"
      @change-trend-days="changeTrendDays"
    />

    <div class="log-section-card">
      <AuditLogFilterBar
        v-model:scene-filter="sceneFilter"
        v-model:model-filter="modelFilter"
        v-model:status-filter="statusFilter"
        v-model:search-keyword="searchKeyword"
        :total="total"
        :model-options="modelOptions"
        :table-loading="tableLoading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <AuditLogTable
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        v-model:detail-drawer-visible="detailDrawerVisible"
        :logs="logs"
        :table-loading="tableLoading"
        :total="total"
        :selected-log="selectedLog"
        @load-logs="loadLogs"
        @view-detail="viewDetail"
        @copy-text="copyText"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import AuditMetricsGrid from '@/components/system/audit/AuditMetricsGrid.vue';
import AuditTrendChart from '@/components/system/audit/AuditTrendChart.vue';
import AuditLogFilterBar from '@/components/system/audit/AuditLogFilterBar.vue';
import AuditLogTable from '@/components/system/audit/AuditLogTable.vue';
import { useAudit } from '@/composables/system/useAudit';

const {
  summary,
  trendDays,
  trendLoading,
  trendChartRef,
  logs,
  tableLoading,
  pageNum,
  pageSize,
  total,
  sceneFilter,
  modelFilter,
  statusFilter,
  searchKeyword,
  modelOptions,
  detailDrawerVisible,
  selectedLog,
  changeTrendDays,
  loadLogs,
  handleSearch,
  handleReset,
  viewDetail,
  copyText
} = useAudit();

function bindTrendChartEl(el: HTMLElement | null) {
  trendChartRef.value = el;
}
</script>

<style scoped lang="scss">
.audit-log-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 4px;
}

.log-section-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
  overflow: hidden;
}
</style>
