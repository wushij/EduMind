<template>
  <div v-if="debugData" class="rag-debug-panel">
    <!-- 1. 顶部总耗时与 Token 指标概览 -->
    <div class="pipeline-summary-bar">
      <div class="summary-metric">
        <span class="label">Pipeline 总耗时</span>
        <span class="value text-emerald">{{ debugData.totalLatencyMs }} <span class="unit">ms</span></span>
      </div>
      <div class="summary-metric">
        <span class="label">Prompt Tokens</span>
        <span class="value">{{ debugData.tokenUsage.promptTokens }}</span>
      </div>
      <div class="summary-metric">
        <span class="label">生成 Tokens</span>
        <span class="value">{{ debugData.tokenUsage.completionTokens }}</span>
      </div>
      <div class="summary-metric">
        <span class="label">总 Token 消耗</span>
        <span class="value text-blue">{{ debugData.tokenUsage.totalTokens }}</span>
      </div>
    </div>

    <!-- 2. Pipeline 执行时序与阶段卡片 -->
    <div class="stages-timeline">
      <div class="timeline-title">
        <el-icon><Histogram /></el-icon>
        <span>执行管线阶段追踪 (Pipeline Stage Tracing)</span>
      </div>

      <div class="stages-flow">
        <div
          v-for="(stage, idx) in debugData.timings"
          :key="stage.stage"
          class="stage-card"
        >
          <div class="stage-card-header">
            <div class="stage-step-num">{{ idx + 1 }}</div>
            <div class="stage-name-col">
              <span class="stage-title">{{ stage.stageName }}</span>
              <span class="stage-code">{{ stage.stage }}</span>
            </div>
            <span class="stage-duration-tag" :class="`is-${(stage.status || 'SUCCESS').toLowerCase()}`">
              {{ stage.status === 'SKIPPED' ? '已跳过' : `${stage.durationMs}ms` }}
            </span>
          </div>
          <p v-if="stage.summary" class="stage-summary">{{ stage.summary }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RAGDebugResponse } from '@/types/knowledge/rag';
import { Histogram } from '@element-plus/icons-vue';

defineProps<{
  debugData: RAGDebugResponse | null;
}>();
</script>

<style scoped lang="scss">
.rag-debug-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .pipeline-summary-bar {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 14px 18px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;

    .summary-metric {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .label {
        font-size: 11.5px;
        color: #94A3B8;
        font-weight: 500;
      }

      .value {
        font-size: 18px;
        font-weight: 700;
        color: #1E293B;

        &.text-emerald { color: #059669; }
        &.text-blue { color: #2563EB; }

        .unit {
          font-size: 12px;
          font-weight: normal;
          color: #94A3B8;
        }
      }
    }
  }

  .stages-timeline {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .timeline-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 700;
      color: #334155;
    }

    .stages-flow {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .stage-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 8px;
        padding: 10px 14px;
        transition: border-color 0.2s;

        &:hover {
          border-color: #BFDBFE;
        }

        .stage-card-header {
          display: flex;
          align-items: center;
          gap: 10px;

          .stage-step-num {
            width: 22px;
            height: 22px;
            border-radius: 50%;
            background: #EFF6FF;
            color: #2563EB;
            font-size: 11px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
          }

          .stage-name-col {
            display: flex;
            flex-direction: column;
            flex: 1;

            .stage-title {
              font-size: 12.5px;
              font-weight: 600;
              color: #1E293B;
            }

            .stage-code {
              font-family: ui-monospace, monospace;
              font-size: 10.5px;
              color: #94A3B8;
            }
          }

          .stage-duration-tag {
            font-family: ui-monospace, monospace;
            font-size: 11.5px;
            font-weight: 600;
            color: #059669;
            background: #ECFDF5;
            padding: 2px 8px;
            border-radius: 4px;

            &.is-skipped {
              color: #64748B;
              background: #F1F5F9;
            }

            &.is-failed {
              color: #DC2626;
              background: #FEF2F2;
            }
          }
        }

        .stage-summary {
          margin: 6px 0 0 32px;
          font-size: 12px;
          color: #64748B;
          line-height: 1.5;
        }
      }
    }
  }
}
</style>
