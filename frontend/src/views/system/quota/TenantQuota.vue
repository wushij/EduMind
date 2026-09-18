<template>
  <div class="tenant-quota-page" v-loading="loading">
    <PageHeroBanner
      title="租户资源配额与用量大盘 · 智能云容量管控"
      subtitle="实时监控各校区与院系的 AI 算力 Token 消耗、向量检索库存储、QPS 并发吞吐与部门配额预算切分"
      background-variant="system"
    >
      <template #extra>
        <QuotaHeroStats :token-percentage="tokenPercentage" :quotas="quotas" />
      </template>
    </PageHeroBanner>
    <div class="main-content-layout">
      <QuotaTopBar
        :tenant-name="tenantStore.activeTenantName"
        :campus-name="tenantStore.activeCampusName"
        :refreshing="refreshing"
        @refresh="handleRefreshAll"
        @open-config="openConfigModal"
      />
      <QuotaTelemetryGrid
        :quotas="quotas"
        :token-percentage="tokenPercentage"
        :is-token-warning="isTokenWarning"
        :storage-percentage="storagePercentage"
      />
      <QuotaManagementPanel
        :active-tab="activeTab"
        :dept-quota-list-length="deptQuotaList.length"
        :total="total"
        @update:active-tab="activeTab = $event"
      >
        <QuotaAllocationTab
          :active="activeTab === 'allocation'"
          :quotas="quotas"
          :total-allocated-tokens="totalAllocatedTokens"
          :total-allocated-used="totalAllocatedUsed"
          :warning-dept-count="warningDeptCount"
          :paginated-dept-quota-list="paginatedDeptQuotaList"
          :filtered-dept-quota-list="filteredDeptQuotaList"
          v-model:dept-search-keyword="deptSearchKeyword"
          v-model:dept-status-filter="deptStatusFilter"
          v-model:dept-page-num="deptPageNum"
          v-model:dept-page-size="deptPageSize"
          @adjust-dept="openAdjustDeptQuota"
        />
        <QuotaLedgerTab
          :active="activeTab === 'ledger'"
          :quotas="quotas"
          :token-percentage="tokenPercentage"
          :audit-logs="auditLogs"
          :table-loading="tableLoading"
          :total="total"
          :model-options="modelOptions"
          :page-total-tokens="pageTotalTokens"
          :page-estimated-cost="pageEstimatedCost"
          v-model:date-range="dateRange"
          v-model:scene-filter="sceneFilter"
          v-model:model-filter="modelFilter"
          v-model:search-keyword="searchKeyword"
          v-model:page-num="pageNum"
          v-model:page-size="pageSize"
          @filter-change="handleFilterChange"
          @search="handleSearch"
          @reset="handleReset"
          @view-detail="viewDetail"
          @load-audit-logs="loadAuditLogs"
        />
      </QuotaManagementPanel>
    </div>
    <QuotaConfigModal v-model:visible="configDialogVisible" :edit-form="editForm" :saving="savingConfig" @save="saveConfig" />
    <QuotaDeptAdjustModal v-model:visible="adjustDeptModalVisible" :current-editing-dept="currentEditingDept" :saving="savingDeptQuota" @save="saveDeptQuotaAdjustment" />
    <QuotaAuditDetailDrawer v-model:visible="detailDrawerVisible" :selected-log="selectedLog" @copy-text="copyText" />
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import QuotaHeroStats from '@/components/system/quota/QuotaHeroStats.vue';
import QuotaTopBar from '@/components/system/quota/QuotaTopBar.vue';
import QuotaTelemetryGrid from '@/components/system/quota/QuotaTelemetryGrid.vue';
import QuotaManagementPanel from '@/components/system/quota/QuotaManagementPanel.vue';
import QuotaAllocationTab from '@/components/system/quota/QuotaAllocationTab.vue';
import QuotaLedgerTab from '@/components/system/quota/QuotaLedgerTab.vue';
import QuotaConfigModal from '@/components/system/quota/QuotaConfigModal.vue';
import QuotaDeptAdjustModal from '@/components/system/quota/QuotaDeptAdjustModal.vue';
import QuotaAuditDetailDrawer from '@/components/system/quota/QuotaAuditDetailDrawer.vue';
import { useTenantQuota } from '@/composables/system/useTenantQuota';

const {
  tenantStore, loading, refreshing, savingConfig, configDialogVisible, activeTab, quotas, editForm,
  tokenPercentage, isTokenWarning, storagePercentage, deptQuotaList, deptSearchKeyword, deptStatusFilter,
  deptPageNum, deptPageSize, savingDeptQuota, totalAllocatedTokens, totalAllocatedUsed, warningDeptCount,
  filteredDeptQuotaList, paginatedDeptQuotaList, adjustDeptModalVisible, currentEditingDept,
  openAdjustDeptQuota, saveDeptQuotaAdjustment, auditLogs, tableLoading, pageNum, pageSize, total,
  dateRange, sceneFilter, modelFilter, searchKeyword, modelOptions, detailDrawerVisible, selectedLog,
  pageTotalTokens, pageEstimatedCost, copyText, loadAuditLogs, handleRefreshAll, handleSearch,
  handleFilterChange, handleReset, viewDetail, openConfigModal, saveConfig
} = useTenantQuota();
</script>

<style lang="scss">
@use '@/components/system/quota/tenant-quota-layout';
</style>
