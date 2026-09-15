<template>
  <div class="trend-visual-card">
    <div class="visual-header">
      <div class="title-group">
        <div class="title-main">
          <span class="dot-indicator"></span>
          <span class="title">Token 消耗与调用频次趋势分析</span>
        </div>
        <span class="sub">每日自动归档统计 · 支持自适应缩放与双指标拟合折线图</span>
      </div>

      <div class="chart-actions">
        <div class="range-switch-pill">
          <button
            :class="['pill-btn', { active: trendDays === 7 }]"
            @click="$emit('change-trend-days', 7)"
          >
            近 7 天
          </button>
          <button
            :class="['pill-btn', { active: trendDays === 14 }]"
            @click="$emit('change-trend-days', 14)"
          >
            近 14 天
          </button>
          <button
            :class="['pill-btn', { active: trendDays === 30 }]"
            @click="$emit('change-trend-days', 30)"
          >
            近 30 天
          </button>
        </div>
      </div>
    </div>

    <!-- ECharts 折线图容器 -->
    <div v-loading="trendLoading" class="chart-container">
      <div ref="chartEl" class="echarts-box" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  trendDays: number;
  trendLoading: boolean;
  bindChartEl: (el: HTMLElement | null) => void;
}>();

defineEmits<{
  'change-trend-days': [days: number];
}>();

const chartEl = ref<HTMLElement | null>(null);

watch(
  chartEl,
  (el) => {
    props.bindChartEl(el);
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.trend-visual-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);

  .visual-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .title-group {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .title-main {
        display: flex;
        align-items: center;
        gap: 8px;

        .dot-indicator {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #2563EB;
        }

        .title {
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      .sub {
        font-size: 12px;
        color: #94A3B8;
        padding-left: 16px;
      }
    }

    .chart-actions {
      .range-switch-pill {
        display: flex;
        background: #F1F5F9;
        padding: 3px;
        border-radius: 9999px;
        gap: 2px;

        .pill-btn {
          border: none;
          background: transparent;
          padding: 5px 14px;
          font-size: 12px;
          font-weight: 600;
          color: #64748B;
          border-radius: 9999px;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            color: #0F172A;
          }

          &.active {
            background: #FFFFFF;
            color: #2563EB;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
          }
        }
      }
    }
  }

  .chart-container {
    width: 100%;
    height: 300px;

    .echarts-box {
      width: 100%;
      height: 100%;
    }
  }
}
</style>
