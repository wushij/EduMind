<template>
  <div class="metric-cards-grid">
    <div class="metric-card metric-card--blue">
      <div class="metric-card-inner">
        <div class="card-icon icon-blue">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">今日已消耗 Token</span>
          <span class="val text-blue">
            {{ formatNumber(usage.todayTokensUsed) }}
            <span class="unit">toks</span>
          </span>
          <span class="sub">限额 {{ formatNumber(usage.dailyTokenLimit) }} toks / 日</span>
        </div>
      </div>
    </div>

    <div class="metric-card metric-card--purple">
      <div class="metric-card-inner">
        <div class="card-icon icon-purple">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">累计智能问答与出题</span>
          <span class="val text-purple">
            {{ formatNumber(usage.totalQaAndGenerateCalls) }}
            <span class="unit">次</span>
          </span>
          <span class="sub">平均耗时 {{ usage.avgLatencyMs }}ms</span>
        </div>
      </div>
    </div>

    <div class="metric-card metric-card--amber">
      <div class="metric-card-inner">
        <div class="card-icon icon-amber">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">本学期预估成本</span>
          <span class="val text-amber">¥ {{ usage.semesterEstimatedCostRMB.toFixed(2) }}</span>
          <span class="sub sub-pill">由教学平台全额资助</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Coin, DataAnalysis } from '@element-plus/icons-vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

defineProps<{
  usage: PersonalAiUsageVO;
  formatNumber: (value: number) => string;
}>();
</script>

<style scoped lang="scss">
.metric-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  @media (max-width: 900px) {
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
    &--purple { background: linear-gradient(160deg, #FFFFFF 0%, #FAF5FF 100%); }
    &--amber { background: linear-gradient(160deg, #FFFFFF 0%, #FFFBEB 100%); }

    .metric-card-inner {
      padding: 22px 24px;
      display: flex;
      align-items: center;
      gap: 18px;
    }

    .card-icon {
      width: 52px;
      height: 52px;
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;

      &.icon-blue {
        background: #FFFFFF;
        color: #2563EB;
        border: 1px solid #BFDBFE;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
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
      }

      .val {
        font-size: 22px;
        font-weight: 800;
        color: #0F172A;
        letter-spacing: -0.02em;
        font-variant-numeric: tabular-nums;

        &.text-blue { color: #2563EB; }
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

      .sub-pill {
        display: inline-block;
        width: fit-content;
        padding: 2px 10px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.7);
        border: 1px solid #FDE68A;
        color: #B45309;
        font-weight: 500;
      }
    }
  }
}

// 鈹€鈹€ 璁板綍闈㈡澘 鈹€鈹€
</style>
