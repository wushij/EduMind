<template>
  <div class="path-task-row">
    <span class="task-dot" :class="{ 'task-dot--done': task.status === 'COMPLETED' }"></span>
    <div class="task-main">
      <div class="task-title-row">
        <span class="task-type-pill">{{ typeLabel }}</span>
        <span class="task-title" :class="{ 'task-title--done': task.status === 'COMPLETED' }">
          {{ task.title }}
        </span>
      </div>
      <div v-if="task.estimatedMinutes" class="task-meta">
        预计 {{ task.estimatedMinutes }} 分钟 · {{ statusLabel }}
      </div>
    </div>
    <button
      v-if="task.status !== 'COMPLETED'"
      type="button"
      class="capsule-task-btn"
      @click="emit('execute', task)"
    >
      {{ task.actionLabel || '开始' }}
    </button>
    <span v-else class="task-status-tag tag--done">已完成</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { LearningPathTask } from '@/types/learning/learning-path';
import {
  resolvePathTaskStatusLabel,
  resolvePathTaskTypeLabel
} from '@/constants/learning-path';

const props = defineProps<{
  task: LearningPathTask;
}>();

const emit = defineEmits<{
  (e: 'execute', task: LearningPathTask): void;
}>();

const typeLabel = computed(() =>
  props.task.typeLabel || resolvePathTaskTypeLabel(props.task.type)
);
const statusLabel = computed(() => resolvePathTaskStatusLabel(props.task.status));
</script>

<style scoped lang="scss">
.path-task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #f1f5f9;

  .task-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #cbd5e1;
    flex-shrink: 0;

    &--done {
      background: #10b981;
    }
  }

  .task-main {
    flex: 1;
    min-width: 0;
  }

  .task-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .task-type-pill {
    font-size: 11px;
    padding: 1px 8px;
    border-radius: 9999px;
    background: #eff6ff;
    color: #1d4ed8;
    font-weight: 600;
  }

  .task-title {
    font-size: 13px;
    font-weight: 600;
    color: #334155;

    &--done {
      color: #94a3b8;
      text-decoration: line-through;
    }
  }

  .task-meta {
    margin-top: 4px;
    font-size: 11px;
    color: #64748b;
  }

  .capsule-task-btn {
    border: none;
    cursor: pointer;
    padding: 6px 14px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;

    &:hover {
      opacity: 0.92;
    }
  }

  .task-status-tag {
    font-size: 11px;
    padding: 2px 10px;
    border-radius: 9999px;
    font-weight: 600;

    &.tag--done {
      background: #dcfce7;
      color: #15803d;
    }
  }
}
</style>
