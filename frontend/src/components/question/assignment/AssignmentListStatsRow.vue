<template>
  <div class="assignment-stats-grid">
    <div class="stat-card">
      <div class="stat-icon stat-icon--blue">
        <el-icon><EditPen /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-val">{{ pageReady ? stats?.activeAssignmentCount ?? 0 : '--' }}</span>
        <span class="stat-title">进行中作业</span>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon stat-icon--amber">
        <el-icon><Timer /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-val">{{ pageReady ? stats?.pendingGradingCount ?? 0 : '--' }}</span>
        <span class="stat-title">待评阅答卷</span>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon stat-icon--green">
        <el-icon><Cpu /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-val">{{ pageReady ? stats?.aiGradedCount ?? 0 : '--' }}</span>
        <span class="stat-title">已批改答卷</span>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon stat-icon--purple">
        <el-icon><DataAnalysis /></el-icon>
      </div>
      <div class="stat-meta">
        <span class="stat-val">{{ pageReady ? avgSubmissionRateLabel : '--' }}</span>
        <span class="stat-title">平均提交率</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { EditPen, Timer, Cpu, DataAnalysis } from '@element-plus/icons-vue';
import type { AssignmentStats } from '@/types/question/assignment';

defineProps<{
  pageReady: boolean;
  stats: AssignmentStats | null;
  avgSubmissionRateLabel: string;
}>();
</script>

<style scoped lang="scss">
.assignment-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 4px 14px rgba(30, 80, 150, 0.04);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;

  &--blue {
    background: #eff6ff;
    color: #2563eb;
  }
  &--amber {
    background: #fffbeb;
    color: #d97706;
  }
  &--green {
    background: #ecfdf5;
    color: #059669;
  }
  &--purple {
    background: #f5f3ff;
    color: #7c3aed;
  }
}

.stat-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-val {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-title {
  font-size: 13px;
  color: #64748b;
}
</style>
