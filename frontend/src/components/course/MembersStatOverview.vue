<template>
  <div class="members-stat-overview">
    <div class="stat-capsule-card">
      <div class="stat-icon-circle stat-icon-circle--blue">
        <el-icon><User /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ totalCount }}</span>
        <span class="stat-desc">选课班级总人数</span>
      </div>
    </div>

    <div class="stat-capsule-card">
      <div class="stat-icon-circle stat-icon-circle--emerald">
        <el-icon><School /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ studentCount }}</span>
        <span class="stat-desc">在读选课学生</span>
      </div>
    </div>

    <div class="stat-capsule-card">
      <div class="stat-icon-circle stat-icon-circle--purple">
        <el-icon><Avatar /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ staffCount }}</span>
        <span class="stat-desc">主讲与助教团队</span>
      </div>
    </div>

    <div class="stat-capsule-card">
      <div class="stat-icon-circle stat-icon-circle--amber">
        <el-icon><TrendCharts /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ averageProgress }}%</span>
        <span class="stat-desc">班级平均章节达成度</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, School, Avatar, TrendCharts } from '@element-plus/icons-vue';

defineProps<{
  totalCount: number;
  studentCount: number;
  staffCount: number;
  averageProgress: number;
}>();
</script>

<style scoped lang="scss">
.members-stat-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;

  .stat-capsule-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #EBF1F7;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(30, 80, 150, 0.08);
    }

    .stat-icon-circle {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;

      &--blue { background: #EFF6FF; color: #1677FF; }
      &--emerald { background: #ECFDF5; color: #10B981; }
      &--purple { background: #F5F3FF; color: #7C3AED; }
      &--amber { background: #FEF3C7; color: #D97706; }
    }

    .stat-meta {
      display: flex;
      flex-direction: column;

      .stat-num {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.2;
      }

      .stat-desc {
        font-size: 12px;
        color: #64748B;
        margin-top: 2px;
      }
    }
  }
}
</style>
