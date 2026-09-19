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
        <div class="week-header">
          <span class="week-badge">第 {{ week.weekNo }} 周</span>
          <span class="week-theme">{{ week.theme }}</span>
          <span v-if="week.masteryPercent != null" class="week-mastery-pill">
            掌握度 {{ Math.round(week.masteryPercent) }}%
          </span>
        </div>
        <p v-if="week.focusReason" class="week-focus-reason">{{ week.focusReason }}</p>
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
import LearningPathTaskRow from '@/components/learning/path/LearningPathTaskRow.vue';
import type { LearningPathTask, LearningPathWeek } from '@/types/learning/learning-path';
import { weekAnchorId } from '@/utils/learning/learning-path';

defineProps<{
  weeks: LearningPathWeek[];
}>();

const emit = defineEmits<{
  (e: 'execute-task', task: LearningPathTask): void;
}>();

</script>

<style scoped lang="scss">
.learning-path-week-grid {
  .adaptive-weeks-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .week-card {
    background: #f8fafc;
    border-radius: 16px;
    padding: 16px;
    border: 1px solid #e2e8f0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .week-header {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .week-badge {
    font-size: 12px;
    padding: 2px 10px;
    border-radius: 9999px;
    background: #10b981;
    color: #fff;
    font-weight: 700;
  }

  .week-theme {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
    flex: 1;
  }

  .week-mastery-pill {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 9999px;
    background: #fef3c7;
    color: #b45309;
    font-weight: 600;
  }

  .week-focus-reason {
    margin: 0;
    font-size: 12px;
    color: #64748b;
    line-height: 1.5;
  }

  .week-tasks-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 1024px) {
  .learning-path-week-grid .adaptive-weeks-grid {
    grid-template-columns: 1fr;
  }
}
</style>
