<template>
  <el-dialog
    :model-value="visible"
    width="640px"
    destroy-on-close
    class="broadcast-detail-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-header-wrap">
        <div class="header-icon-badge">
          <el-icon :size="20"><Promotion /></el-icon>
        </div>
        <div class="header-title-box">
          <div class="header-main-row">
            <span class="header-title-text">广播推送详情</span>
            <el-tag
              v-if="broadcast"
              size="small"
              round
              :type="priorityTagType(broadcast.priority)"
              class="priority-tag"
            >
              {{ priorityLabel(broadcast.priority) }}
            </el-tag>
            <span v-if="broadcast" class="id-badge">#{{ broadcast.id }}</span>
          </div>
          <span class="header-sub-text">查看历史广播的下发范围、师生触达与正文内容</span>
        </div>
      </div>
    </template>

    <div v-if="broadcast" class="dialog-body-container">
      <!-- 广播标题展示 -->
      <div class="broadcast-title-banner">
        <h3>{{ broadcast.title }}</h3>
      </div>

      <!-- 核心指标统计卡片网格 -->
      <div class="stats-overview-grid">
        <div class="overview-box">
          <span class="overview-label">推送目标受众</span>
          <div class="overview-val-row">
            <el-tag size="small" round :type="audienceTagType(broadcast.targetType, broadcast.targetPayload)">
              {{ audienceLabel(broadcast.targetType, broadcast.targetPayload) }}
            </el-tag>
          </div>
        </div>

        <div class="overview-box">
          <span class="overview-label">覆盖下发人数</span>
          <div class="overview-val-row">
            <span class="overview-num">{{ broadcast.totalCount || 0 }}</span>
            <span class="overview-unit">人</span>
          </div>
        </div>

        <div class="overview-box overview-box-interactive" @click="emit('open-recipients')">
          <div class="overview-header-row">
            <span class="overview-label">已读确认人数</span>
            <span class="detail-badge-pill">明细 &gt;</span>
          </div>
          <div class="overview-val-row">
            <span class="overview-num highlight-emerald">{{ broadcast.readCount || 0 }}</span>
            <span class="overview-unit">人</span>
          </div>
        </div>

        <div class="overview-box">
          <span class="overview-label">触达已读率</span>
          <div class="overview-val-row">
            <span class="overview-num highlight-amber">{{ calcRate(broadcast.readCount, broadcast.totalCount) }}%</span>
          </div>
          <el-progress
            :percentage="calcRate(broadcast.readCount, broadcast.totalCount)"
            :stroke-width="4"
            :show-text="false"
            :color="calcRate(broadcast.readCount, broadcast.totalCount) >= 60 ? '#10b981' : '#f59e0b'"
            style="margin-top: 4px"
          />
        </div>
      </div>

      <!-- 基础元信息列表 -->
      <div class="meta-info-strip">
        <div class="meta-item">
          <span class="meta-label">提醒级别：</span>
          <span class="meta-val font-semibold">{{ priorityDetailedLabel(broadcast.priority) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">下发人：</span>
          <span class="meta-val font-mono sender-meta-val">
            <el-avatar
              :size="20"
              :src="resolveSenderAvatar(broadcast)"
              class="detail-sender-avatar"
            >
              <el-icon :size="12"><UserFilled /></el-icon>
            </el-avatar>
            <span>{{ broadcast.senderName || 'admin' }}</span>
          </span>
        </div>
        <div class="meta-item">
          <span class="meta-label">发布时间：</span>
          <span class="meta-val font-mono">
            {{ broadcast.createTime ? formatDateTime(broadcast.createTime) : '-' }}
          </span>
        </div>
      </div>

      <!-- 广播正文详情卡片 -->
      <div class="content-card-wrap">
        <div class="content-header">
          <span class="content-title">
            <el-icon class="mr-1"><Document /></el-icon> 广播正文内容
          </span>
          <el-button
            size="small"
            round
            class="copy-pill-btn"
            :icon="DocumentCopy"
            @click="handleCopy(broadcast.content)"
          >
            复制正文
          </el-button>
        </div>
        <div class="content-body">{{ broadcast.content }}</div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-wrap">
        <el-button
          round
          class="btn-recipients-pill"
          :icon="UserFilled"
          @click="emit('open-recipients')"
        >
          查看已读名单明细
        </el-button>
        <el-button round class="btn-close-pill" @click="emit('update:visible', false)">
          关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { Promotion, Document, DocumentCopy, UserFilled } from '@element-plus/icons-vue';
import { formatDateTime } from '@/utils/format/date';
import { useAuthStore } from '@/stores/auth/auth';
import type { NotificationBroadcastVO } from '@/types/notification/broadcast';

const authStore = useAuthStore();

defineProps<{
  visible: boolean;
  broadcast: NotificationBroadcastVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'open-recipients'): void;
}>();

function resolveSenderAvatar(b: NotificationBroadcastVO | null): string {
  if (!b) return '';
  if (b.senderAvatar) return b.senderAvatar;
  if (
    authStore.currentUser &&
    (b.senderId === authStore.currentUser.id ||
      (b.senderName && b.senderName === authStore.currentUser.username))
  ) {
    return authStore.currentUser.avatar || '';
  }
  return '';
}

function priorityLabel(p?: number) {
  if (p === 2) return '紧急公告';
  if (p === 1) return '重要弹窗';
  return '普通广播';
}

function priorityDetailedLabel(p?: number) {
  if (p === 2) return '紧急公告（全站置顶横幅）';
  if (p === 1) return '重要提醒（弹窗强提醒）';
  return '普通广播（常规站内信）';
}

function priorityTagType(p?: number): 'danger' | 'warning' | 'info' {
  if (p === 2) return 'danger';
  if (p === 1) return 'warning';
  return 'info';
}

function audienceLabel(type?: string, payload?: string) {
  if (type === 'all') return '全校全体用户';
  if (type === 'role') {
    const map: Record<string, string> = {
      ADMIN: '系统管理员',
      TEACHER: '教师角色',
      STUDENT: '学生角色'
    };
    return map[payload || ''] || payload || '指定身份角色';
  }
  return type || '全员';
}

function audienceTagType(type?: string, payload?: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'all') return 'primary';
  if (payload === 'TEACHER') return 'success';
  if (payload === 'STUDENT') return 'warning';
  return 'info';
}

