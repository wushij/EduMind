<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">{{ overallProgressPercent }}%</span>
      <span class="stat-label">路径完成率</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-danger">{{ weakPointCount }}</span>
      <span class="stat-label">薄弱考点</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-warning">{{ graphGapCount }}</span>
      <span class="stat-label">图谱待补缺口</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">{{ estimatedTotalMinutes }}</span>
      <span class="stat-label">预计学时(分钟)</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  overallProgressPercent: number;
  weakPointCount: number;
  graphGapCount: number;
  estimatedTotalMinutes: number;
}>();
</script>

<style scoped lang="scss">
.hero-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  width: 100%;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.85);
    border-radius: 14px;
    padding: 12px 16px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    display: flex;
    flex-direction: column;
    gap: 4px;

    .stat-num {
      font-size: 22px;
      font-weight: 800;
      line-height: 1.1;

      &.text-primary {
        color: #1677ff;
      }
      &.text-danger {
        color: #ef4444;
      }
      &.text-warning {
        color: #f59e0b;
      }
      &.text-success {
        color: #10b981;
      }
    }

    .stat-label {
      font-size: 12px;
      color: #64748b;
      font-weight: 600;
    }
  }
}

@media (max-width: 900px) {
  .hero-stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
