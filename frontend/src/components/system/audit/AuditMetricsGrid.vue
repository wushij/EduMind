<template>
  <div class="metrics-grid">
    <div class="stat-card">
      <div class="card-icon icon-blue">
        <el-icon><DataLine /></el-icon>
      </div>
      <div class="card-details">
        <span class="label">累计 AI 调用总次数</span>
        <span class="val">{{ summary.totalCalls.toLocaleString() }} <span class="unit">次</span></span>
        <span class="hint text-green">调用成功率 {{ summary.successRate }}%</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="card-icon icon-purple">
        <el-icon><Cpu /></el-icon>
      </div>
      <div class="card-details">
        <span class="label">Token 消耗总规模</span>
        <span class="val text-purple">{{ (summary.totalTokens / 10000).toFixed(2) }} <span class="unit">万 toks</span></span>
        <span class="hint">约 {{ (summary.totalTokens / 1000000).toFixed(3) }} M Tokens</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="card-icon icon-amber">
        <el-icon><Coin /></el-icon>
      </div>
      <div class="card-details">
        <span class="label">预估模型消耗总成本</span>
        <span class="val text-amber">¥ {{ summary.totalCostRMB.toFixed(2) }}</span>
        <span class="hint">按各厂商 1K Tokens 阶梯计费</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="card-icon icon-emerald">
        <el-icon><Timer /></el-icon>
      </div>
      <div class="card-details">
        <span class="label">平均响应耗时 (Avg Latency)</span>
        <span class="val text-emerald">{{ summary.avgLatencyMs }} <span class="unit">ms</span></span>
        <span class="hint">流式首包平均 ~180ms</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataLine, Cpu, Coin, Timer } from '@element-plus/icons-vue';
import type { AuditSummaryVO } from '@/types/system/audit';

defineProps<{
  summary: AuditSummaryVO;
}>();
</script>

<style scoped lang="scss">
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 18px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.06);
      border-color: #CBD5E1;
    }

    .card-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;

      &.icon-blue { background: #EFF6FF; color: #2563EB; }
      &.icon-purple { background: #FAF5FF; color: #9333EA; }
      &.icon-amber { background: #FFFBEB; color: #D97706; }
      &.icon-emerald { background: #ECFDF5; color: #059669; }
    }

    .card-details {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .label {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
      }

      .val {
        font-size: 22px;
        font-weight: 700;
        color: #0F172A;
        letter-spacing: -0.5px;

        &.text-purple { color: #9333EA; }
        &.text-amber { color: #D97706; }
        &.text-emerald { color: #059669; }

        .unit {
          font-size: 12px;
          font-weight: 500;
          color: #94A3B8;
          margin-left: 2px;
        }
      }

      .hint {
        font-size: 11.5px;
        color: #94A3B8;

        &.text-green {
          color: #16A34A;
          font-weight: 600;
        }
      }
    }
  }
}
</style>
