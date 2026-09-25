<template>
  <div class="ai-usage-page" v-loading="loading">
    <!-- 1. 顶部长圆毛玻璃 Hero 导航与控制中心 (与学情概览/学情分析同款视觉设计) -->
    <AIUsageHero
      :course-id="selectedCourseId"
      :course-name="currentCourseName"
      :course-code="currentCourseCode"
      :course-options="courseOptions"
      :courses-loading="coursesLoading"
      :range="range"
      :loading="loading || logsLoading"
      :avg-latency-ms="aiUsageData?.avgLatencyMs ?? 280"
      :success-rate="aiUsageData?.successRate ?? 99.8"
      :total-saved-hours="aiUsageData?.totalSavedHours ?? 0"
      @change-course="handleCourseChange"
      @change-range="handleRangeChange"
      @refresh="handleRefreshAll"
      @export="handleExportAuditLog"
    />

    <!-- Mock 兜底提示条 -->
    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前数据由本地智能兜底引擎呈现（后端接口离线或 VITE_USE_MOCK=true）"
      class="mock-alert"
    />

    <!-- 2. 4 维微渐变长圆 KPI 算力指标卡 -->
    <div class="kpi-grid">
      <div class="kpi-card kpi-card--blue">
        <div class="kpi-icon-wrap">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">累计总调用次数</div>
          <div class="kpi-value-row">
            <span class="kpi-value">{{ aiUsageData?.totalCalls?.toLocaleString() ?? 0 }}</span>
            <span class="kpi-unit">次</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-sub-tip">今日调用: <strong>+{{ aiUsageData?.todayCalls?.toLocaleString() ?? 0 }}</strong> 次</span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-card--emerald">
        <div class="kpi-icon-wrap">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">累计 Token 算力消耗</div>
          <div class="kpi-value-row">
            <span class="kpi-value">{{ formatTokens(aiUsageData?.totalTokens) }}</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-sub-tip">今日消耗: <strong>{{ formatTokens(aiUsageData?.todayTokens) }}</strong></span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-card--purple">
        <div class="kpi-icon-wrap">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">模型平均响应耗时</div>
          <div class="kpi-value-row">
            <span class="kpi-value">{{ aiUsageData?.avgLatencyMs ?? 280 }}</span>
            <span class="kpi-unit">ms</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-sub-tip">健康度: <span class="text-success font-semibold">优秀 (< 500ms)</span></span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-card--amber">
        <div class="kpi-icon-wrap">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">活跃接入场景数</div>
          <div class="kpi-value-row">
            <span class="kpi-value">{{ activeSceneCount }}</span>
            <span class="kpi-unit">类</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-sub-tip">主力模型: <strong>{{ mainProviderName }}</strong></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 可视化图表区：双轴时序趋势 + 教育场景/模型分布环形图 -->
    <AIUsageChart :data="aiUsageData" />

    <!-- 4. 核心功能补全：AI 调用明细审计流水表格 -->
    <AIUsageLogTable
      :logs="aiLogsData?.list ?? []"
      :total="aiLogsData?.total ?? 0"
      :page-num="aiLogsData?.pageNum ?? 1"
      :page-size="aiLogsData?.pageSize ?? 10"
      :loading="logsLoading"
      @query-change="handleLogQueryChange"
      @view-detail="handleOpenLogDetail"
    />

    <!-- 5. 单次调用审计详情抽屉 -->
    <AIUsageDetailDrawer
      v-model="drawerVisible"
      :log-item="selectedLogItem"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Cpu, Coin, Timer, Operation } from '@element-plus/icons-vue';
import AIUsageHero from '@/components/analytics/AIUsageHero.vue';
import AIUsageChart from '@/components/analytics/AIUsageChart.vue';
import AIUsageLogTable from '@/components/analytics/AIUsageLogTable.vue';
import AIUsageDetailDrawer from '@/components/analytics/AIUsageDetailDrawer.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { AiCallLogItem, AiUsageLogQuery } from '@/types/analytics/learning';

const { courseOptions, courseId, loading: coursesLoading } = useTeacherCourses();

const selectedCourseId = ref<number | undefined>(undefined);
const range = ref('7d');

const {
  loading,
  logsLoading,
  usedMockFallback,
  aiUsageData,
  aiLogsData,
  fetchAiUsage,
  fetchAiUsageLogs
} = useLearningAnalytics();

const drawerVisible = ref(false);
const selectedLogItem = ref<AiCallLogItem | null>(null);

const currentCourse = computed(() => {
  if (!selectedCourseId.value) return null;
  return courseOptions.value.find((c) => c.id === selectedCourseId.value);
});

const currentCourseName = computed(() => currentCourse.value?.name || '');
const currentCourseCode = computed(() => (currentCourse.value as any)?.code || '');

