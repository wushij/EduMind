<template>
  <div class="gateway-dashboard" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>AI 网关监控</h2>
        <p>查看模型网关请求量、成功率、延迟与供应商分布</p>
      </div>
      <el-select v-model="range" style="width: 140px" @change="loadMetrics">
        <el-option label="近 24 小时" value="24h" />
        <el-option label="近 7 天" value="7d" />
        <el-option label="近 30 天" value="30d" />
      </el-select>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <div class="kpi-grid">
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">总请求量</div>
        <div class="kpi-value">{{ metrics?.totalRequests?.toLocaleString() ?? 0 }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">成功率</div>
        <div class="kpi-value">{{ metrics?.successRate?.toFixed(1) ?? 0 }}%</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">平均延迟</div>
        <div class="kpi-value">{{ metrics?.avgLatencyMs ?? 0 }} ms</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">降级次数</div>
        <div class="kpi-value">{{ metrics?.fallbackCount?.toLocaleString() ?? 0 }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="provider-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">供应商调用分布</span>
          <el-button type="primary" link @click="router.push('/system/gateway/routes')">
            路由规则配置 →
          </el-button>
        </div>
      </template>
      <el-table :data="metrics?.byProvider ?? []" stripe>
        <el-table-column prop="provider" label="供应商" />
        <el-table-column prop="calls" label="调用次数" />
        <el-table-column label="Token 消耗">
          <template #default="{ row }">
            {{ row.tokens?.toLocaleString() }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getGatewayMetrics } from '@/api/system/gateway';
import { USE_MOCK } from '@/config/mock';
import { MOCK_GATEWAY_METRICS } from '@/mock/gateway';
import type { GatewayMetricsVO } from '@/types/system/gateway';
import { ElMessage } from 'element-plus';

const router = useRouter();
const loading = ref(false);
const usedMockFallback = ref(false);
const range = ref('24h');
const metrics = ref<GatewayMetricsVO | null>(null);

async function loadMetrics() {
  loading.value = true;
  usedMockFallback.value = false;
  try {
    const res = await getGatewayMetrics(range.value);
    metrics.value = res.data;
  } catch {
    if (USE_MOCK) {
      usedMockFallback.value = true;
      metrics.value = MOCK_GATEWAY_METRICS;
    } else {
      metrics.value = null;
      ElMessage.error('加载网关指标失败');
    }
  } finally {
    loading.value = false;
  }
}

onMounted(loadMetrics);
</script>

<style scoped lang="scss">
.gateway-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;

    h2 {
      margin: 0 0 6px;
      font-size: 22px;
      font-weight: 700;
      color: #0F172A;
    }

    p {
      margin: 0;
      color: #64748B;
      font-size: 14px;
    }
  }

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }

  .kpi-card {
    border-radius: 12px;

    .kpi-label {
      font-size: 13px;
      color: #64748B;
      margin-bottom: 8px;
    }

    .kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .provider-card {
    border-radius: 14px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .card-title {
      font-weight: 700;
      color: #0F172A;
    }
  }
}

@media (max-width: 960px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
</style>