function calcRate(read?: number, total?: number) {
  if (!total || total <= 0) return 0;
  return Math.min(Math.round(((read || 0) / total) * 100), 100);
}

function handleCopy(text?: string) {
  if (!text) return;
  navigator.clipboard.writeText(text);
  ElMessage.success('广播正文已复制到剪贴板');
}
</script>

<style scoped lang="scss">
.dialog-header-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  flex-shrink: 0;
}

.header-title-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-main-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title-text {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.priority-tag {
  font-weight: 600;
  padding: 1px 8px;
}

.id-badge {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}

.header-sub-text {
  font-size: 12px;
  color: #64748b;
}

.dialog-body-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0;
}

.broadcast-title-banner {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 16px;

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
    line-height: 1.4;
  }
}

.stats-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.overview-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  &.overview-box-interactive {
    cursor: pointer;
    transition: all 0.2s ease;
    border-color: #a7f3d0;
    background: #f0fdf4;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
      border-color: #10b981;

      .detail-badge-pill {
        color: #047857;
        text-decoration: underline;
      }
    }
  }

  .overview-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .detail-badge-pill {
    font-size: 10px;
    font-weight: 500;
    color: #059669;
    background: transparent;
    border: none;
    padding: 0;
    transition: all 0.2s ease;
  }

  .overview-label {
    font-size: 11px;
    color: #64748b;
  }

  .overview-val-row {
    display: flex;
    align-items: baseline;
    gap: 2px;
  }

  .overview-num {
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
    font-feature-settings: 'tnum';

    &.highlight-emerald {
      color: #059669;
    }
    &.highlight-amber {
      color: #d97706;
    }
  }

  .overview-unit {
    font-size: 11px;
    color: #94a3b8;
  }
}

.meta-info-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  padding: 10px 14px;
  background: #f1f5f9;
  border-radius: 8px;
  font-size: 12px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .meta-label {
    color: #64748b;
  }

  .meta-val {
    color: #1e293b;

    &.font-semibold {
      font-weight: 600;
    }
    &.font-mono {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    }
  }

  .sender-meta-val {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .detail-sender-avatar {
    background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
    color: #fff;
    border: 1px solid #e0e7ff;
    box-shadow: 0 1px 4px rgba(59, 130, 246, 0.2);

    :deep(img) {
      object-fit: cover;
      width: 100%;
      height: 100%;
    }
  }
}

.content-card-wrap {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  overflow: hidden;

  .content-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;

    .content-title {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      display: flex;
      align-items: center;
    }
  }

  .content-body {
    padding: 14px 16px;
    white-space: pre-wrap;
    line-height: 1.6;
    font-size: 13px;
    color: #334155;
    max-height: 260px;
    overflow-y: auto;
  }
}

.copy-pill-btn {
  border-radius: 9999px !important;
  font-size: 11px;
  padding: 3px 10px;
}

.dialog-footer-wrap {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;
}

.btn-recipients-pill {
  border-radius: 9999px !important;
  padding: 8px 18px !important;
  background: #ecfdf5 !important;
  border: 1px solid #a7f3d0 !important;
  color: #047857 !important;
  font-weight: 500 !important;
  font-size: 13px !important;
  transition: all 0.2s ease !important;

  &:hover {
    background: #10b981 !important;
    border-color: #10b981 !important;
    color: #ffffff !important;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25) !important;
    transform: translateY(-1px);
  }
}

.btn-close-pill {
  border-radius: 9999px !important;
  padding: 8px 24px;
}
</style>
