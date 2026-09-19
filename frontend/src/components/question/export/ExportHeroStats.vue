<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">{{ totalQuestionsCount }}</span>
      <span class="stat-label">试卷题量</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">{{ totalScoreLabel }}</span>
      <span class="stat-label">试卷满分</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-warning">{{ durationLabel }}</span>
      <span class="stat-label">考试时长</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-info">{{ lastExportLabel }}</span>
      <span class="stat-label">最近导出</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ExamPaper } from '@/types/question/exam';
import type { ExportHistoryItem } from '@/composables/question/useExport';

const props = defineProps<{
  examPaper: ExamPaper | null;
  totalQuestionsCount: number;
  lastExportTask: ExportHistoryItem | null;
}>();

const totalScoreLabel = computed(() => {
  if (!props.examPaper?.totalScore) return '—';
  return `${props.examPaper.totalScore} 分`;
});

const durationLabel = computed(() => {
  if (!props.examPaper?.durationMinutes) return '—';
  return `${props.examPaper.durationMinutes} 分钟`;
});

const lastExportLabel = computed(() => {
  const task = props.lastExportTask;
  if (!task) return '—';
  if (task.status === 'SUCCESS') return '已完成';
  if (task.status === 'FAILED') return '失败';
  if (task.status === 'PROCESSING') return '排版中';
  return '排队中';
});
</script>

<style scoped lang="scss">
.hero-stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    padding: 10px 16px;
    border-radius: 12px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 88px;

    .stat-num {
      font-size: 18px;
      font-weight: 700;
      line-height: 1.2;

      &.text-primary { color: #2563eb; }
      &.text-success { color: #16a34a; }
      &.text-warning { color: #d97706; }
      &.text-info { color: #0284c7; }
    }

    .stat-label {
      font-size: 11px;
      color: #64748b;
      margin-top: 4px;
    }
  }
}
</style>
