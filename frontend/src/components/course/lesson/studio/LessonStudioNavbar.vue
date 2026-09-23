<template>
  <header class="lesson-studio-navbar">
    <div class="navbar-left">
      <button type="button" class="back-btn" @click="emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        <span>教学大纲</span>
      </button>
      <div class="title-preview">
        <span class="title-text">{{ title || '未命名课节' }}</span>
        <span class="status-pill" :class="statusClass">{{ statusLabel }}</span>
      </div>
    </div>

    <div class="navbar-center">
      <span class="meta-item">
        <el-icon><EditPen /></el-icon>
        {{ wordCount }} 字
      </span>
      <span class="meta-dot">·</span>
      <span class="meta-item">
        <el-icon><Timer /></el-icon>
        约 {{ readMinutes }} 分钟
      </span>
      <span class="meta-dot">·</span>
      <span class="save-status" :class="{ 'is-saving': saving }">
        <el-icon v-if="saving" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><CircleCheck /></el-icon>
        {{ saveStatusText }}
      </span>
    </div>

    <div class="navbar-right">
      <el-radio-group
        :model-value="viewMode"
        size="small"
        class="view-mode-group"
        @update:model-value="(v: string) => emit('update:viewMode', v as ViewMode)"
      >
        <el-radio-button value="edit">编辑</el-radio-button>
        <el-radio-button value="split">分屏</el-radio-button>
        <el-radio-button value="preview">预览</el-radio-button>
      </el-radio-group>

      <button
        type="button"
        class="capsule-btn secondary"
        :class="{ active: sidebarOpen }"
        @click="emit('toggle-sidebar')"
      >
        <el-icon><Operation /></el-icon>
        课节属性
      </button>

      <button type="button" class="capsule-btn secondary" :disabled="saving" @click="emit('save-draft')">
        保存草稿
      </button>

      <button
        v-if="contentStatus !== 'PUBLISHED'"
        type="button"
        class="capsule-btn primary"
        :disabled="saving"
        @click="emit('publish')"
      >
        发布课节
      </button>
      <button v-else type="button" class="capsule-btn warn" :disabled="saving" @click="emit('unpublish')">
        撤回草稿
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ArrowLeft,
  EditPen,
  Timer,
  Loading,
  CircleCheck,
  Operation
} from '@element-plus/icons-vue';

export type ViewMode = 'split' | 'edit' | 'preview';

const props = defineProps<{
  title: string;
  wordCount: number;
  readMinutes: number;
  saveStatusText: string;
  saving: boolean;
  viewMode: ViewMode;
  sidebarOpen: boolean;
  contentStatus?: string;
}>();

const emit = defineEmits<{
  back: [];
  'save-draft': [];
  publish: [];
  unpublish: [];
  'update:viewMode': [ViewMode];
  'toggle-sidebar': [];
}>();

const statusLabel = computed(() => (props.contentStatus === 'PUBLISHED' ? '已发布' : '草稿'));
const statusClass = computed(() =>
  props.contentStatus === 'PUBLISHED' ? 'status-pill--published' : 'status-pill--draft'
);
</script>

<style scoped lang="scss">
.lesson-studio-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 20;
  flex-wrap: wrap;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: #f1f5f9;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
  color: #475569;
  font-size: 13px;
}

.title-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.title-text {
  font-weight: 700;
  color: #0f172a;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.status-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;

  &--draft {
    background: #fef3c7;
    color: #b45309;
  }

  &--published {
    background: #dcfce7;
    color: #15803d;
  }
}

.navbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-dot {
  opacity: 0.5;
}

.save-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  &.is-saving {
    color: #2563eb;
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.view-mode-group {
  margin-right: 4px;
}

.capsule-btn {
  border: none;
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  // <button> 默认 inline-block，图标按基线对齐会显得歪且与文字无间距；
  // 纯文字按钮同样居中显示，不受影响
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &.secondary {
    background: #f1f5f9;
    color: #334155;

    &.active {
      background: #e0e7ff;
      color: #4338ca;
    }
  }

  &.primary {
    background: #2563eb;
    color: #fff;
  }

  &.warn {
    background: #fff7ed;
    color: #c2410c;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>
