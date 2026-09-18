<template>
  <div class="broadcast-page system-page-shell">
    <div class="system-page-hero-card page-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <el-icon :size="24"><Promotion /></el-icon>
        </div>
        <div class="header-text">
          <div class="title-row">
            <h2>消息广播推送中心</h2>
            <el-tag size="small" effect="plain" round class="status-live-tag">
              <span class="live-dot" /> 实时全校触达
            </el-tag>
          </div>
          <p>支持面向全校师生或指定角色（教师/学生/管理员）定向下发系统广播、业务提醒与紧急公告</p>
        </div>
      </div>
      <div class="header-right">
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="danger"
          plain
          round
          :icon="Delete"
          class="header-btn-clear"
          @click="handleClear"
        >
          清空历史
        </el-button>
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="primary"
          round
          :icon="Promotion"
          class="header-btn-create"
          @click="drawerVisible = true"
        >
          发起广播
        </el-button>
      </div>
    </div>

    <BroadcastStatsRow :stats="stats" />

    <BroadcastFilterBar
      v-model:search-keyword="searchKeyword"
      v-model:target-type-filter="targetTypeFilter"
      v-model:priority-filter="priorityFilter"
      :loading="loading"
      :total-count="filteredTableData.length"
      @search="handleSearch"
      @reset="handleReset"
    />

    <BroadcastTable
      :loading="loading"
      :paginated-data="paginatedData"
      v-model:page-num="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="filteredTableData.length"
      :can-delete="authStore.hasPermission('notice:broadcast:send')"
      :priority-label="priorityLabel"
      :priority-tag-type="priorityTagType"
      :audience-label="audienceLabel"
      :audience-tag-type="audienceTagType"
      :audience-icon="audienceIcon"
      :calc-percent="calcPercent"
      :get-reach-progress-color="getReachProgressColor"
      :resolve-sender-avatar="resolveSenderAvatar"
      @open-detail="openDetail"
      @open-recipients="openRecipients"
      @delete="handleDelete"
      @size-change="handleSizeChange"
      @page-change="handlePageChange"
    />

    <BroadcastCreateDrawer v-model:visible="drawerVisible" @sent="onSent" />

    <BroadcastDetailDialog
      v-model:visible="detailVisible"
      :broadcast="currentBroadcast"
      @open-recipients="openRecipients(currentBroadcast)"
    />

    <BroadcastRecipientsDialog
      v-model:visible="recipientsVisible"
      :broadcast="currentBroadcast"
    />
  </div>
</template>

<script setup lang="ts">
import { Promotion, Delete } from '@element-plus/icons-vue';
import BroadcastCreateDrawer from '@/components/notification/BroadcastCreateDrawer.vue';
import BroadcastDetailDialog from '@/components/notification/BroadcastDetailDialog.vue';
import BroadcastRecipientsDialog from '@/components/notification/BroadcastRecipientsDialog.vue';
import BroadcastStatsRow from '@/components/system/broadcast/BroadcastStatsRow.vue';
import BroadcastFilterBar from '@/components/system/broadcast/BroadcastFilterBar.vue';
import BroadcastTable from '@/components/system/broadcast/BroadcastTable.vue';
import { useBroadcast } from '@/composables/system/useBroadcast';

const {
  authStore,
  loading,
  searchKeyword,
  targetTypeFilter,
  priorityFilter,
  drawerVisible,
  detailVisible,
  recipientsVisible,
  currentBroadcast,
  query,
  stats,
  filteredTableData,
  paginatedData,
  handleSearch,
  handleReset,
  handleSizeChange,
  handlePageChange,
  onSent,
  openDetail,
  openRecipients,
  priorityLabel,
  priorityTagType,
  audienceLabel,
  audienceTagType,
  audienceIcon,
  calcPercent,
  getReachProgressColor,
  resolveSenderAvatar,
  handleDelete,
  handleClear
} = useBroadcast();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.broadcast-page {
  /* layout from system-page-shell */
}

.page-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px -4px rgba(37, 99, 235, 0.35);
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.01em;
    }
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
}

.status-live-tag {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  font-weight: 500;
  font-size: 12px;
}

.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  margin-right: 4px;
  box-shadow: 0 0 6px #2563eb;
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.2); }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-btn-clear {
  border-radius: 9999px !important;
  padding: 9px 18px;
  font-size: 13px;
  transition: all 0.2s ease;
}

.header-btn-create {
  border-radius: 9999px !important;
  padding: 9px 20px;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
  }
}

@media (max-width: 640px) {
  .page-header-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
