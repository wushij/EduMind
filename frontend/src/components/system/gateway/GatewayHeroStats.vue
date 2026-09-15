<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">{{ formatTokens(metrics?.totalTokens) }}</span>
      <span class="stat-label">本周期 Token 算力消耗</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">{{ metrics?.successRate ? metrics.successRate.toFixed(1) : '100.0' }}%</span>
      <span class="stat-label">网关可用率 SLA</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-info">{{ metrics?.p95LatencyMs ?? metrics?.avgLatencyMs ?? 0 }} ms</span>
      <span class="stat-label">P95 响应延迟</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-purple">{{ metrics?.byProvider?.length ?? 0 }} 个模型</span>
      <span class="stat-label">活跃服务商</span>
    </div>
    <div class="hero-stat-card">
      <span
        class="stat-num"
        :class="(metrics?.circuitOpenCount ?? 0) > 0 ? 'text-danger' : ((metrics?.fallbackCount ?? 0) > 0 ? 'text-warning' : 'text-success')"
      >
        {{ (metrics?.circuitOpenCount ?? 0) > 0 ? '熔断防护中' : ((metrics?.fallbackCount ?? 0) > 0 ? '降级运行' : '全链路健康') }}
      </span>
      <span class="stat-label">网关韧性状态</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { GatewayMetricsVO } from '@/types/system/gateway';

defineProps<{
  metrics: GatewayMetricsVO | null;
  formatTokens: (value?: number) => string;
}>();
</script>

<style scoped lang="scss">
.hero-stats-row {
  display: flex;
  gap: 10px;
  margin-top: 4px;
  flex-wrap: nowrap;
  width: 100%;
  overflow-x: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }

  .hero-stat-card {
    flex: 1 1 0;
    min-width: 0;
    justify-content: center;
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(8px);
    padding: 6px 12px;
    border-radius: 9999px;
    border: 1.5px solid rgba(22, 119, 255, 0.12);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    display: flex;
    align-items: center;
    gap: 8px;
    transition: all 0.25s ease;
    white-space: nowrap;

    &:hover {
      background: #FFFFFF;
      border-color: #1677FF;
      transform: translateY(-2px);
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.12);
    }

    .stat-num {
      font-size: 16px;
      font-weight: 800;
      line-height: 1;
      flex-shrink: 0;

      &.text-primary { color: #2563EB; }
      &.text-success { color: #16A34A; }
      &.text-info { color: #0284C7; }
      &.text-purple { color: #8B5CF6; }
      &.text-warning { color: #D97706; }
      &.text-danger { color: #DC2626; }
    }

    .stat-label {
      font-size: 11.5px;
      font-weight: 500;
      color: #475569;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>
