<template>
  <div class="metrics-hero-grid">
    <div class="metric-capsule-card">
      <div class="capsule-icon icon-blue">
        <el-icon><DataLine /></el-icon>
      </div>
      <div class="capsule-content">
        <span class="capsule-label">累计操作总数</span>
        <span class="capsule-val">{{ stats.totalCount.toLocaleString() }} <span class="unit">条</span></span>
        <span class="capsule-hint text-blue">系统操作全生命周期记录</span>
      </div>
    </div>

    <div class="metric-capsule-card">
      <div class="capsule-icon icon-purple">
        <el-icon><Clock /></el-icon>
      </div>
      <div class="capsule-content">
        <span class="capsule-label">今日操作记录</span>
        <span class="capsule-val text-purple">{{ stats.todayCount.toLocaleString() }} <span class="unit">次</span></span>
        <span class="capsule-hint">实时业务事件流审计</span>
      </div>
    </div>

    <div class="metric-capsule-card">
      <div class="capsule-icon icon-emerald">
        <el-icon><CircleCheck /></el-icon>
      </div>
      <div class="capsule-content">
        <span class="capsule-label">操作执行成功率</span>
        <span class="capsule-val text-emerald">{{ stats.successRate }} <span class="unit">%</span></span>
        <span class="capsule-hint" :class="stats.errorCount > 0 ? 'text-danger' : 'text-emerald'">
          {{ stats.errorCount > 0 ? `当前发现 ${stats.errorCount} 次异常拦截` : '全链路调用健康稳定' }}
        </span>
      </div>
    </div>

    <div class="metric-capsule-card">
      <div class="capsule-icon icon-amber">
        <el-icon><Timer /></el-icon>
      </div>
      <div class="capsule-content">
        <span class="capsule-label">平均响应耗时</span>
        <span class="capsule-val text-amber">{{ stats.avgCostTime }} <span class="unit">ms</span></span>
        <span class="capsule-hint">接口端到端执行效率监控</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataLine, Clock, CircleCheck, Timer } from '@element-plus/icons-vue';
import type { OperLogStatsVO } from '@/types/system/oper-log';

defineProps<{
  stats: OperLogStatsVO;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.metrics-hero-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .metric-capsule-card {
    background: #ffffff;
    border-radius: $border-radius-xl;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    box-shadow: $shadow-sm;
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-hover;
      border-color: rgba(22, 119, 255, 0.2);
    }

    .capsule-icon {
      width: 52px;
      height: 52px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;

      &.icon-blue {
        background: #eff6ff;
        color: #2563eb;
      }
      &.icon-purple {
        background: #faf5ff;
        color: #9333ea;
      }
      &.icon-emerald {
        background: #ecfdf5;
        color: #059669;
      }
      &.icon-amber {
        background: #fffbeb;
        color: #d97706;
      }
    }

    .capsule-content {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .capsule-label {
        font-size: 13px;
        color: #64748b;
        font-weight: 500;
      }

      .capsule-val {
        font-size: 24px;
        font-weight: 700;
        color: #0f172a;
        line-height: 1.2;

        .unit {
          font-size: 13px;
          font-weight: 500;
          color: #94a3b8;
          margin-left: 2px;
        }

        &.text-purple {
          color: #9333ea;
        }
        &.text-emerald {
          color: #059669;
        }
        &.text-amber {
          color: #d97706;
        }
      }

      .capsule-hint {
        font-size: 12px;
        color: #94a3b8;
        margin-top: 2px;

        &.text-blue {
          color: #2563eb;
        }
        &.text-emerald {
          color: #059669;
        }
        &.text-danger {
          color: #dc2626;
          font-weight: 600;
        }
      }
    }
  }
}
</style>
