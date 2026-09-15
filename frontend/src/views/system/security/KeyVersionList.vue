<template>
  <div class="key-version-page">
    <!-- 顶部高端 Hero Banner -->
    <PageHeroBanner
      title="国密 KMS 密钥管理 · 数据安全与合规中枢"
      subtitle="基于多租户沙箱隔离的国密数据加密密钥（Data Key）与模型凭证全生命周期管理，全面落实 SM4-GCM 认证加密与 Fail-Closed 隐私保护机制"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">国密三级</span>
            <span class="stat-label">商密合规标准</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">SM4-GCM</span>
            <span class="stat-label">128-bit AEAD 认证算法</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ distinctAliases.length || 2 }} 组</span>
            <span class="stat-label">托管密钥核心资产</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">Fail-Closed</span>
            <span class="stat-label">防篡改隐私熔断</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <KeyVersionTopBar
        :active-tenant-name="tenantStore.activeTenantName"
        :active-campus-name="tenantStore.activeCampusName"
        :active-data-key="activeDataKey"
        :active-model-key="activeModelKey"
        :refreshing="refreshing"
        :open-crypto-test-modal="openCryptoTestModal"
        :open-rotate-modal="openRotateModal"
        :handle-refresh-all="handleRefreshAll"
      />

      <KeyVersionActiveCards
        :active-data-key="activeDataKey"
        :active-model-key="activeModelKey"
        :open-crypto-test-modal="openCryptoTestModal"
        :open-rotate-modal="openRotateModal"
        :copy-text="copyText"
      />

      <KeyVersionTable
        v-model:alias-filter="aliasFilter"
        v-model:status-filter="statusFilter"
        v-model:search-keyword="searchKeyword"
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :loading="loading"
        :key-list="keyList"
        :paginated-key-list="paginatedKeyList"
        :filtered-key-list="filteredKeyList"
        :handle-filter-change="handleFilterChange"
        :handle-search="handleSearch"
        :handle-reset="handleReset"
        :open-crypto-test-modal="openCryptoTestModal"
        :open-detail-drawer="openDetailDrawer"
        :open-rotate-modal="openRotateModal"
        :copy-text="copyText"
      />
    </div>

    <KeyCryptoTestDialog
      v-model:visible="cryptoTestModalVisible"
      :test-form="testForm"
      :test-result="testResult"
      :testing-crypto="testingCrypto"
      :load-sample-text="loadSampleText"
      :execute-crypto-test="executeCryptoTest"
      :flip-to-decrypt="flipToDecrypt"
      :copy-text="copyText"
    />

    <KeyRotationPanel
      v-model:visible="rotateDialogVisible"
      v-model:selected-rotate-alias="selectedRotateAlias"
      :rotating="rotating"
      :get-active-ver-by-alias="getActiveVerByAlias"
      :execute-rotate="executeRotate"
    />

    <KeyVersionDetailDrawer
      v-model:visible="detailDrawerVisible"
      :selected-key-detail="selectedKeyDetail"
      :copy-text="copyText"
    />
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import KeyVersionTopBar from '@/components/system/security/KeyVersionTopBar.vue';
import KeyVersionActiveCards from '@/components/system/security/KeyVersionActiveCards.vue';
import KeyVersionTable from '@/components/system/security/KeyVersionTable.vue';
import KeyCryptoTestDialog from '@/components/system/security/KeyCryptoTestDialog.vue';
import KeyRotationPanel from '@/components/system/security/KeyRotationPanel.vue';
import KeyVersionDetailDrawer from '@/components/system/security/KeyVersionDetailDrawer.vue';
import { useSecurityKey } from '@/composables/system/useSecurityKey';

const {
  tenantStore,
  loading,
  refreshing,
  rotating,
  testingCrypto,
  aliasFilter,
  statusFilter,
  searchKeyword,
  pageNum,
  pageSize,
  keyList,
  activeDataKey,
  activeModelKey,
  distinctAliases,
  filteredKeyList,
  paginatedKeyList,
  handleRefreshAll,
  handleSearch,
  handleFilterChange,
  handleReset,
  cryptoTestModalVisible,
  testForm,
  testResult,
  openCryptoTestModal,
  loadSampleText,
  executeCryptoTest,
  flipToDecrypt,
  rotateDialogVisible,
  selectedRotateAlias,
  openRotateModal,
  executeRotate,
  detailDrawerVisible,
  selectedKeyDetail,
  openDetailDrawer,
  copyText,
  getActiveVerByAlias
} = useSecurityKey();
</script>
<style lang="scss">
@use '@/components/system/security/security-key-layout' as *;
</style>
