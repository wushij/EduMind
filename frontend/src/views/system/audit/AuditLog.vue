<template>
  <div class="audit-log-page">
    <!-- 1. 顶部 Token 审计与成本核心指标看板 -->
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

    <!-- 2. Token 消耗与调用频次趋势分析折线图 -->
    <div class="trend-visual-card">
      <div class="visual-header">
        <div class="title-group">
          <div class="title-main">
            <span class="dot-indicator"></span>
            <span class="title">Token 消耗与调用频次趋势分析</span>
          </div>
          <span class="sub">每日自动归档统计 · 支持自适应缩放与双指标拟合折线图</span>
        </div>

        <div class="chart-actions">
          <div class="range-switch-pill">
            <button
              :class="['pill-btn', { active: trendDays === 7 }]"
              @click="changeTrendDays(7)"
            >
              近 7 天
            </button>
            <button
              :class="['pill-btn', { active: trendDays === 14 }]"
              @click="changeTrendDays(14)"
            >
              近 14 天
            </button>
            <button
              :class="['pill-btn', { active: trendDays === 30 }]"
              @click="changeTrendDays(30)"
            >
              近 30 天
            </button>
          </div>
        </div>
      </div>

      <!-- ECharts 折线图容器 -->
      <div v-loading="trendLoading" class="chart-container">
        <div ref="trendChartRef" class="echarts-box" />
      </div>
    </div>

    <!-- 3. 调用审计明细表格与多维筛选卡片 -->
    <div class="log-section-card">
      <!-- 筛选工具栏 -->
      <div class="filter-header-bar">
        <div class="title-wrap">
          <h3 class="sec-title">AI 调用全链路审计日志</h3>
          <span class="sec-count">共 {{ total }} 条调用记录</span>
        </div>

        <div class="filter-controls">
          <el-select
            v-model="sceneFilter"
            placeholder="业务场景"
            clearable
            class="filter-pill-select"
            @change="handleSearch"
          >
            <el-option value="" label="全部场景" />
            <el-option value="CHAT" label="智能对话 (CHAT)" />
            <el-option value="CHAT_RAG" label="知识增强对话 (CHAT_RAG)" />
            <el-option value="PREP" label="备课助手 (PREP)" />
            <el-option value="RAG" label="知识问答 (RAG)" />
            <el-option value="GRADING" label="作业批改 (GRADING)" />
            <el-option value="AGENT" label="智能体协作 (AGENT)" />
          </el-select>

          <!-- 动态加载已配置的系统模型，不硬编码 mock 或 chat -->
          <el-select
            v-model="modelFilter"
            :placeholder="modelOptions.length ? '生成模型' : '暂无可用模型'"
            :disabled="!modelOptions.length"
            clearable
            class="filter-pill-select"
            @change="handleSearch"
          >
            <el-option value="" label="全部模型" />
            <el-option
              v-for="m in modelOptions"
              :key="m"
              :value="m"
              :label="m"
            />
          </el-select>

          <el-select
            v-model="statusFilter"
            placeholder="调用状态"
            clearable
            class="filter-pill-select"
            @change="handleSearch"
          >
            <el-option value="" label="全部状态" />
            <el-option value="SUCCESS" label="成功 (SUCCESS)" />
            <el-option value="TIMEOUT" label="超时 (TIMEOUT)" />
            <el-option value="FAILED" label="异常 (FAILED)" />
          </el-select>

          <el-input
            v-model="searchKeyword"
            placeholder="搜索 Trace ID、用户名..."
            clearable
            class="filter-pill-input"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />

          <el-button
            type="primary"
            class="pill-action-btn primary"
            :disabled="tableLoading"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button
            round
            class="btn-refresh"
            :icon="Refresh"
            :loading="tableLoading"
            @click="handleReset"
          >
            重置
          </el-button>
        </div>
      </div>

      <!-- 表格数据展示 -->
      <div class="table-wrap">
        <el-table
          v-loading="tableLoading"
          element-loading-text="正在检索全链路 AI 审计日志..."
          :data="logs"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '48px' }"
        >
          <!-- 链路 Trace ID -->
          <el-table-column prop="traceId" label="链路 Trace ID" min-width="150">
            <template #default="{ row }">
              <div class="trace-cell">
                <span class="trace-badge">{{ row.traceId }}</span>
                <el-tooltip content="复制 Trace ID" placement="top">
                  <button class="copy-btn" @click="copyText(row.traceId)">
                    <el-icon><CopyDocument /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>

          <!-- 调用用户（显示图形头像，对标 UserList） -->
          <el-table-column label="调用用户" min-width="170">
            <template #default="{ row }">
              <div class="user-info-cell">
                <el-avatar
                  :size="36"
                  :src="getUserAvatarUrl(row)"
                  class="em-user-avatar-sm"
                >
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="user-text">
                  <span class="user-name">{{ row.realName || row.username }}</span>
                  <div class="user-sub">
                    <span class="user-account">@{{ row.username }}</span>
                    <span class="role-pill" :class="row.userRole.toLowerCase()">{{ getRoleLabel(row.userRole) }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 业务场景 / 工具 -->
          <el-table-column prop="toolName" label="业务场景 / 工具" min-width="160">
            <template #default="{ row }">
              <div class="scene-tag-wrap">
                <span class="scene-badge" :class="getSceneStyleClass(row.scene || row.toolName)">
                  {{ getSceneLabel(row.scene || row.toolName) }}
                </span>
              </div>
            </template>
          </el-table-column>

          <!-- 生成模型 -->
          <el-table-column prop="model" label="生成模型" min-width="150">
            <template #default="{ row }">
              <span class="model-badge">
                <el-icon class="model-icon"><Cpu /></el-icon>
                {{ row.model }}
              </span>
            </template>
          </el-table-column>

          <!-- Token 规模与成本 -->
          <el-table-column label="Token 规模 & 成本" min-width="180">
            <template #default="{ row }">
              <div class="tokens-cell">
                <div class="tokens-main">
                  <span class="token-sum">{{ row.totalTokens.toLocaleString() }}</span>
                  <span class="cost-val">¥{{ row.estimatedCost.toFixed(4) }}</span>
                </div>
                <div class="tokens-split">
                  入: {{ row.promptTokens }} · 出: {{ row.completionTokens }}
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 耗时 Latency -->
          <el-table-column label="耗时" min-width="110">
            <template #default="{ row }">
              <span class="latency-badge" :class="getLatencyClass(row.durationMs)">
                {{ row.durationMs }}ms
              </span>
            </template>
          </el-table-column>

          <!-- 调用状态 -->
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="status-pill" :class="row.status.toLowerCase()">
                <span class="status-dot"></span>
                {{ row.status }}
              </span>
            </template>
          </el-table-column>

          <!-- 请求时间 -->
          <el-table-column prop="createdAt" label="请求时间" min-width="165" />

          <!-- 操作 -->
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <button class="table-detail-btn" @click="viewDetail(row)">
                <el-icon><View /></el-icon>
                <span>详情</span>
              </button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页栏：严格左对齐，支持每页 10 条分页 -->
      <div class="pagination-footer">
        <AppPagination
          v-model:page-num="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadLogs"
        />
      </div>
    </div>

    <!-- 4. 链路调用审计详情抽屉（使用专业 Icon 代替 Emoji） -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="AI 调用全链路审计详情"
      size="580px"
      destroy-on-close
      class="audit-detail-drawer"
    >
      <div v-if="selectedLog" class="drawer-content">
        <!-- 头部 Trace 概览卡片 -->
        <div class="drawer-hero-card">
          <div class="hero-top">
            <div class="trace-title">
              <span class="label">Trace ID</span>
              <span class="val">{{ selectedLog.traceId }}</span>
            </div>
            <span class="status-pill" :class="selectedLog.status.toLowerCase()">
              <span class="status-dot"></span>
              {{ selectedLog.status }}
            </span>
          </div>
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hi-label">总消耗 Token</span>
              <span class="hi-val text-purple">{{ selectedLog.totalTokens.toLocaleString() }}</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">响应耗时</span>
              <span class="hi-val text-emerald">{{ selectedLog.durationMs }}ms</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">预估成本</span>
              <span class="hi-val text-amber">¥{{ selectedLog.estimatedCost.toFixed(4) }}</span>
            </div>
          </div>
        </div>

        <!-- 结构化项：全部使用 el-icon 规范呈现，无 emoji -->
        <div class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-blue"><User /></el-icon>
            <span>调用方身份上下文</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">调用用户</span>
              <div class="v user-flex-item">
                <el-avatar :size="24" :src="getUserAvatarUrl(selectedLog)" class="mr-1" />
                <span class="font-bold">{{ selectedLog.realName || selectedLog.username }} (@{{ selectedLog.username }})</span>
              </div>
            </div>
            <div class="info-row">
              <span class="k">用户 ID / 角色</span>
              <span class="v">ID: {{ selectedLog.userId }} · <span class="role-pill" :class="selectedLog.userRole.toLowerCase()">{{ getRoleLabel(selectedLog.userRole) }}</span></span>
            </div>
            <div class="info-row">
              <span class="k">请求 IP 地址</span>
              <span class="v monospace">{{ selectedLog.ipAddress || '127.0.0.1' }}</span>
            </div>
            <div class="info-row">
              <span class="k">发起时间</span>
              <span class="v monospace">{{ selectedLog.createdAt }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-purple"><Cpu /></el-icon>
            <span>AI 模型与调度场景</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">业务场景 / 工具</span>
              <span class="v">
                <span class="scene-badge" :class="getSceneStyleClass(selectedLog.scene || selectedLog.toolName)">
                  {{ getSceneLabel(selectedLog.scene || selectedLog.toolName) }}
                </span>
              </span>
            </div>
            <div class="info-row">
              <span class="k">生成模型</span>
              <span class="v monospace font-bold">{{ selectedLog.model }}</span>
            </div>
            <div class="info-row">
              <span class="k">模型提供方</span>
              <span class="v">{{ selectedLog.provider }}</span>
            </div>
            <div v-if="selectedLog.courseId" class="info-row">
              <span class="k">关联课程 ID</span>
              <span class="v monospace">Course #{{ selectedLog.courseId }}</span>
            </div>
            <div v-if="selectedLog.conversationId" class="info-row">
              <span class="k">关联会话 ID</span>
              <span class="v monospace">{{ selectedLog.conversationId }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-emerald"><DataLine /></el-icon>
            <span>Token 细化度量</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">输入 Prompt Tokens</span>
              <span class="v monospace">{{ selectedLog.promptTokens }} toks</span>
            </div>
            <div class="info-row">
              <span class="k">输出 Completion Tokens</span>
              <span class="v monospace">{{ selectedLog.completionTokens }} toks</span>
            </div>
            <div class="info-row">
              <span class="k">总计 Tokens</span>
              <span class="v monospace font-bold">{{ selectedLog.totalTokens }} toks</span>
            </div>
            <div class="info-row">
              <span class="k">预估算力成本</span>
              <span class="v text-amber font-bold">¥ {{ selectedLog.estimatedCost.toFixed(4) }} RMB</span>
            </div>
          </div>
        </div>

        <div v-if="selectedLog.knowledgeBaseId || selectedLog.retrievalHitCount" class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-cyan"><Reading /></el-icon>
            <span>RAG 知识检索与切片溯源</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">知识库 ID</span>
              <span class="v monospace">KB #{{ selectedLog.knowledgeBaseId }}</span>
            </div>
            <div class="info-row">
              <span class="k">向量检索命中切片数</span>
              <span class="v font-bold">{{ selectedLog.retrievalHitCount }} 片</span>
            </div>
            <div v-if="selectedLog.citationDocIds" class="info-row">
              <span class="k">引用文档溯源 ID</span>
              <span class="v monospace">{{ selectedLog.citationDocIds }}</span>
            </div>
          </div>
        </div>

        <!-- 原始元数据预览 -->
        <div class="detail-section">
          <div class="json-header">
            <h4 class="sec-heading mb-0">
              <el-icon class="sec-icon icon-amber"><Document /></el-icon>
              <span>链路完整元数据 (JSON)</span>
            </h4>
            <button class="copy-json-btn" @click="copyText(JSON.stringify(selectedLog, null, 2))">
              <el-icon><CopyDocument /></el-icon>
              <span>复制 JSON</span>
            </button>
          </div>
          <pre class="json-code-block"><code>{{ JSON.stringify(selectedLog, null, 2) }}</code></pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  DataLine,
  Cpu,
  Coin,
  Timer,
  Search,
  Refresh,
  RefreshRight,
  CopyDocument,
  View,
  User,
  Reading,
  Document
} from '@element-plus/icons-vue';
import { getAuditSummary, getAuditLogs, getDailyTrend, getAvailableAiModels } from '@/api/system/audit';
import { AIAuditLog, AuditSummaryVO, AuditDailyTrendItem } from '@/types/system/audit';
import AppPagination from '@/components/common/AppPagination.vue';
import { normalizeAvatarUrl } from '@/utils/format/file';

// 空统计指标基准
const createEmptySummary = (): AuditSummaryVO => ({
  totalCalls: 0,
  totalTokens: 0,
  totalCostRMB: 0,
  avgLatencyMs: 0,
  successRate: 100,
  dailyTrend: [],
  modelDistribution: []
});

// 核心统计指标
const summary = ref<AuditSummaryVO>(createEmptySummary());

// 趋势折线图
const trendDays = ref(7);
const trendLoading = ref(false);
const trendChartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

// 表格与分页状态：默认 10 条/页，与用户管理保持一致
const logs = ref<AIAuditLog[]>([]);
const tableLoading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 筛选条件
const sceneFilter = ref('');
const modelFilter = ref('');
const statusFilter = ref('');
const searchKeyword = ref('');
const modelOptions = ref<string[]>([]);

// 详情抽屉
const detailDrawerVisible = ref(false);
const selectedLog = ref<AIAuditLog | null>(null);

// 初始化加载
onMounted(async () => {
  await Promise.all([loadSummary(), loadTrendData(trendDays.value), loadLogs(), loadModels()]);
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance.value?.dispose();
  chartInstance.value = null;
});

function handleResize() {
  chartInstance.value?.resize();
}

// 加载当前系统可用模型列表
async function loadModels() {
  try {
    const models = await getAvailableAiModels();
    modelOptions.value = models && models.length > 0 ? models : [];
  } catch (err) {
    console.warn('Failed to load models list', err);
    modelOptions.value = [];
  }
}

// 加载汇总核心指标
async function loadSummary() {
  try {
    summary.value = await getAuditSummary();
  } catch (err) {
    console.error('Failed to load audit summary', err);
    summary.value = createEmptySummary();
    ElMessage.warning('审计摘要加载失败');
  }
}

// 切换趋势天数
async function changeTrendDays(days: number) {
  trendDays.value = days;
  await loadTrendData(days);
}

// 加载趋势折线图数据并渲染 ECharts
async function loadTrendData(days: number) {
  trendLoading.value = true;
  try {
    const trendData = await getDailyTrend(days);
    renderTrendChart(trendData);
  } catch (err) {
    console.error('Failed to load trend data', err);
  } finally {
    trendLoading.value = false;
  }
}

// 构建并渲染折线图 (双 Y 轴: Token 消耗面积折线 + 调用频次折线)
function renderTrendChart(trendData: AuditDailyTrendItem[]) {
  if (!trendChartRef.value) return;

  if (!chartInstance.value) {
    chartInstance.value = echarts.init(trendChartRef.value);
  }

  const dates = trendData.map((d) => d.date.slice(5)); // '09-14'
  const tokens = trendData.map((d) => d.tokens);
  const calls = trendData.map((d) => d.calls);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: { color: '#1E293B', fontSize: 12 },
      extraCssText: 'box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08); border-radius: 10px; padding: 12px 16px;',
      formatter: (params: any) => {
        if (!params || !params.length) return '';
        const idx = params[0].dataIndex;
        const item = trendData[idx];
        if (!item) return '';

        return `
          <div style="font-weight: 700; margin-bottom: 8px; color: #0F172A; font-size: 13px;">${item.date} 统计详情</div>
          <div style="display: flex; justify-content: space-between; gap: 20px; margin-bottom: 4px; font-size: 12px; color: #475569;">
            <span><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#2563EB;margin-right:6px;"></span>Token 消耗量:</span>
            <b style="color: #1E293B; font-family: monospace;">${item.tokens.toLocaleString()} Tokens</b>
          </div>
          <div style="display: flex; justify-content: space-between; gap: 20px; margin-bottom: 4px; font-size: 12px; color: #475569;">
            <span><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#F59E0B;margin-right:6px;"></span>调用频次:</span>
            <b style="color: #1E293B; font-family: monospace;">${item.calls} 次</b>
          </div>
          <div style="display: flex; justify-content: space-between; gap: 20px; margin-bottom: 4px; font-size: 12px; color: #475569;">
            <span>Token 构成:</span>
            <span style="color: #64748B;">入: ${item.promptTokens} · 出: ${item.completionTokens}</span>
          </div>
          <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 12px; color: #475569;">
            <span>预估算力成本:</span>
            <b style="color: #D97706; font-family: monospace;">¥${item.cost.toFixed(4)}</b>
          </div>
        `;
      }
    },
    legend: {
      data: ['Token 消耗量 (Tokens)', '调用频次 (次)'],
      left: 'center',
      top: 4,
      textStyle: { color: '#64748B', fontSize: 12 },
      icon: 'roundRect'
    },
    grid: {
      left: '2%',
      right: '2%',
      top: 50,
      bottom: '6%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 12, margin: 12 }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Token 消耗',
        nameTextStyle: { color: '#94A3B8', fontSize: 11, align: 'right', padding: [0, 6, 4, 0] },
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: {
          color: '#64748B',
          fontSize: 11,
          formatter: (v: number) => (v >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`)
        }
      },
      {
        type: 'value',
        name: '调用频次',
        nameTextStyle: { color: '#94A3B8', fontSize: 11, align: 'left', padding: [0, 0, 4, 6] },
        splitLine: { show: false },
        minInterval: 1,
        axisLabel: { color: '#64748B', fontSize: 11, formatter: '{value} 次' }
      }
    ],
    series: [
      {
        name: 'Token 消耗量 (Tokens)',
        type: 'line',
        smooth: 0.35,
        yAxisIndex: 0,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#2563EB', borderColor: '#FFFFFF', borderWidth: 2 },
        lineStyle: { width: 3, color: '#2563EB' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.28)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.01)' }
          ])
        },
        data: tokens
      },
      {
        name: '调用频次 (次)',
        type: 'line',
        smooth: 0.35,
        yAxisIndex: 1,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#F59E0B', borderColor: '#FFFFFF', borderWidth: 2 },
        lineStyle: { width: 2.5, color: '#F59E0B' },
        data: calls
      }
    ]
  };

  chartInstance.value.setOption(option, true);
}

// 获取用户头像真实 URL 或优质头像兜底
function getUserAvatarUrl(row?: AIAuditLog | null): string {
  if (!row) return '';
  if (row.avatar) {
    const normalized = normalizeAvatarUrl(row.avatar);
    if (normalized) return normalized;
  }
  const seed = row.username || `User_${row.userId}`;
  return `https://api.dicebear.com/7.x/bottts/svg?seed=${seed}&backgroundColor=e0e7ff`;
}

// 加载日志列表（支持每页 10 条分页）
async function loadLogs() {
  tableLoading.value = true;
  try {
    const [res] = await Promise.all([
      getAuditLogs({
        page: pageNum.value,
        pageSize: pageSize.value,
        scene: sceneFilter.value || undefined,
        model: modelFilter.value || undefined,
        status: statusFilter.value || undefined,
        keyword: searchKeyword.value.trim() || undefined
      }),
      // 保底微延时 220ms，确保本地毫秒级极速响应也能呈现清晰平滑的刷新加载反馈
      new Promise((resolve) => setTimeout(resolve, 220))
    ]);
    logs.value = res.list;
    total.value = res.total;

    // 动态提取日志中存在的模型名称补充至下拉框
    const logModels = res.list.map((l) => l.model).filter(Boolean);
    if (logModels.length > 0) {
      modelOptions.value = Array.from(new Set([...modelOptions.value, ...logModels]));
    }
  } catch (err) {
    console.error('Failed to load audit logs', err);
    logs.value = [];
    total.value = 0;
  } finally {
    tableLoading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadLogs();
}

function handleReset() {
  sceneFilter.value = '';
  modelFilter.value = '';
  statusFilter.value = '';
  searchKeyword.value = '';
  pageNum.value = 1;
  loadLogs();
}

// 打开查看详情
function viewDetail(row: AIAuditLog) {
  selectedLog.value = row;
  detailDrawerVisible.value = true;
}

// 复制文本辅助函数
async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败，请手动选取复制');
  }
}

// 角色、场景、耗时视觉标签样式辅助
function getRoleLabel(role?: string) {
  if (!role) return '用户';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[role.toUpperCase()] || role;
}

function getSceneLabel(scene?: string) {
  if (!scene) return '通用对话';
  const s = scene.toUpperCase();
  const map: Record<string, string> = {
    CHAT: '智能对话 (CHAT)',
    CHAT_RAG: '知识增强对话 (CHAT_RAG)',
    PREP: '备课助手 (PREP)',
    RAG: '知识问答 (RAG)',
    GRADING: '作业批改 (GRADING)',
    AGENT: '智能体协作 (AGENT)',
    GLOBAL_ASSISTANT: '全局助手 (ASSISTANT)',
    QUESTION_GENERATE: '智能出题 (QUESTION)'
  };
  return map[s] || scene;
}

function getSceneStyleClass(scene?: string) {
  const s = (scene || '').toUpperCase();
  if (s === 'CHAT' || s === 'CHAT_RAG') return 'badge-chat';
  if (s === 'PREP') return 'badge-prep';
  if (s === 'RAG') return 'badge-rag';
  if (s === 'GRADING') return 'badge-grading';
  if (s === 'AGENT') return 'badge-agent';
  return 'badge-default';
}

function getLatencyClass(ms: number) {
  if (ms < 300) return 'lat-fast';
  if (ms < 1000) return 'lat-normal';
  if (ms < 3000) return 'lat-warning';
  return 'lat-danger';
}
</script>

<style scoped lang="scss">
.audit-log-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 4px;

  /* 1. 顶部指标网格 */
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

  /* 2. 趋势可视化折线图卡片 */
  .trend-visual-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 16px;
    padding: 20px 24px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);

    .visual-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .title-group {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .title-main {
          display: flex;
          align-items: center;
          gap: 8px;

          .dot-indicator {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #2563EB;
          }

          .title {
            font-size: 15px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .sub {
          font-size: 12px;
          color: #94A3B8;
          padding-left: 16px;
        }
      }

      .chart-actions {
        .range-switch-pill {
          display: flex;
          background: #F1F5F9;
          padding: 3px;
          border-radius: 9999px;
          gap: 2px;

          .pill-btn {
            border: none;
            background: transparent;
            padding: 5px 14px;
            font-size: 12px;
            font-weight: 600;
            color: #64748B;
            border-radius: 9999px;
            cursor: pointer;
            transition: all 0.2s ease;

            &:hover {
              color: #0F172A;
            }

            &.active {
              background: #FFFFFF;
              color: #2563EB;
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
            }
          }
        }
      }
    }

    .chart-container {
      width: 100%;
      height: 300px;

      .echarts-box {
        width: 100%;
        height: 100%;
      }
    }
  }

  /* 3. 日志明细卡片与表格 */
  .log-section-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 16px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
    overflow: hidden;

    .filter-header-bar {
      padding: 18px 24px;
      border-bottom: 1px solid #F1F5F9;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 14px;

      .title-wrap {
        display: flex;
        align-items: center;
        gap: 10px;

        .sec-title {
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
          margin: 0;
        }

        .sec-count {
          font-size: 12px;
          color: #64748B;
          background: #F8FAFC;
          padding: 2px 8px;
          border-radius: 9999px;
          border: 1px solid #E2E8F0;
        }
      }

      .filter-controls {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .filter-pill-select {
          width: 150px;

          :deep(.el-select__wrapper) {
            border-radius: 9999px;
            font-size: 12.5px;
          }
        }

        .filter-pill-input {
          width: 240px;

          :deep(.el-input__wrapper) {
            border-radius: 9999px;
            font-size: 12.5px;
          }
        }

        .pill-action-btn {
          border-radius: 9999px;
          height: 32px;
          padding: 0 16px;
          font-size: 12.5px;
          font-weight: 600;
          display: inline-flex;
          align-items: center;
          gap: 5px;

          &.primary {
            background: #2563EB;
            border-color: #2563EB;

            &:hover {
              background: #1D4ED8;
            }
          }
        }
      }
    }

    .table-wrap {
      padding: 0 8px;

      .trace-cell {
        display: flex;
        align-items: center;
        gap: 6px;

        .trace-badge {
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 12px;
          font-weight: 600;
          color: #334155;
          background: #F1F5F9;
          padding: 3px 10px;
          border-radius: 9999px;
          border: 1px solid #E2E8F0;
        }

        .copy-btn {
          background: transparent;
          border: none;
          color: #94A3B8;
          cursor: pointer;
          padding: 2px;
          display: flex;
          align-items: center;
          border-radius: 4px;
          transition: all 0.2s;

          &:hover {
            color: #2563EB;
            background: #EFF6FF;
          }
        }
      }

      .user-info-cell {
        display: flex;
        align-items: center;
        gap: 10px;

        .em-user-avatar-sm {
          flex-shrink: 0;
          background: #EFF6FF;
          border: 1px solid #DBEAFE;
        }

        .user-text {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .user-name {
            font-size: 13px;
            font-weight: 600;
            color: #0F172A;
            line-height: 1.2;
          }

          .user-sub {
            display: flex;
            align-items: center;
            gap: 6px;

            .user-account {
              font-size: 11px;
              color: #94A3B8;
              font-family: monospace;
            }
          }
        }
      }

      .role-pill {
        font-size: 10px;
        font-weight: 600;
        padding: 1px 6px;
        border-radius: 9999px;
        line-height: 1.2;

        &.admin { background: #FEE2E2; color: #DC2626; }
        &.teacher { background: #EFF6FF; color: #2563EB; }
        &.student { background: #ECFDF5; color: #059669; }
        &.user { background: #F1F5F9; color: #475569; }
      }

      .scene-tag-wrap {
        .scene-badge {
          display: inline-block;
          font-size: 11.5px;
          font-weight: 600;
          padding: 3px 10px;
          border-radius: 9999px;

          &.badge-chat { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
          &.badge-prep { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
          &.badge-rag { background: #F0FDFA; color: #0D9488; border: 1px solid #99F6E4; }
          &.badge-grading { background: #EEF2FF; color: #4F46E5; border: 1px solid #C7D2FE; }
          &.badge-agent { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
          &.badge-default { background: #F8FAFC; color: #475569; border: 1px solid #E2E8F0; }
        }
      }

      .model-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-family: monospace;
        font-size: 12px;
        color: #334155;
        background: #F8FAFC;
        padding: 4px 12px;
        border-radius: 9999px;
        border: 1px solid #E2E8F0;
        transition: all 0.2s;

        &:hover {
          background: #F1F5F9;
          border-color: #CBD5E1;
        }

        .model-icon {
          font-size: 13px;
          color: #64748B;
        }
      }

      .tokens-cell {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .tokens-main {
          display: flex;
          align-items: baseline;
          gap: 8px;

          .token-sum {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A;
            font-family: monospace;
          }

          .cost-val {
            font-size: 11px;
            font-weight: 600;
            color: #D97706;
            font-family: monospace;
          }
        }

        .tokens-split {
          font-size: 11px;
          color: #94A3B8;
        }
      }

      .latency-badge {
        font-family: monospace;
        font-size: 11.5px;
        font-weight: 700;
        padding: 2px 10px;
        border-radius: 9999px;

        &.lat-fast { background: #ECFDF5; color: #059669; }
        &.lat-normal { background: #EFF6FF; color: #2563EB; }
        &.lat-warning { background: #FFFBEB; color: #D97706; }
        &.lat-danger { background: #FEF2F2; color: #DC2626; }
      }

      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        font-size: 11px;
        font-weight: 700;
        padding: 2px 8px;
        border-radius: 9999px;
        white-space: nowrap;
        flex-shrink: 0;

        .status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        &.success {
          background: #ECFDF5;
          color: #059669;
          .status-dot { background: #10B981; }
        }

        &.timeout {
          background: #FFFBEB;
          color: #D97706;
          .status-dot { background: #F59E0B; }
        }

        &.failed {
          background: #FEF2F2;
          color: #DC2626;
          .status-dot { background: #EF4444; }
        }
      }

      .table-detail-btn {
        background: #EFF6FF;
        border: 1px solid #BFDBFE;
        color: #2563EB;
        padding: 4px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        transition: all 0.2s;

        &:hover {
          background: #2563EB;
          border-color: #2563EB;
          color: #FFFFFF;
        }
      }
    }

    .pagination-footer {
      padding: 14px 20px 6px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      justify-content: flex-start !important;
      align-items: center;

      :deep(.el-pagination) {
        justify-content: flex-start !important;
        display: flex;
        align-items: center;
      }
    }
  }

  /* 4. 详情抽屉 */
  :deep(.audit-detail-drawer) {
    .el-drawer__header {
      margin-bottom: 16px;
      padding: 20px 24px 16px;
      border-bottom: 1px solid #F1F5F9;
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }

    .el-drawer__body {
      padding: 0 24px 24px;
    }
  }

  .drawer-content {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .drawer-hero-card {
      background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
      border: 1px solid #DBEAFE;
      border-radius: 14px;
      padding: 16px 20px;

      .hero-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;

        .trace-title {
          display: flex;
          align-items: center;
          gap: 8px;

          .label { font-size: 12px; color: #64748B; }
          .val { font-family: monospace; font-size: 14px; font-weight: 700; color: #1E293B; }
        }
      }

      .hero-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 10px;

        .hero-item {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .hi-label { font-size: 11.5px; color: #64748B; }
          .hi-val {
            font-size: 17px;
            font-weight: 700;
            font-family: monospace;

            &.text-purple { color: #9333EA; }
            &.text-emerald { color: #059669; }
            &.text-amber { color: #D97706; }
          }
        }
      }
    }

    .detail-section {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 16px 18px;

      .sec-heading {
        font-size: 13.5px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 12px 0;
        display: flex;
        align-items: center;
        gap: 7px;

        &.mb-0 { margin-bottom: 0; }

        .sec-icon {
          font-size: 16px;

          &.icon-blue { color: #2563EB; }
          &.icon-purple { color: #9333EA; }
          &.icon-emerald { color: #059669; }
          &.icon-cyan { color: #0891B2; }
          &.icon-amber { color: #D97706; }
        }
      }

      .info-list {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .info-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12.5px;

          .k { color: #64748B; }
          .v {
            color: #1E293B;
            text-align: right;

            &.font-bold { font-weight: 600; }
            &.monospace { font-family: monospace; }
            &.text-amber { color: #D97706; }
          }

          .user-flex-item {
            display: inline-flex;
            align-items: center;
            gap: 6px;
          }
        }
      }

      .json-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .copy-json-btn {
          background: #F1F5F9;
          border: 1px solid #E2E8F0;
          color: #475569;
          padding: 3px 8px;
          border-radius: 6px;
          font-size: 11.5px;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 4px;

          &:hover {
            background: #E2E8F0;
            color: #0F172A;
          }
        }
      }

      .json-code-block {
        margin: 0;
        background: #0F172A;
        color: #E2E8F0;
        border-radius: 8px;
        padding: 12px 14px;
        font-family: monospace;
        font-size: 11.5px;
        line-height: 1.5;
        max-height: 220px;
        overflow-y: auto;
      }
    }
  }
}
</style>
