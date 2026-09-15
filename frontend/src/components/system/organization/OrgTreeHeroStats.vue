<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">{{ tenantStats.campusCount }}</span>
      <span class="stat-label">校区单元</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">{{ tenantStats.facultyCount }}</span>
      <span class="stat-label">学院/年级</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-warning">{{ tenantStats.classCount }}</span>
      <span class="stat-label">行政班级</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-info">{{ tenantStats.studentCount.toLocaleString() }}</span>
      <span class="stat-label">在册学生总数</span>
    </div>
    <div class="hero-stat-card" v-if="tenantStats.teacherCount > 0">
      <span class="stat-num text-purple">{{ tenantStats.teacherCount }}</span>
      <span class="stat-label">教职工总数</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { OrgStatsVO } from '@/types/system/tenant';

defineProps<{
  tenantStats: OrgStatsVO;
}>();
</script>

<style scoped lang="scss">
.hero-stats-row {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  flex-wrap: wrap;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 6px 20px;
    border-radius: 9999px;
    border: 1.5px solid rgba(22, 119, 255, 0.12);
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
    display: flex;
    align-items: center;
    gap: 10px;
    transition: all 0.25s ease;

    &:hover {
      background: #FFFFFF;
      border-color: #2563EB;
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(37, 99, 235, 0.15);
    }

    .stat-num {
      font-size: 19px;
      font-weight: 800;
      line-height: 1;

      &.text-primary { color: #2563EB; }
      &.text-success { color: #16A34A; }
      &.text-warning { color: #D97706; }
      &.text-info { color: #0284C7; }
      &.text-purple { color: #9333EA; }
    }

    .stat-label {
      font-size: 12.5px;
      font-weight: 600;
      color: #475569;
      margin-top: 0;
      white-space: nowrap;
    }
  }
}
</style>