const activeSceneCount = computed(() => {
  return aiUsageData.value?.byScene?.length || 5;
});

const mainProviderName = computed(() => {
  const p = aiUsageData.value?.byProvider?.[0];
  return p ? p.provider : 'DeepSeek-V3';
});

function formatTokens(val?: number) {
  if (val == null) return '0';
  if (val >= 1000000) return `${(val / 1000000).toFixed(2)} M`;
  if (val >= 10000) return `${(val / 10000).toFixed(1)} 万`;
  return val.toLocaleString();
}

async function loadData() {
  const cid = selectedCourseId.value && selectedCourseId.value > 0 ? selectedCourseId.value : undefined;
  await Promise.all([
    fetchAiUsage(cid, range.value),
    fetchAiUsageLogs({
      courseId: cid,
      range: range.value,
      pageNum: 1,
      pageSize: 10
    })
  ]);
}

function handleCourseChange(val?: number) {
  selectedCourseId.value = val;
  loadData();
}

function handleRangeChange(val: string) {
  range.value = val;
  loadData();
}

function handleRefreshAll() {
  loadData();
  ElMessage.success('已刷新最新 AI 算力与调用审计数据');
}

function handleLogQueryChange(params: { scene?: string; model?: string; pageNum: number; pageSize: number }) {
  const cid = selectedCourseId.value && selectedCourseId.value > 0 ? selectedCourseId.value : undefined;
  fetchAiUsageLogs({
    courseId: cid,
    range: range.value,
    ...params
  });
}

function handleOpenLogDetail(item: AiCallLogItem) {
  selectedLogItem.value = item;
  drawerVisible.value = true;
}

function handleExportAuditLog() {
  const list = aiLogsData.value?.list ?? [];
  if (list.length === 0) {
    ElMessage.warning('暂无调用明细数据可导出');
    return;
  }
  let csv = '流水ID,调用时间,调用人,登录账号,身份角色,业务场景,模型,PromptTokens,CompletionTokens,总Tokens,延迟(ms)\n';
  for (const item of list) {
    const time = item.createTime || '';
    const callerName = item.realName || item.username || (item.userId ? `用户#${item.userId}` : '系统/匿名');
    const uname = item.username || '';
    const urole = item.userRole || '';
    const scene = item.sceneLabel || item.scene || '';
    const model = item.model || '';
    const pt = item.promptTokens ?? 0;
    const ct = item.completionTokens ?? 0;
    const tt = item.totalTokens ?? pt + ct;
    const lat = item.latencyMs ?? 0;
    csv += `"${item.id}","${time}","${callerName}","${uname}","${urole}","${scene}","${model}","${pt}","${ct}","${tt}","${lat}"\n`;
  }

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `EduMind_AI算力调用审计_${new Date().toISOString().substring(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success('AI 调用审计报表已导出');
}

watch(courseId, (val) => {
  if (val && val > 0 && selectedCourseId.value === undefined) {
    selectedCourseId.value = val;
  }
});

onMounted(() => {
  if (courseId.value && courseId.value > 0) {
    selectedCourseId.value = courseId.value;
  }
  loadData();
});
</script>

<style scoped lang="scss">
.ai-usage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 30px;

  .mock-alert {
    border-radius: 12px;
  }

  /* 4 维微渐变长圆 KPI 指标卡网格 */
  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }

  .kpi-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    padding: 20px;
    display: flex;
    align-items: flex-start;
    gap: 16px;
    box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.03);
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px -4px rgba(0, 0, 0, 0.06);
    }

    .kpi-icon-wrap {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
    }

    .kpi-body {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .kpi-label {
      font-size: 13px;
      color: #64748b;
      margin-bottom: 6px;
      font-weight: 500;
    }

    .kpi-value-row {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-bottom: 8px;
    }

    .kpi-value {
      font-size: 26px;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.02em;
      line-height: 1.1;
    }

    .kpi-unit {
      font-size: 12px;
      color: #94a3b8;
      font-weight: 500;
    }

    .kpi-footer {
      font-size: 12px;
      color: #64748b;

      strong {
        color: #1e293b;
      }
    }

    /* 4 款主题渐变 */
    &--blue {
      .kpi-icon-wrap {
        background: #eff6ff;
        color: #2563eb;
      }
    }

    &--emerald {
      .kpi-icon-wrap {
        background: #ecfdf5;
        color: #059669;
      }
    }

    &--purple {
      .kpi-icon-wrap {
        background: #f5f3ff;
        color: #7c3aed;
      }
    }

    &--amber {
      .kpi-icon-wrap {
        background: #fffbeb;
        color: #d97706;
      }
    }
  }
}

@media (max-width: 1200px) {
  .ai-usage-page .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 680px) {
  .ai-usage-page .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
