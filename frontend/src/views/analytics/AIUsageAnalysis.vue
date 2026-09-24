<template>
  <div class="ai-usage-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>AI 消耗与调用分析</h2>
        <p>统计 AI 工具调用次数、Token 消耗与供应商分布</p>
      </div>
      <div class="header-filters">
        <el-select v-model="courseId" clearable placeholder="全部课程" style="width: 220px" @change="reload">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="range" style="width: 120px" @change="reload">
          <el-option label="近 7 天" value="7d" />
          <el-option label="近 30 天" value="30d" />
          <el-option label="近 24 小时" value="24h" />
        </el-select>
      </div>
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
        <div class="kpi-label">总调用次数</div>
        <div class="kpi-value">{{ aiUsageData?.totalCalls?.toLocaleString() ?? 0 }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">总 Token 消耗</div>
        <div class="kpi-value">{{ formatTokens(aiUsageData?.totalTokens) }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">供应商数量</div>
        <div class="kpi-value">{{ aiUsageData?.byProvider?.length ?? 0 }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <span class="card-title">调用趋势与供应商分布</span>
      </template>
      <AIUsageChart :data="aiUsageData" />
    </el-card>

    <el-card shadow="never" class="provider-card">
      <template #header>
        <span class="card-title">供应商明细</span>
      </template>
      <el-table :data="aiUsageData?.byProvider ?? []" stripe>
        <el-table-column prop="provider" label="供应商" />
        <el-table-column prop="calls" label="调用次数" />
        <el-table-column label="Token 消耗">
          <template #default="{ row }">
            {{ formatTokens(row.tokens) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import AIUsageChart from '@/components/analytics/AIUsageChart.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

const { courseOptions, courseId } = useTeacherCourses();
const range = ref('7d');

const { loading, usedMockFallback, aiUsageData, fetchAiUsage } = useLearningAnalytics();

function formatTokens(value?: number) {
  if (value == null) return '-';
  if (value >= 10000) return `${(value / 10000).toFixed(1)} 万`;
  return value.toLocaleString();
}

async function reload() {
  if (!courseId.value || courseId.value <= 0) return;
  await fetchAiUsage(courseId.value, range.value);
}

watch(courseId, () => {
  reload();
});

onMounted(reload);
</script>

<style scoped lang="scss">
.ai-usage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;

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

  .header-filters {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
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

  .chart-card,
  .provider-card {
    border-radius: 14px;

    .card-title {
      font-weight: 700;
      color: #0F172A;
    }
  }
}

@media (max-width: 960px) {
  .kpi-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
