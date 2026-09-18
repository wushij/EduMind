<template>
  <div class="tenant-management-page system-page-shell" v-loading="loading">
    <PageHeroBanner
      title="多租户与校区中心 · 集团化多级隔离治理"
      subtitle="统一管控多校区/分校、独立组织机构、企业级数据多租户隔离与独立资源配额"
      background-variant="system"
    >
      <template #extra>
        <TenantListHero :overview-stats="overviewStats" />
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <TenantFilterBar
        v-model:search-keyword="searchKeyword"
        :status-filter="statusFilter"
        v-model:plan-filter="planFilter"
        :status-tabs="statusTabs"
        @status-tab-change="handleStatusTabChange"
        @search="handleSearch"
        @create="openCreateDialog"
      />

      <TenantGrid
        :filtered-tenants="filteredTenants"
        :current-tenant-id="tenantStore.currentTenant?.id"
        :resolve-plan-name="resolvePlanName"
        :is-expiring-soon="isExpiringSoon"
        @copy-domain="copyDomain"
        @switch-tenant="openSwitchDialog"
        @open-campus="openCampusDrawer"
        @open-quota="openQuotaDrawer"
        @jump-org-tree="jumpToOrgTree"
        @more-command="handleMoreCommand"
      />
    </div>

    <TenantEditDialog
      ref="editDialogRef"
      v-model="editDialogVisible"
      :tenant="activeEditingTenant"
      @saved="handleSaved"
    />

    <CampusManagementDrawer
      ref="campusDrawerRef"
      v-model="campusDrawerVisible"
      :tenant="activeDrawerTenant"
      @changed="handleCampusChanged"
    />

    <TenantQuotaDrawer
      ref="quotaDrawerRef"
      v-model="quotaDrawerVisible"
      :tenant="activeDrawerTenant"
      @changed="handleQuotaChanged"
    />

    <TenantSwitchDialog
      v-model="switchDialogVisible"
      :tenant="activeSwitchTenant"
      @switched="handleSwitched"
    />
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import TenantListHero from '@/components/system/tenant/TenantListHero.vue';
import TenantFilterBar from '@/components/system/tenant/TenantFilterBar.vue';
import TenantGrid from '@/components/system/tenant/TenantGrid.vue';
import CampusManagementDrawer from '@/components/system/tenant/CampusManagementDrawer.vue';
import TenantQuotaDrawer from '@/components/system/tenant/TenantQuotaDrawer.vue';
import TenantEditDialog from '@/components/system/tenant/TenantEditDialog.vue';
import TenantSwitchDialog from '@/components/system/tenant/TenantSwitchDialog.vue';
import { useTenant } from '@/composables/system/useTenant';

const {
  tenantStore,
  loading,
  searchKeyword,
  statusFilter,
  planFilter,
  statusTabs,
  overviewStats,
  campusDrawerVisible,
  quotaDrawerVisible,
  editDialogVisible,
  switchDialogVisible,
  activeDrawerTenant,
  activeEditingTenant,
  activeSwitchTenant,
  campusDrawerRef,
  quotaDrawerRef,
  editDialogRef,
  filteredTenants,
  handleSearch,
  handleStatusTabChange,
  openCreateDialog,
  openCampusDrawer,
  openQuotaDrawer,
  jumpToOrgTree,
  openSwitchDialog,
  handleSwitched,
  copyDomain,
  resolvePlanName,
  isExpiringSoon,
  handleMoreCommand,
  handleSaved,
  handleCampusChanged,
  handleQuotaChanged
} = useTenant();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.tenant-management-page {
  padding-bottom: 40px;

  .main-content-layout {
    width: 100%;
    max-width: none;
    margin: 0;
    padding: 0;
  }
}
</style>
