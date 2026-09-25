<template>
  <el-dialog
    v-model="visible"
    width="660px"
    append-to-body
    class="notify-detail-dialog"
    :show-close="true"
    @close="emit('close')"
  >
    <template #header>
      <div class="notify-detail-header">
        <div class="notify-detail-badges">
          <span class="notify-type-badge" :class="item?.typeClass">{{ item?.typeLabel }}</span>
          <span v-if="item?.metaTag" class="notify-detail-meta">{{ item?.metaTag }}</span>
        </div>
        <h3 class="notify-detail-title">{{ item?.title }}</h3>
      </div>
    </template>

    <div v-if="item" class="notify-detail-body">
      <p v-if="item.rawTime" class="notify-detail-time">{{ formatDateTime(item.rawTime) }}</p>
      <div class="notify-detail-content" v-html="formattedContent"></div>
      <div v-if="item.navigatePath" class="notify-detail-nav">
        <el-button type="primary" link @click="emit('navigate', item)">
          查看关联内容 →
        </el-button>
      </div>
    </div>

    <template #footer>
      <div class="notify-detail-footer">
        <button type="button" class="notify-known-btn" @click="emit('close')">
          <el-icon><CircleCheck /></el-icon>
          我已知晓
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { CircleCheck } from '@element-plus/icons-vue';
import { formatDateTime } from '@/utils/format/date';
import { formatStructuredProposal } from '@/utils/format/structured-text';
import type { NotifyListItem } from '@/composables/notification/useNotificationList';

const props = defineProps<{
  modelValue: boolean;
  item: NotifyListItem | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  close: [];
  navigate: [item: NotifyListItem];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const formattedContent = computed(() => {
  if (!props.item?.content) return '';
  return formatStructuredProposal(props.item.content);
});
</script>

<style scoped lang="scss">
.notify-detail-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notify-detail-badges {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notify-detail-meta {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
}

.notify-detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.4;
}

.notify-detail-time {
  margin: 0 0 12px;
  font-size: 12px;
  color: #94a3b8;
}

.notify-detail-content {
  font-size: 14px;
  line-height: 1.75;
  color: #334155;

  :deep(.structured-intro-p) {
    margin: 0 0 12px;
    line-height: 1.75;
    color: #334155;
    word-break: break-word;
  }

  :deep(.structured-point-card) {
    margin-top: 12px;
    padding: 13px 16px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-left: 4px solid #2563eb;
    border-radius: 10px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:first-child {
      margin-top: 0;
    }

    &:hover {
      border-color: #cbd5e1;
      border-left-color: #1d4ed8;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
    }

    .point-badge-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 7px;

      .point-num-pill {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 22px;
        height: 22px;
        padding: 0 7px;
        font-size: 12px;
        font-weight: 700;
        color: #ffffff;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        border-radius: 999px;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
      }

      .point-title-text {
        font-size: 14px;
        font-weight: 700;
        color: #0f172a;
        letter-spacing: 0.2px;
      }
    }

    .point-body-text {
      font-size: 13.5px;
      line-height: 1.75;
      color: #334155;
      word-break: break-word;

      :deep(.katex) {
        font-size: 1.05em;
      }
    }
  }
}

.notify-detail-nav {
  margin-top: 16px;
}

.notify-detail-footer {
  display: flex;
  justify-content: flex-end;
}

.notify-known-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #fff;
  border: none;
  font-weight: 700;
  font-size: 13px;
  padding: 9px 24px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.35);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(22, 119, 255, 0.45);
  }
}
</style>

<style lang="scss">
.notify-detail-dialog {
  border-radius: 16px !important;

  .el-dialog__header {
    padding: 20px 24px 8px !important;
    margin-right: 0 !important;
  }

  .el-dialog__body {
    padding: 8px 24px 16px !important;
    max-height: 72vh;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 6px;
    }
    &::-webkit-scrollbar-thumb {
      background: rgba(148, 163, 184, 0.4);
      border-radius: 3px;
    }
    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }

  .el-dialog__footer {
    padding: 0 24px 20px !important;
  }
}
</style>
