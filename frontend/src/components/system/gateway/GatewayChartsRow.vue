<template>
  <div class="charts-row">
    <div class="chart-card-box span-13">
      <div class="card-title-row">
        <div class="title-group">
          <h3>网关请求吞吐与 Token 算力走势</h3>
          <span class="subtitle">双轴联动呈现请求波峰与 Token 算力消耗量</span>
        </div>
      </div>
      <GatewayTrendChart :data="metrics?.timeSeriesTrend ?? []" height="300px" />
    </div>

    <div class="chart-card-box span-11">
      <div class="card-title-row">
        <div class="title-group">
          <h3>资源与场景分布</h3>
          <span class="subtitle">多服务商权重与业务教学场景占比</span>
        </div>
      </div>
      <GatewayDistributionChart
        :providers="metrics?.byProvider ?? []"
        :scenes="metrics?.byScene ?? []"
        height="300px"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import GatewayTrendChart from '@/components/system/gateway/GatewayTrendChart.vue';
import GatewayDistributionChart from '@/components/system/gateway/GatewayDistributionChart.vue';
import type { GatewayMetricsVO } from '@/types/system/gateway';

defineProps<{
  metrics: GatewayMetricsVO | null;
}>();
</script>

<style scoped lang="scss">
.charts-row {
  display: flex;
  gap: 18px;
  width: 100%;

  @media (max-width: 1080px) {
    flex-direction: column;
  }

  .chart-card-box {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 20px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    &.span-13 { flex: 13; min-width: 0; }
    &.span-11 { flex: 11; min-width: 0; }

    .card-title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 14px;

      .title-group {
        h3 {
          font-size: 15.5px;
          font-weight: 700;
          color: #0F172A;
          margin: 0;
        }
        .subtitle {
          font-size: 12px;
          color: #64748B;
          margin-top: 3px;
          display: block;
        }
      }

      .chart-legend-badge {
        font-size: 11px;
        color: #94A3B8;
        background: #F8FAFC;
        padding: 3px 8px;
        border-radius: 6px;
      }
    }
  }
}
</style>
