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
      <!-- 当前租户上下文提示：让管理者明确「看到/操作的数据属于哪个租户」 -->
      <div v-if="tenantStore.currentTenant" class="current-tenant-banner">
        <div class="banner-left">
          <el-icon class="banner-icon"><School /></el-icon>
          <span class="banner-label">当前租户上下文</span>
          <span class="banner-name">{{ tenantStore.currentTenant.name }}</span>
          <el-tag size="small" type="success" effect="light">{{ tenantStore.activeCampusName }}</el-tag>
        </div>
        <span class="banner-hint">列表中的「切入」操作将切换全局数据与权限范围</span>
      </div>

      <!-- 列表加载失败可重试，避免只有瞬时 toast 导致用户无从恢复 -->
      <el-alert
        v-if="listError"
        class="list-error-alert"
        type="error"
        :closable="false"
        show-icon
        :title="listError"
      >
        <template #default>
          <el-button link type="primary" @click="loadTenants">重新加载</el-button>
        </template>
      </el-alert>

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
import { School } from '@element-plus/icons-vue';
import { useTenant } from '@/composables/system/useTenant';

const {
  tenantStore,
  loading,
  listError,
  loadTenants,
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

/* 当前租户上下文提示条：黑金治理风格，与页面主色一致 */
.current-tenant-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 16px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(212, 168, 83, 0.12) 0%, rgba(212, 168, 83, 0.04) 100%);
  border: 1px solid #f0d78c;

  .banner-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .banner-icon {
    color: #b8860b;
    font-size: 16px;
  }

  .banner-label {
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.04em;
    color: #8a6d1f;
  }

  .banner-name {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .banner-hint {
    font-size: 12px;
    color: #8a6d1f;
  }
}

.list-error-alert {
  margin-bottom: 14px;
  border-radius: 12px;
}

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
