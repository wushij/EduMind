<template>
  <div class="data-table-card">
    <div class="table-card-header">
      <div class="header-left">
        <h3>模型与服务商调用明细及健康度</h3>
        <span class="sub">统计各模型在不同时段的实际算力消耗、输入输出结构、延迟表现与预估成本</span>
      </div>
      <div class="header-right">
        <el-input
          :model-value="modelSearchKey"
          placeholder="按模型名称搜索..."
          prefix-icon="Search"
          clearable
          style="width: 220px;"
          @update:model-value="$emit('update:modelSearchKey', $event)"
        />
      </div>
    </div>

    <el-table :data="filteredProviders" stripe style="width: 100%;">
      <el-table-column prop="provider" label="模型规格 / 供应商" min-width="180">
        <template #default="{ row }">
          <div class="model-name-cell">
            <span class="model-tag font-mono">{{ row.provider }}</span>
            <span class="model-type-hint">{{ getModelProviderBrand(row.provider) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="calls" label="调用次数" width="160" sortable>
        <template #default="{ row }">
          <div class="calls-cell">
            <span class="calls-val font-mono">{{ row.calls?.toLocaleString() }}</span>
            <el-progress
              :percentage="getCallsPercentage(row.calls)"
              :show-text="false"
              :stroke-width="5"
              color="#2563EB"
              style="width: 80px;"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Token 进出比 (输入 / 输出)" min-width="190">
        <template #default="{ row }">
          <div class="token-ratio-cell font-mono">
            <span class="prompt-text">{{ formatTokens(row.promptTokens ?? Math.round(row.tokens * 0.4)) }}</span>
            <span class="ratio-slash">/</span>
            <span class="completion-text">{{ formatTokens(row.completionTokens ?? Math.round(row.tokens * 0.6)) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="tokens" label="总 Token 消耗" width="160" sortable>
        <template #default="{ row }">
          <span class="total-token-val font-mono">{{ row.tokens?.toLocaleString() }}</span>
        </template>
      </el-table-column>

      <el-table-column label="预估费用 (¥)" width="130">
        <template #default="{ row }">
          <span class="cost-val font-mono">¥{{ ((row.cost ?? (row.tokens / 1000) * 0.002)).toFixed(3) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="avgLatencyMs" label="平均耗时" width="130" sortable>
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="(row.avgLatencyMs ?? 0) <= 800 ? 'success' : ((row.avgLatencyMs ?? 0) <= 1500 ? 'primary' : 'warning')"
          >
            {{ row.avgLatencyMs ?? 0 }} ms
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="熔断保护状态" width="140">
        <template #default="{ row }">
          <div class="circuit-status-cell">
            <span class="status-indicator-dot" :class="getCircuitStateClass(row.provider)"></span>
            <span class="status-text">{{ getCircuitStateText(row.provider) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="$emit('open-trace-by-model', row.provider)">
            链路 Trace
          </el-button>
          <el-button type="danger" link size="small" @click="$emit('reset-model-circuit', row.provider)">
            复位
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { GatewayMetricsVO } from '@/types/system/gateway';

type ProviderRow = NonNullable<GatewayMetricsVO['byProvider']>[number];

defineProps<{
  modelSearchKey: string;
  filteredProviders: ProviderRow[];
  formatTokens: (value?: number) => string;
  getCallsPercentage: (calls: number) => number;
  getModelProviderBrand: (name: string) => string;
  getCircuitStateClass: (modelKey: string) => string;
  getCircuitStateText: (modelKey: string) => string;
}>();

defineEmits<{
  'update:modelSearchKey': [value: string];
  'open-trace-by-model': [provider: string];
  'reset-model-circuit': [provider: string];
}>();
</script>

<style scoped lang="scss">
.data-table-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 22px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

  .table-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;
    flex-wrap: wrap;
    gap: 12px;

    .header-left {
      h3 {
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
        margin: 0;
      }
      .sub {
        font-size: 12px;
        color: #64748B;
        margin-top: 4px;
        display: block;
      }
    }
  }

  .model-name-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .model-tag {
      font-weight: 700;
      color: #1E293B;
    }
    .model-type-hint {
      font-size: 11px;
      color: #94A3B8;
    }
  }

  .calls-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .calls-val {
      font-weight: 700;
      color: #0F172A;
    }
  }

  .token-ratio-cell {
    font-size: 12.5px;
    .prompt-text { color: #2563EB; font-weight: 600; }
    .ratio-slash { color: #CBD5E1; margin: 0 4px; }
    .completion-text { color: #06B6D4; font-weight: 600; }
  }

  .total-token-val {
    font-weight: 700;
    color: #0F172A;
  }

  .cost-val {
    font-weight: 600;
    color: #D97706;
  }

  .circuit-status-cell {
    display: flex;
    align-items: center;
    gap: 6px;

    .status-indicator-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.closed { background: #10B981; }
      &.open { background: #EF4444; }
      &.half-open { background: #F59E0B; }
    }

    .status-text {
      font-size: 12px;
      font-weight: 500;
      color: #475569;
    }
  }
}
</style>
