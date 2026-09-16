<template>
  <div class="metric-cards-grid">
    <!-- 维度 1: 今日 (1日) -->
    <div class="metric-card metric-card--blue">
      <div class="metric-card-inner">
        <div class="card-icon icon-blue">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">今日已消耗 Token (1日)</span>
          <span class="val text-blue">
            {{ formatNumber(usage.todayTokensUsed) }}
            <span class="unit">toks</span>
          </span>
          <span class="sub">
            今日调用 {{ todayCalls || 0 }} 次 · 限额 {{ formatNumber(usage.dailyTokenLimit) }} toks/日
          </span>
        </div>
      </div>
    </div>

    <!-- 维度 2: 近 7 天 (一周) -->
    <div class="metric-card metric-card--teal">
      <div class="metric-card-inner">
        <div class="card-icon icon-teal">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">近 7 天已消耗 (一周)</span>
          <span class="val text-teal">
            {{ formatNumber(effectiveWeekTokens) }}
            <span class="unit">toks</span>
          </span>
          <span class="sub">
            近 7 天调用 {{ weekCalls || 0 }} 次 · 日均 {{ formatNumber(Math.round(effectiveWeekTokens / 7)) }} toks
          </span>
        </div>
      </div>
    </div>

    <!-- 维度 3: 近 30 天 (一个月) -->
    <div class="metric-card metric-card--purple">
      <div class="metric-card-inner">
        <div class="card-icon icon-purple">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">近 30 天已消耗 (一个月)</span>
          <span class="val text-purple">
            {{ formatNumber(effectiveMonthTokens) }}
            <span class="unit">toks</span>
          </span>
          <span class="sub">
            近 30 天调用 {{ monthCalls || 0 }} 次 · 预估 ¥{{ (monthCostRMB || 0).toFixed(2) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 维度 4: 历史全部累计 (总的) -->
    <div class="metric-card metric-card--amber">
      <div class="metric-card-inner">
        <div class="card-icon icon-amber">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">历史全部累计 (总计)</span>
          <span class="val text-amber">
            {{ formatNumber(effectiveTotalTokens) }}
            <span class="unit">toks</span>
          </span>
          <span class="sub">
            共调用 {{ totalCalls || 0 }} 次 · 教学平台全额资助
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ChatDotRound, Coin, DataAnalysis, TrendCharts } from '@element-plus/icons-vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

const props = withDefaults(
  defineProps<{
    usage: PersonalAiUsageVO;
    formatNumber: (value: number) => string;
    weekTokensUsed?: number;
    monthTokensUsed?: number;
    totalTokensUsed?: number;
    todayCalls?: number;
    weekCalls?: number;
    monthCalls?: number;
    totalCalls?: number;
    monthCostRMB?: number;
    totalCostRMB?: number;
  }>(),
  {
    weekTokensUsed: 0,
    monthTokensUsed: 0,
    totalTokensUsed: 0,
    todayCalls: 0,
    weekCalls: 0,
    monthCalls: 0,
    totalCalls: 0,
    monthCostRMB: 0,
    totalCostRMB: 0
  }
);

const effectiveWeekTokens = computed(() => {
  if (props.weekTokensUsed > 0) return props.weekTokensUsed;
  return props.usage.todayTokensUsed;
});

const effectiveMonthTokens = computed(() => {
  if (props.monthTokensUsed > 0) return props.monthTokensUsed;
  return effectiveWeekTokens.value;
});

const effectiveTotalTokens = computed(() => {
  if (props.totalTokensUsed > 0) return props.totalTokensUsed;
  return effectiveMonthTokens.value;
});
</script>

<style scoped lang="scss">
.metric-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .metric-card {
    border-radius: 22px;
    border: 1px solid #E2E8F0;
    overflow: hidden;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 28px rgba(15, 23, 42, 0.07);
    }

    &--blue { background: linear-gradient(160deg, #FFFFFF 0%, #EFF6FF 100%); }
    &--teal { background: linear-gradient(160deg, #FFFFFF 0%, #F0FDFA 100%); }
    &--purple { background: linear-gradient(160deg, #FFFFFF 0%, #FAF5FF 100%); }
    &--amber { background: linear-gradient(160deg, #FFFFFF 0%, #FFFBEB 100%); }

    .metric-card-inner {
      padding: 22px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .card-icon {
      width: 48px;
      height: 48px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      flex-shrink: 0;

      &.icon-blue {
        background: #FFFFFF;
        color: #2563EB;
        border: 1px solid #BFDBFE;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
      }

      &.icon-teal {
        background: #FFFFFF;
        color: #0D9488;
        border: 1px solid #99F6E4;
        box-shadow: 0 4px 12px rgba(13, 148, 136, 0.12);
      }

      &.icon-purple {
        background: #FFFFFF;
        color: #9333EA;
        border: 1px solid #E9D5FF;
        box-shadow: 0 4px 12px rgba(147, 51, 234, 0.12);
      }

      &.icon-amber {
        background: #FFFFFF;
        color: #D97706;
        border: 1px solid #FDE68A;
        box-shadow: 0 4px 12px rgba(217, 119, 6, 0.12);
      }
    }

    .card-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;

      .label {
        font-size: 12px;
        color: #64748B;
        font-weight: 500;
        white-space: nowrap;
      }

      .val {
        font-size: 21px;
        font-weight: 800;
        color: #0F172A;
        letter-spacing: -0.02em;
        font-variant-numeric: tabular-nums;

        &.text-blue { color: #2563EB; }
        &.text-teal { color: #0D9488; }
        &.text-purple { color: #9333EA; }
        &.text-amber { color: #D97706; }

        .unit {
          font-size: 12px;
          font-weight: 500;
          color: #94A3B8;
          margin-left: 2px;
        }
      }

      .sub {
        font-size: 11.5px;
        color: #94A3B8;
      }
    }
  }
}

// 鈹€鈹€ 璁板綍闈㈡澘 鈹€鈹€
</style>
