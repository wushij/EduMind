<template>
  <div
    v-loading="loading"
    element-loading-text="正在检索全校广播推送记录..."
    class="table-card system-table-card"
  >
    <el-table :data="paginatedData" stripe class="broadcast-table">
      <el-table-column prop="id" label="ID" width="80" align="center">
        <template #default="{ row }">
          <span class="id-pill">#{{ row.id }}</span>
        </template>
      </el-table-column>

      <el-table-column label="消息内容" min-width="320">
        <template #default="{ row }">
          <div class="msg-column-wrap">
            <div class="msg-title-line">
              <el-tag
                size="small"
                round
                :type="priorityTagType(row.priority)"
                class="priority-tag"
              >
                <span class="status-dot" :class="'dot-' + priorityTagType(row.priority)" />
                {{ priorityLabel(row.priority) }}
              </el-tag>
              <span class="msg-title-text" :title="row.title">{{ row.title }}</span>
            </div>
            <p class="msg-snippet-text" :title="row.content">{{ row.content }}</p>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="受众对象" width="130" align="center">
        <template #default="{ row }">
          <el-tag size="small" round :type="audienceTagType(row.targetType, row.targetPayload)" class="audience-tag">
            <el-icon class="audience-icon"><component :is="audienceIcon(row.targetType, row.targetPayload)" /></el-icon>
            {{ audienceLabel(row.targetType, row.targetPayload) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="触达 / 已读" width="180" align="center">
        <template #default="{ row }">
          <div
            class="reach-cell-wrap clickable"
            title="点击查看受众已读/未读名单明细"
            @click.stop="$emit('open-recipients', row)"
          >
            <div class="reach-meta-line">
              <span class="read-count">{{ row.readCount || 0 }}</span>
              <span class="slash">/</span>
              <span class="total-count">{{ row.totalCount || 0 }}</span>
              <span class="rate-badge">({{ calcPercent(row.readCount, row.totalCount) }}%)</span>
              <span class="detail-link-pill">
                明细 <el-icon :size="10"><ArrowRight /></el-icon>
              </span>
            </div>
            <el-progress
              :percentage="calcPercent(row.readCount, row.totalCount)"
              :stroke-width="5"
              :show-text="false"
              :color="getReachProgressColor(row.readCount, row.totalCount)"
              class="reach-progress-bar"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="发送人" width="140" align="center">
        <template #default="{ row }">
          <div class="sender-cell">
            <el-avatar
              :size="26"
              :src="resolveSenderAvatar(row)"
              class="sender-avatar-img"
            >
              <el-icon :size="14"><UserFilled /></el-icon>
            </el-avatar>
            <span class="sender-name">{{ row.senderName || 'admin' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="发布时间" width="170" align="center">
        <template #default="{ row }">
          <div class="time-cell">
            <el-icon class="time-icon"><Clock /></el-icon>
            <span>{{ row.createTime ? formatDateTime(row.createTime) : '-' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-pill-group">
            <button type="button" class="table-action-pill table-action-pill--primary" @click="$emit('open-detail', row)">
              详情
            </button>
            <button
              v-if="canDelete"
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click="$emit('delete', row)"
            >
              删除
            </button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @update:current-page="$emit('update:pageNum', $event)"
        @update:page-size="$emit('update:pageSize', $event)"
        @size-change="$emit('size-change', $event)"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Clock, UserFilled, ArrowRight } from '@element-plus/icons-vue';
import { formatDateTime } from '@/utils/format/date';
import type { NotificationBroadcastVO } from '@/types/notification/broadcast';

defineProps<{
  loading: boolean;
  paginatedData: NotificationBroadcastVO[];
  pageNum: number;
  pageSize: number;
  total: number;
  canDelete: boolean;
  priorityLabel: (priority: number) => string;
  priorityTagType: (priority: number) => string;
  audienceLabel: (targetType: string, targetPayload?: string) => string;
  audienceTagType: (targetType: string, targetPayload?: string) => string;
  audienceIcon: (targetType: string, targetPayload?: string) => unknown;
  calcPercent: (readCount?: number, totalCount?: number) => number;
  getReachProgressColor: (readCount?: number, totalCount?: number) => string;
  resolveSenderAvatar: (row: NotificationBroadcastVO) => string;
}>();

defineEmits<{
  'update:pageNum': [value: number];
  'update:pageSize': [value: number];
  'open-detail': [row: NotificationBroadcastVO];
  'open-recipients': [row: NotificationBroadcastVO];
  delete: [row: NotificationBroadcastVO];
  'size-change': [size: number];
  'page-change': [page: number];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.table-card {
  padding: 24px 28px 20px;
}

.broadcast-table {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header) th {
    background: #f8fafc;
    color: #475569;
    font-weight: 600;
    font-size: 13px;
  }
}

.id-pill {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
}

.msg-column-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
}

.msg-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.priority-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  padding: 1px 9px;
  border-radius: 9999px;
  flex-shrink: 0;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;

  &.dot-danger {
    background: #ef4444;
    box-shadow: 0 0 5px rgba(239, 68, 68, 0.8);
  }
  &.dot-warning {
    background: #f59e0b;
    box-shadow: 0 0 5px rgba(245, 158, 11, 0.8);
  }
  &.dot-info {
    background: #94a3b8;
  }
}

.msg-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-snippet-text {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420px;
}

.audience-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-weight: 500;
}

.reach-cell-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 4px 6px;
  border-radius: 8px;
  transition: all 0.2s ease;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: #f8fafc;

      .reach-meta-line .detail-link-pill {
        color: #047857;
      }
    }
  }
}

.reach-meta-line {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #475569;
  font-feature-settings: 'tnum';

  .read-count {
    font-weight: 700;
    color: #0f172a;
  }
  .slash {
    margin: 0 3px;
    color: #cbd5e1;
  }
  .total-count {
    color: #64748b;
  }
  .rate-badge {
    margin-left: 4px;
    color: #059669;
    font-weight: 600;
    font-size: 11px;
  }
  .detail-link-pill {
    margin-left: 6px;
    font-size: 11px;
    color: #059669;
    background: transparent;
    border: none;
    padding: 0 2px;
    display: inline-flex;
    align-items: center;
    gap: 1px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      color: #047857;
      text-decoration: underline;
    }
  }
}

.reach-progress-bar {
  width: 100%;
}

.sender-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.sender-avatar-img {
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
  color: #ffffff;
  border: 1.5px solid #e0e7ff;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
  flex-shrink: 0;

  :deep(img) {
    object-fit: cover;
    width: 100%;
    height: 100%;
  }
}

.sender-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;

  .time-icon {
    font-size: 13px;
    color: #94a3b8;
  }
}

.action-pill-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-pill-btn {
  border-radius: 9999px !important;
  padding: 4px 13px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  height: 28px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.action-pill-btn-detail {
  background: #f0f7ff !important;
  border: 1px solid #bfdbfe !important;
  color: #1d4ed8 !important;

  &:hover {
    background: #2563eb !important;
    border-color: #2563eb !important;
    color: #ffffff !important;
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3) !important;
    transform: translateY(-1px);
  }
}

.action-pill-btn-delete {
  background: #fef2f2 !important;
  border: 1px solid #fecaca !important;
  color: #dc2626 !important;

  &:hover {
    background: #ef4444 !important;
    border-color: #ef4444 !important;
    color: #ffffff !important;
    box-shadow: 0 4px 10px rgba(239, 68, 68, 0.3) !important;
    transform: translateY(-1px);
  }
}

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
