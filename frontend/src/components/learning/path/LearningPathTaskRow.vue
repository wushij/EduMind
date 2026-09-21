<template>
  <div class="path-task-row" :class="{ 'is-completed': task.status === 'COMPLETED' }">
    <!-- 任务类型图标底座 -->
    <div class="task-type-badge" :class="typeBadgeClass">
      <el-icon v-if="task.type === 'LESSON'"><Reading /></el-icon>
      <el-icon v-else-if="task.type === 'PRACTICE' || task.type === 'EXERCISE'"><EditPen /></el-icon>
      <el-icon v-else-if="task.type === 'AI_TUTOR'"><MagicStick /></el-icon>
      <el-icon v-else><DocumentChecked /></el-icon>
    </div>

    <!-- 任务核心主体 -->
    <div class="task-main">
      <div class="task-tag-row">
        <span class="task-type-pill" :class="typePillClass">{{ typeLabel }}</span>
        <span v-if="task.estimatedMinutes" class="task-meta-time">
          <el-icon class="time-icon"><Timer /></el-icon>
          预计 {{ task.estimatedMinutes }} 分钟
        </span>
      </div>

      <div class="task-title-text" :class="{ 'title--done': task.status === 'COMPLETED' }" :title="task.title">
        {{ cleanTaskTitle }}
      </div>
    </div>

    <!-- 右侧操作按钮 -->
    <div class="task-action-box">
      <button
        v-if="task.status !== 'COMPLETED'"
        type="button"
        class="capsule-task-btn"
        :class="btnTypeClass"
        @click="emit('execute', task)"
      >
        <el-icon v-if="task.type === 'AI_TUTOR'" class="btn-icon"><MagicStick /></el-icon>
        <el-icon v-else-if="task.type === 'LESSON'" class="btn-icon"><VideoPlay /></el-icon>
        <el-icon v-else class="btn-icon"><Edit /></el-icon>
        <span>{{ task.actionLabel || '开始' }}</span>
      </button>
      <span v-else class="task-status-tag tag--done">
        <el-icon><Check /></el-icon>
        已完成
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Reading,
  EditPen,
  MagicStick,
  DocumentChecked,
  Timer,
  VideoPlay,
  Edit,
  Check
} from '@element-plus/icons-vue';
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

/** 净化任务标题：若标题前缀重复写了「巩固练习：」「学习章节：」，予以美化精简 */
const cleanTaskTitle = computed(() => {
  const t = props.task.title || '';
  return t
    .replace(/^学习章节[:：]\s*/, '')
    .replace(/^巩固练习[:：]\s*/, '')
    .replace(/^AI 讲解巩固[:：]\s*/, '')
    .replace(/^AI讲解巩固[:：]\s*/, '')
    .trim() || t;
});

const typeBadgeClass = computed(() => {
  const type = props.task.type;
  if (type === 'LESSON') return 'badge--lesson';
  if (type === 'PRACTICE' || type === 'EXERCISE') return 'badge--practice';
  if (type === 'AI_TUTOR') return 'badge--ai';
  return 'badge--default';
});

const typePillClass = computed(() => {
  const type = props.task.type;
  if (type === 'LESSON') return 'pill--lesson';
  if (type === 'PRACTICE' || type === 'EXERCISE') return 'pill--practice';
  if (type === 'AI_TUTOR') return 'pill--ai';
  return 'pill--default';
});

const btnTypeClass = computed(() => {
  const type = props.task.type;
  if (type === 'LESSON') return 'btn--lesson';
  if (type === 'PRACTICE' || type === 'EXERCISE') return 'btn--practice';
  if (type === 'AI_TUTOR') return 'btn--ai';
  return 'btn--default';
});
</script>

<style scoped lang="scss">
.path-task-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fbfcfe;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  transition: all 0.2s ease;

  &:hover {
    background: #ffffff;
    border-color: #cbd5e1;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    transform: translateY(-1px);
  }

  &.is-completed {
    background: #f8fafc;
    opacity: 0.85;
  }

  // 1. 图标底座
  .task-type-badge {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 17px;
    flex-shrink: 0;

    &.badge--lesson {
      background: #eff6ff;
      color: #2563eb;
      border: 1px solid rgba(37, 99, 235, 0.15);
    }
    &.badge--practice {
      background: #fff7ed;
      color: #ea580c;
      border: 1px solid rgba(234, 88, 12, 0.15);
    }
    &.badge--ai {
      background: #f5f3ff;
      color: #7c3aed;
      border: 1px solid rgba(124, 58, 237, 0.15);
    }
    &.badge--default {
      background: #f1f5f9;
      color: #475569;
    }
  }

  // 2. 主体内容
  .task-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .task-tag-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .task-type-pill {
        font-size: 11px;
        padding: 1px 7px;
        border-radius: 4px;
        font-weight: 700;
        letter-spacing: 0.2px;

        &.pill--lesson {
          background: #e0f2fe;
          color: #0369a1;
        }
        &.pill--practice {
          background: #ffedd5;
          color: #c2410c;
        }
        &.pill--ai {
          background: #ede9fe;
          color: #6d28d9;
        }
        &.pill--default {
          background: #f1f5f9;
          color: #475569;
        }
      }

      .task-meta-time {
        display: inline-flex;
        align-items: center;
        gap: 3px;
        font-size: 11px;
        color: #94a3b8;

        .time-icon {
          font-size: 11.5px;
        }
      }
    }

    .task-title-text {
      font-size: 12.5px;
      font-weight: 600;
      color: #1e293b;
      line-height: 1.45;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;

      &.title--done {
        color: #94a3b8;
        text-decoration: line-through;
      }
    }
  }

  // 3. 操作按钮
  .task-action-box {
    flex-shrink: 0;

    .capsule-task-btn {
      border: none;
      cursor: pointer;
      padding: 5px 12px;
      height: 30px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
      white-space: nowrap;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.2s ease;

      .btn-icon {
        font-size: 12.5px;
      }

      &.btn--lesson {
        background: #1677ff;
        color: #ffffff;
        box-shadow: 0 2px 6px rgba(22, 119, 255, 0.2);

        &:hover {
          background: #0958d9;
          box-shadow: 0 4px 10px rgba(22, 119, 255, 0.3);
          transform: translateY(-1px);
        }
      }

      &.btn--practice {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
        }
      }

      &.btn--ai {
        background: linear-gradient(135deg, #8b5cf6, #6d28d9);
        color: #ffffff;
        box-shadow: 0 2px 6px rgba(109, 40, 217, 0.2);

        &:hover {
          background: linear-gradient(135deg, #7c3aed, #5b21b6);
          box-shadow: 0 4px 10px rgba(109, 40, 217, 0.3);
          transform: translateY(-1px);
        }
      }

      &.btn--default {
        background: #1677ff;
        color: #fff;
      }
    }

    .task-status-tag {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-size: 11.5px;
      padding: 3px 9px;
      border-radius: 9999px;
      font-weight: 600;

      &.tag--done {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid rgba(5, 150, 105, 0.15);
      }
    }
  }
}
</style>
