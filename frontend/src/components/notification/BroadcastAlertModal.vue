<template>
  <el-dialog
    :model-value="!!alertItem"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    width="480px"
    class="em-broadcast-alert-dialog"
    destroy-on-close
    center
    align-center
  >
    <div v-if="alertItem" class="alert-modal-content">
      <div class="alert-modal-header">
        <div class="alert-icon-ring">
          <el-icon class="pulse-bell"><BellFilled /></el-icon>
        </div>
        <div class="alert-badge-pill">重要系统公告</div>
      </div>

      <h3 class="alert-title">{{ alertItem.title }}</h3>

      <div class="alert-body-text">
        {{ alertItem.content }}
      </div>

      <div v-if="displayTime" class="alert-time">
        发布于 {{ displayTime }}
      </div>

      <div class="alert-footer-actions">
        <el-button type="primary" round class="alert-confirm-btn" @click="handleConfirm">
          我知道了
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { BellFilled } from '@element-plus/icons-vue';
import { formatDateTime } from '@/utils/format/date';
import { useBroadcastPush } from '@/composables/notification/useBroadcastPush';

const { alertItem, dismissAlert } = useBroadcastPush();

const displayTime = computed(() => {
  const item = alertItem.value;
  if (!item) return '';
  const time = 'createTime' in item ? item.createTime : '';
  return time ? formatDateTime(time) : '';
});

async function handleConfirm() {
  await dismissAlert();
}
</script>

<style scoped lang="scss">
:deep(.em-broadcast-alert-dialog) {
  border-radius: 20px;
  overflow: hidden;
  padding: 0;
}

.alert-modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 32px 24px 24px;
}

.alert-modal-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.alert-icon-ring {
  width: 56px;
  height: 56px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(249, 115, 22, 0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
  font-size: 26px;
}

.pulse-bell {
  animation: bell-bounce 2s infinite ease-in-out;
}

@keyframes bell-bounce {
  0%,
  100% {
    transform: rotate(0);
  }
  20% {
    transform: rotate(15deg);
  }
  40% {
    transform: rotate(-15deg);
  }
  60% {
    transform: rotate(7deg);
  }
  80% {
    transform: rotate(-7deg);
  }
}

.alert-badge-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 9999px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.alert-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 12px;
  line-height: 1.4;
}

.alert-body-text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 16px;
  max-height: 240px;
  overflow-y: auto;
  word-break: break-word;
}

.alert-time {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 24px;
}

.alert-footer-actions {
  width: 100%;
}

.alert-confirm-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}
</style>
