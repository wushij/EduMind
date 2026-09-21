<template>
  <div class="learning-path-week-grid">
    <div v-if="!weeks.length" class="empty-hint">
      <el-empty description="暂无学习路径，请先完成课程测评积累掌握度数据" />
    </div>
    <div v-else class="adaptive-weeks-grid">
      <div
        v-for="week in weeks"
        :key="week.weekNo"
        :id="weekAnchorId(week.weekNo)"
        class="week-card"
      >
        <!-- 顶部装饰色条 -->
        <div class="week-top-accent"></div>

        <!-- 1. 周次与掌握度元信息行 -->
        <div class="week-meta-bar">
          <div class="week-badge-pill">
            <el-icon class="badge-icon"><Calendar /></el-icon>
            <span>第 {{ week.weekNo }} 周</span>
          </div>

          <div v-if="week.masteryPercent != null" class="week-mastery-capsule" :class="masteryLevelClass(week.masteryPercent)">
            <span class="mastery-dot"></span>
            <span>掌握度 {{ Math.round(week.masteryPercent) }}%</span>
          </div>
        </div>

        <!-- 2. 章节大纲核心主题（独立一行，充足空间，杜绝文字截断） -->
        <div class="week-title-box">
          <h4 class="week-theme-title" :title="week.theme">
            {{ week.theme }}
          </h4>
        </div>

        <!-- 3. AI 排期与导学建议提示框 -->
        <div v-if="week.focusReason" class="week-reason-callout">
          <el-icon class="reason-icon"><InfoFilled /></el-icon>
          <span class="reason-text">{{ week.focusReason }}</span>
        </div>

        <!-- 4. 周任务卡片流 -->
        <div class="week-tasks-list">
          <LearningPathTaskRow
            v-for="(task, idx) in week.tasks"
            :key="task.id || `${week.weekNo}-${idx}`"
            :task="task"
            @execute="emit('execute-task', $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Calendar, InfoFilled } from '@element-plus/icons-vue';
import LearningPathTaskRow from '@/components/learning/path/LearningPathTaskRow.vue';
import type { LearningPathTask, LearningPathWeek } from '@/types/learning/learning-path';
import { weekAnchorId } from '@/utils/learning/learning-path';

defineProps<{
  weeks: LearningPathWeek[];
}>();

const emit = defineEmits<{
  (e: 'execute-task', task: LearningPathTask): void;
}>();

function masteryLevelClass(percent?: number) {
  if (percent == null || percent === 0) return 'level--zero';
  if (percent < 60) return 'level--low';
  if (percent < 85) return 'level--mid';
  return 'level--high';
}
</script>

<style scoped lang="scss">
.learning-path-week-grid {
  width: 100%;

  .adaptive-weeks-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 18px;
    align-items: stretch;
  }

  .week-card {
    position: relative;
    background: #ffffff;
    border-radius: 18px;
    padding: 20px 20px 22px;
    border: 1px solid #ebf1f7;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow: hidden;
    transition: all 0.25s ease;

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
    }

    .week-top-accent {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%);
    }
  }

  // 1. 顶部元信息栏
  .week-meta-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding-top: 2px;

    .week-badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      padding: 3px 10px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #ffffff;
      font-weight: 700;
      letter-spacing: 0.2px;
      box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);

      .badge-icon {
        font-size: 13px;
      }
    }

    .week-mastery-capsule {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 11.5px;
      padding: 3px 9px;
      border-radius: 9999px;
      font-weight: 600;

      .mastery-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: currentColor;
      }

      &.level--zero {
        background: #f8fafc;
        color: #64748b;
        border: 1px solid #e2e8f0;
      }

      &.level--low {
        background: #fef2f2;
        color: #ef4444;
        border: 1px solid rgba(239, 68, 68, 0.18);
      }

      &.level--mid {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid rgba(217, 119, 6, 0.18);
      }

      &.level--high {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid rgba(5, 150, 105, 0.18);
      }
    }
  }

  // 2. 主题标题独占一行，绝不重叠截断
  .week-title-box {
    margin: 2px 0 0 0;

    .week-theme-title {
      margin: 0;
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      line-height: 1.45;
      letter-spacing: -0.1px;
      word-break: break-word;
    }
  }

  // 3. 导学建议提示框
  .week-reason-callout {
    display: flex;
    align-items: flex-start;
    gap: 7px;
    background: #f8fafc;
    border: 1px solid #eef2f6;
    border-radius: 10px;
    padding: 8px 10px;

    .reason-icon {
      font-size: 14px;
      color: #3b82f6;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .reason-text {
      font-size: 11.5px;
      color: #64748b;
      line-height: 1.5;
    }
  }

  // 4. 任务卡片流
  .week-tasks-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 4px;
  }
}
</style>
