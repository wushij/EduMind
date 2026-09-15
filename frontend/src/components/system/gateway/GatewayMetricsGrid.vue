<template>
  <div class="telemetry-grid">
    <!-- 1. AI Token 消耗总览 -->
    <div class="telemetry-card">
      <div class="card-header">
        <div class="header-left">
          <div class="icon-box token"><el-icon><Coin /></el-icon></div>
          <div class="title-meta">
            <h4>AI Token 算力消耗</h4>
            <span class="sub">折合预估费用 ¥{{ (metrics?.estimatedCost ?? 0).toFixed(2) }}</span>
          </div>
        </div>
        <el-tag type="primary" size="small">算力开销</el-tag>
      </div>
      <div class="card-body">
        <div class="usage-stats">
          <span class="used-val font-mono">{{ formatTokens(metrics?.totalTokens) }}</span>
          <span class="unit-text">Tokens</span>
        </div>
        <div class="token-split-bar">
          <div
            class="split-prompt"
            :style="{ width: getPromptRatio + '%' }"
            :title="`输入 Tokens: ${(metrics?.promptTokens ?? 0).toLocaleString()}`"
          ></div>
          <div
            class="split-completion"
            :style="{ width: (100 - getPromptRatio) + '%' }"
            :title="`输出 Tokens: ${(metrics?.completionTokens ?? 0).toLocaleString()}`"
          ></div>
        </div>
        <div class="card-bottom-info">
          <span>输入: {{ formatTokens(metrics?.promptTokens) }} ({{ getPromptRatio }}%)</span>
          <span>输出: {{ formatTokens(metrics?.completionTokens) }} ({{ 100 - getPromptRatio }}%)</span>
        </div>
      </div>
    </div>

    <!-- 2. 网关请求吞吐与可用率 SLA -->
    <div class="telemetry-card">
      <div class="card-header">
        <div class="header-left">
          <div class="icon-box requests"><el-icon><TrendCharts /></el-icon></div>
          <div class="title-meta">
            <h4>网关总请求量</h4>
            <span class="sub">周期内全部模型路由转发</span>
          </div>
        </div>
        <el-tag :type="(metrics?.successRate ?? 100) >= 99 ? 'success' : 'warning'" size="small">
          {{ (metrics?.successRate ?? 100).toFixed(1) }}% 可用率
        </el-tag>
      </div>
      <div class="card-body">
        <div class="usage-stats">
          <span class="used-val font-mono">{{ (metrics?.totalRequests ?? 0).toLocaleString() }}</span>
          <span class="unit-text">次调用</span>
        </div>
        <el-progress
          :percentage="Math.min(100, Math.max(0, metrics?.successRate ?? 100))"
          :color="(metrics?.successRate ?? 100) >= 99 ? '#10B981' : '#F59E0B'"
          :stroke-width="8"
          style="margin: 14px 0;"
        />
        <div class="card-bottom-info">
          <span>降级触发: <b>{{ metrics?.fallbackCount ?? 0 }}</b> 次</span>
          <span class="est-text text-success">转发平稳</span>
        </div>
      </div>
    </div>

    <!-- 3. 平均响应耗时与性能 SLA -->
    <div class="telemetry-card">
      <div class="card-header">
        <div class="header-left">
          <div class="icon-box latency"><el-icon><Timer /></el-icon></div>
          <div class="title-meta">
            <h4>平均响应耗时</h4>
            <span class="sub">端到端全链路往返延迟</span>
          </div>
        </div>
        <el-tag
          :type="(metrics?.avgLatencyMs ?? 0) <= 1000 ? 'success' : ((metrics?.avgLatencyMs ?? 0) <= 2500 ? 'primary' : 'warning')"
          size="small"
        >
          {{ (metrics?.avgLatencyMs ?? 0) <= 1000 ? '极速响应' : ((metrics?.avgLatencyMs ?? 0) <= 2500 ? '良好' : '负荷偏高') }}
        </el-tag>
      </div>
      <div class="card-body">
        <div class="usage-stats">
          <span class="used-val font-mono">{{ metrics?.avgLatencyMs ?? 0 }}</span>
          <span class="unit-text">ms</span>
        </div>
        <div class="latency-percentiles">
          <div class="p-item">
            <span class="p-label">P95</span>
            <span class="p-val font-mono">{{ metrics?.p95LatencyMs ?? metrics?.avgLatencyMs ?? 0 }} ms</span>
          </div>
          <div class="p-divider"></div>
          <div class="p-item">
            <span class="p-label">P99</span>
            <span class="p-val font-mono">{{ metrics?.p99LatencyMs ?? metrics?.avgLatencyMs ?? 0 }} ms</span>
          </div>
        </div>
        <div class="card-bottom-info" style="margin-top: 10px;">
          <span>SLA 阈值标准：&lt; 3000 ms</span>
          <span class="est-text text-success">符合规范</span>
        </div>
      </div>
    </div>

    <!-- 4. 网关韧性治理与熔断拦截 -->
    <div class="telemetry-card">
      <div class="card-header">
        <div class="header-left">
          <div class="icon-box resilience"><el-icon><WarningFilled /></el-icon></div>
          <div class="title-meta">
            <h4>网关韧性与防御</h4>
            <span class="sub">熔断、限流与故障自动降级</span>
          </div>
        </div>
        <el-button type="primary" link size="small" @click="$emit('reset-circuit')">一键复位</el-button>
      </div>
      <div class="card-body">
        <div class="resilience-mini-grid">
          <div class="r-badge-item">
            <span class="r-title">降级次数</span>
            <span class="r-num font-mono">{{ metrics?.fallbackCount ?? 0 }}</span>
          </div>
          <div class="r-badge-item">
            <span class="r-title">重试次数</span>
            <span class="r-num font-mono">{{ metrics?.retryCount ?? 0 }}</span>
          </div>
          <div class="r-badge-item" :class="{ warn: (metrics?.circuitOpenCount ?? 0) > 0 }">
            <span class="r-title">熔断触发</span>
            <span class="r-num font-mono">{{ metrics?.circuitOpenCount ?? 0 }}</span>
          </div>
          <div class="r-badge-item" :class="{ warn: (metrics?.rateLimitedCount ?? 0) > 0 }">
            <span class="r-title">限流拦截</span>
            <span class="r-num font-mono">{{ metrics?.rateLimitedCount ?? 0 }}</span>
          </div>
        </div>
        <div class="card-bottom-info" style="margin-top: 10px;">
          <span>分布式漏桶限流 120 QPM</span>
          <span class="est-text text-primary">高可用保护</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Coin, TrendCharts, Timer, WarningFilled } from '@element-plus/icons-vue';
