<template>
  <div class="stats-row">
    <div class="stat-card">
      <div class="stat-icon-wrapper stat-blue">
        <el-icon :size="22"><ChatDotSquare /></el-icon>
      </div>
      <div class="stat-info">
        <span class="stat-label">历史广播任务</span>
        <div class="stat-number-wrap">
          <span class="stat-value">{{ stats.totalBroadcasts }}</span>
          <span class="stat-unit">批次</span>
        </div>
        <span class="stat-desc">全校累计下发记录</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon-wrapper stat-purple">
        <el-icon :size="22"><UserFilled /></el-icon>
      </div>
      <div class="stat-info">
        <span class="stat-label">累计受众触达</span>
        <div class="stat-number-wrap">
          <span class="stat-value">{{ stats.totalReach }}</span>
          <span class="stat-unit">人次</span>
        </div>
        <span class="stat-desc">系统覆盖接收人次</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon-wrapper stat-emerald">
        <el-icon :size="22"><CircleCheckFilled /></el-icon>
      </div>
      <div class="stat-info">
        <span class="stat-label">累计已读确认</span>
        <div class="stat-number-wrap">
          <span class="stat-value">{{ stats.totalRead }}</span>
          <span class="stat-unit">人次</span>
        </div>
        <span class="stat-desc">师生已阅回执统计</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon-wrapper stat-amber">
        <el-icon :size="22"><DataAnalysis /></el-icon>
      </div>
      <div class="stat-info">
        <span class="stat-label">平均触达已读率</span>
        <div class="stat-number-wrap">
          <span class="stat-value">{{ Math.round(stats.avgReadRate || 0) }}%</span>
        </div>
        <div class="stat-progress-box">
          <el-progress
            :percentage="Math.min(Math.round(stats.avgReadRate || 0), 100)"
            :stroke-width="5"
            :show-text="false"
            color="#f59e0b"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotSquare, UserFilled, CircleCheckFilled, DataAnalysis } from '@element-plus/icons-vue';
import type { BroadcastStatsVO } from '@/types/notification/broadcast';

defineProps<{
  stats: BroadcastStatsVO;
}>();
</script>

<style scoped lang="scss">
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px -3px rgba(0, 0, 0, 0.06);
    border-color: #cbd5e1;
  }
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.stat-blue {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #2563eb;
  }
  &.stat-purple {
    background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
    color: #7c3aed;
  }
  &.stat-emerald {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    color: #059669;
  }
  &.stat-amber {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    color: #d97706;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.stat-number-wrap {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
  font-feature-settings: 'tnum';
}

.stat-unit {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.stat-desc {
  font-size: 11px;
  color: #94a3b8;
}

.stat-progress-box {
  margin-top: 4px;
}

@media (max-width: 1080px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
