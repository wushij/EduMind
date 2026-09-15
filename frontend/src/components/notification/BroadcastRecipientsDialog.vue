<template>
  <el-dialog
    :model-value="visible"
    width="760px"
    destroy-on-close
    class="broadcast-recipients-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-header-custom">
        <div class="header-icon-box">
          <el-icon :size="20"><UserFilled /></el-icon>
        </div>
        <div class="header-text-group">
          <div class="header-main-title">
            <span>受众触达与已读明细</span>
            <el-tag v-if="broadcast" size="small" round class="broadcast-id-pill">
              广播 #{{ broadcast.id }}
            </el-tag>
          </div>
          <span class="header-subtitle-text" :title="broadcast?.title">
            广播主题：「{{ broadcast?.title || '-' }}」
          </span>
        </div>
      </div>
    </template>

    <div class="dialog-content-wrap">
      <!-- 顶部 3 大核心穿透指标卡片 -->
      <div class="metrics-kpi-row">
        <div class="kpi-box kpi-blue">
          <div class="kpi-icon">
            <el-icon :size="18"><UserFilled /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">下发总人数</span>
            <div class="kpi-val-group">
              <span class="kpi-number">{{ summary?.totalCount ?? broadcast?.totalCount ?? 0 }}</span>
              <span class="kpi-unit">人</span>
            </div>
            <span class="kpi-hint">计划触达全量成员</span>
          </div>
        </div>

        <div class="kpi-box kpi-emerald">
          <div class="kpi-icon">
            <el-icon :size="18"><CircleCheckFilled /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-label-row">
              <span class="kpi-label">已读确认</span>
              <span class="kpi-rate-badge">{{ currentReadRate }}%</span>
            </div>
            <div class="kpi-val-group">
              <span class="kpi-number highlight-green">
                {{ summary?.readCount ?? broadcast?.readCount ?? 0 }}
              </span>
              <span class="kpi-unit">人</span>
            </div>
            <div class="kpi-progress-strip">
              <el-progress
                :percentage="currentReadRate"
                :stroke-width="4"
                :show-text="false"
                color="#10b981"
              />
            </div>
          </div>
        </div>

        <div class="kpi-box kpi-amber">
          <div class="kpi-icon">
            <el-icon :size="18"><Clock /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">待查阅人数</span>
            <div class="kpi-val-group">
              <span class="kpi-number highlight-amber">{{ currentUnreadCount }}</span>
              <span class="kpi-unit">人</span>
            </div>
            <span class="kpi-hint">尚未打开并查阅通知</span>
          </div>
        </div>
      </div>

      <!-- 快捷筛选工具栏 -->
      <div class="filter-strip">
        <div class="status-tabs-pill">
          <div
            class="tab-pill-item"
            :class="{ active: currentReadFilter === undefined }"
            @click="handleStatusTabChange(undefined)"
          >
            全部受众 ({{ summary?.totalCount ?? broadcast?.totalCount ?? 0 }})
          </div>
          <div
            class="tab-pill-item tab-read"
            :class="{ active: currentReadFilter === 1 }"
            @click="handleStatusTabChange(1)"
          >
            <span class="dot-indicator dot-green" />
            已读 ({{ summary?.readCount ?? broadcast?.readCount ?? 0 }})
          </div>
          <div
            class="tab-pill-item tab-unread"
            :class="{ active: currentReadFilter === 0 }"
            @click="handleStatusTabChange(0)"
          >
            <span class="dot-indicator dot-gray" />
            未读 ({{ currentUnreadCount }})
          </div>
        </div>

        <div class="filter-right-inputs">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名或真实姓名..."
            clearable
            :prefix-icon="Search"
            class="keyword-search-input"
            @clear="fetchRecipients"
            @keyup.enter="fetchRecipients"
          />
          <el-button round :icon="Refresh" class="btn-refresh" @click="fetchRecipients">
            刷新
          </el-button>
        </div>
      </div>

      <!-- 受众穿透数据表格 -->
      <div v-loading="loading" class="recipients-table-card">
        <el-table :data="tableList" stripe height="340" class="recipients-table">
          <el-table-column label="受众成员" min-width="180">
            <template #default="{ row }">
              <div class="user-info-cell">
                <el-avatar :size="28" :src="row.avatar" class="user-avatar">
                  <el-icon :size="14"><UserFilled /></el-icon>
                </el-avatar>
                <div class="user-name-group">
                  <span class="user-realname">{{ row.realName || row.username }}</span>
                  <span class="user-account">@{{ row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="身份角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                round
                :type="roleTagType(row.roleCode)"
                class="role-pill-tag"
              >
                {{ row.roleName || '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="阅读状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.isRead === 1"
                size="small"
                round
                type="success"
                effect="light"
                class="read-status-tag"
              >
                <el-icon class="mr-1"><CircleCheckFilled /></el-icon> 已读
              </el-tag>
              <el-tag
                v-else
                size="small"
                round
                type="info"
                effect="light"
                class="unread-status-tag"
              >
                <el-icon class="mr-1"><Clock /></el-icon> 未读
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="触达下发时间" width="180" align="center">
            <template #default="{ row }">
              <div class="time-cell">
                <el-icon class="time-icon"><Clock /></el-icon>
                <span>{{ row.createTime ? formatDateTime(row.createTime) : '-' }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-wrap">
        <el-button round class="btn-close-pill" @click="emit('update:visible', false)">
          关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  UserFilled,
  CircleCheckFilled,
  Clock,
  Search,
  Refresh
} from '@element-plus/icons-vue';
import { fetchBroadcastRecipients } from '@/composables/system/useBroadcast';
import type {
  BroadcastRecipientSummaryVO,
  BroadcastRecipientVO,
  NotificationBroadcastVO
} from '@/types/notification/broadcast';
import { formatDateTime } from '@/utils/format/date';

const props = defineProps<{
  visible: boolean;
  broadcast: NotificationBroadcastVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
}>();

const loading = ref(false);
const summary = ref<BroadcastRecipientSummaryVO | null>(null);
const currentReadFilter = ref<number | undefined>(undefined);
const keyword = ref('');
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableList = ref<BroadcastRecipientVO[]>([]);

const currentReadRate = computed(() => {
  if (summary.value?.readRate !== undefined) {
    return summary.value.readRate;
  }
  const t = props.broadcast?.totalCount || 0;
  const r = props.broadcast?.readCount || 0;
  if (t <= 0) return 0;
  return Math.min(Math.round((r / t) * 100), 100);
});

const currentUnreadCount = computed(() => {
  if (summary.value?.unreadCount !== undefined) {
    return summary.value.unreadCount;
  }
  const t = props.broadcast?.totalCount || 0;
  const r = props.broadcast?.readCount || 0;
  return Math.max(0, t - r);
});

function roleTagType(roleCode?: string): 'primary' | 'success' | 'warning' | 'info' {
  if (!roleCode) return 'info';
  const c = roleCode.toUpperCase();
  if (c.includes('ADMIN')) return 'primary';
  if (c.includes('TEACHER')) return 'success';
  if (c.includes('STUDENT')) return 'warning';
  return 'info';
}

async function fetchRecipients() {
  if (!props.broadcast?.id) return;
  loading.value = true;
  try {
    const res = await fetchBroadcastRecipients(props.broadcast.id, {
      isRead: currentReadFilter.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value
    });
    if (res.data) {
      summary.value = res.data;
      tableList.value = res.data.recipients?.list || [];
      total.value = res.data.recipients?.total || 0;
    }
  } catch {
    /* ignore */
  } finally {
    loading.value = false;
  }
}

function handleStatusTabChange(status?: number) {
  currentReadFilter.value = status;
  page.value = 1;
  fetchRecipients();
}

function handlePageChange(p: number) {
  page.value = p;
  fetchRecipients();
}

watch(
  () => props.visible,
  (open) => {
    if (open && props.broadcast?.id) {
      keyword.value = '';
      currentReadFilter.value = undefined;
      page.value = 1;
      fetchRecipients();
    } else {
      summary.value = null;
      tableList.value = [];
    }
  }
);
</script>

<style scoped lang="scss">
.dialog-header-custom {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-box {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  flex-shrink: 0;
}

.header-text-group {
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
}

.header-main-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.broadcast-id-pill {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.header-subtitle-text {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 580px;
}

.dialog-content-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
}

/* 3 大 KPI 指标卡片 */
.metrics-kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.kpi-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    transform: translateY(-1px);
  }

  &.kpi-blue .kpi-icon {
    background: #eff6ff;
    color: #2563eb;
  }
  &.kpi-emerald .kpi-icon {
    background: #ecfdf5;
    color: #059669;
  }
  &.kpi-amber .kpi-icon {
    background: #fffbeb;
    color: #d97706;
  }
}

.kpi-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.kpi-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.kpi-rate-badge {
  font-size: 11px;
  font-weight: 700;
  color: #059669;
  background: #d1fae5;
  padding: 1px 6px;
  border-radius: 9999px;
}

.kpi-val-group {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.kpi-number {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
  font-feature-settings: 'tnum';

  &.highlight-green {
    color: #059669;
  }
  &.highlight-amber {
    color: #d97706;
  }
}

.kpi-unit {
  font-size: 11px;
  color: #94a3b8;
}

.kpi-hint {
  font-size: 11px;
  color: #94a3b8;
}

.kpi-progress-strip {
  margin-top: 3px;
}

/* 筛选与状态切换栏 */
.filter-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.status-tabs-pill {
  display: flex;
  background: #e2e8f0;
  border-radius: 9999px;
  padding: 3px;
  gap: 2px;
}

.tab-pill-item {
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s ease;

  &:hover {
    color: #0f172a;
  }

  &.active {
    background: #ffffff;
    color: #0f172a;
    font-weight: 600;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }
}

.dot-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;

  &.dot-green {
    background: #10b981;
  }
  &.dot-gray {
    background: #f59e0b;
  }
}

.filter-right-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.keyword-search-input {
  width: 220px;
}

.btn-refresh {
  border-radius: 9999px !important;
}

/* 表格样式 */
.recipients-table-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;

  :deep(.el-table__header) th {
    background: #f8fafc;
    color: #475569;
    font-weight: 600;
    font-size: 12px;
  }
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
  color: #ffffff;
  border: 1px solid #e0e7ff;
  flex-shrink: 0;

  :deep(img) {
    object-fit: cover;
  }
}

.user-name-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-realname {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.user-account {
  font-size: 11px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.role-pill-tag {
  font-weight: 500;
  font-size: 11px;
  padding: 1px 8px;
}

.read-status-tag,
.unread-status-tag {
  font-weight: 600;
  font-size: 11px;
  padding: 2px 10px;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;

  .time-icon {
    color: #94a3b8;
    font-size: 13px;
  }
}

.pagination-bar {
  padding: 10px 14px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f1f5f9;
}

.dialog-footer-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
}

.btn-close-pill {
  border-radius: 9999px !important;
  padding: 8px 24px;
}
</style>