import type { GatewayMetricsVO } from '@/types/system/gateway';

defineProps<{
  metrics: GatewayMetricsVO | null;
  getPromptRatio: number;
  formatTokens: (value?: number) => string;
}>();

defineEmits<{
  'reset-circuit': [];
}>();
</script>

<style scoped lang="scss">
.telemetry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  width: 100%;

  @media (max-width: 1280px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .telemetry-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 18px 20px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
    display: flex;
    flex-direction: column;
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
      border-color: #CBD5E1;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;

      .header-left {
        display: flex;
        gap: 12px;

        .icon-box {
          width: 42px;
          height: 42px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 20px;

          &.token { background: #EFF6FF; color: #2563EB; }
          &.requests { background: #F0FDF4; color: #16A34A; }
          &.latency { background: #F0F9FF; color: #0284C7; }
          &.resilience { background: #FFFBEB; color: #F59E0B; }
        }

        .title-meta {
          h4 {
            font-size: 14.5px;
            font-weight: 700;
            color: #0F172A;
            margin: 0;
          }
          .sub {
            font-size: 11.5px;
            color: #94A3B8;
            margin-top: 3px;
            display: block;
          }
        }
      }
    }

    .card-body {
      display: flex;
      flex-direction: column;
      flex: 1;

      .usage-stats {
        display: flex;
        align-items: baseline;
        gap: 6px;

        .used-val {
          font-size: 24px;
          font-weight: 800;
          color: #0F172A;
        }

        .unit-text {
          font-size: 12px;
          color: #64748B;
          font-weight: 500;
        }
      }

      .token-split-bar {
        display: flex;
        height: 8px;
        border-radius: 4px;
        overflow: hidden;
        background: #E2E8F0;
        margin: 14px 0 10px;

        .split-prompt {
          background: #2563EB;
          height: 100%;
          transition: width 0.3s ease;
        }

        .split-completion {
          background: #06B6D4;
          height: 100%;
          transition: width 0.3s ease;
        }
      }

      .latency-percentiles {
        display: flex;
        align-items: center;
        background: #F8FAFC;
        border-radius: 8px;
        padding: 8px 12px;
        margin: 12px 0 4px;

        .p-item {
          flex: 1;
          display: flex;
          flex-direction: column;

          .p-label {
            font-size: 11px;
            color: #94A3B8;
          }
          .p-val {
            font-size: 13px;
            font-weight: 700;
            color: #1E293B;
          }
        }

        .p-divider {
          width: 1px;
          height: 24px;
          background: #E2E8F0;
          margin: 0 10px;
        }
      }

      .resilience-mini-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 8px;
        margin: 8px 0 4px;

        .r-badge-item {
          background: #F8FAFC;
          border-radius: 8px;
          padding: 6px 10px;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .r-title {
            font-size: 11.5px;
            color: #64748B;
          }
          .r-num {
            font-size: 13.5px;
            font-weight: 700;
            color: #0F172A;
          }

          &.warn .r-num {
            color: #D97706;
          }
        }
      }

      .card-bottom-info {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #64748B;
        margin-top: auto;

        .est-text {
          font-weight: 600;
          &.text-success { color: #16A34A; }
          &.text-primary { color: #2563EB; }
        }
      }
    }
  }
}
</style>
