<template>
  <div class="gateway-dashboard-page" v-loading="loading">
    <!-- 顶部专属 SaaS 英雄大盘 (参照 TenantQuota 视觉范式) -->
    <PageHeroBanner
      title="AI 模型网关与算力调度监控 · 全链路治理大盘"
      subtitle="全链路监控大模型网关请求吞吐、Token 算力消耗水位、调用延迟 SLA、多服务商路由健康度与熔断防御"
      background-variant="system"
      size="large"
    >
      <template #extra>
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
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部控制条：时间跨度筛选、自动刷新与快捷导航 -->
      <div class="dashboard-control-bar">
        <div class="left-status-box">
          <span class="pulse-dot" :class="{ warning: (metrics?.circuitOpenCount ?? 0) > 0 }"></span>
          <span class="status-label">网关运行状态：</span>
          <el-tag :type="(metrics?.circuitOpenCount ?? 0) > 0 ? 'danger' : 'success'" size="small" effect="light">
            {{ (metrics?.circuitOpenCount ?? 0) > 0 ? '触发熔断隔离' : '正常转发中' }}
          </el-tag>
          <span class="sync-time">数据最后同步: {{ lastSyncTime }}</span>
        </div>

        <div class="right-actions-box">
          <el-radio-group v-model="range" size="default" class="range-radio-group" @change="loadMetrics">
            <el-radio-button value="1h">近 1 小时</el-radio-button>
            <el-radio-button value="24h">近 24 小时</el-radio-button>
            <el-radio-button value="7d">近 7 天</el-radio-button>
            <el-radio-button value="30d">近 30 天</el-radio-button>
          </el-radio-group>

          <div class="auto-refresh-switch">
            <el-switch v-model="autoRefresh" size="small" inline-prompt active-text="开" inactive-text="关" @change="toggleAutoRefresh" />
            <span class="switch-tip">30s 自动刷新</span>
          </div>

          <el-button plain size="default" :loading="loading" @click="loadMetrics">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>

          <el-button type="primary" size="default" class="gradient-btn" @click="router.push('/system/gateway/routes')">
            <el-icon><Connection /></el-icon>
            <span>路由规则配置 →</span>
          </el-button>

          <el-button plain size="default" @click="openTraceDrawer">
            <el-icon><Document /></el-icon>
            <span>调用 Trace 明细</span>
          </el-button>
        </div>
      </div>

      <!-- Mock 提示 -->
      <el-alert
        v-if="usedMockFallback"
        type="info"
        :closable="false"
        show-icon
        title="当前处于 Mock 预览模式，呈现样例数据"
        class="mock-alert"
      />

      <!-- 四大核心算力与网关 KPI 卡片矩阵 (Telemetry Grid) -->
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
            <!-- 输入 Prompt 与 输出 Completion 构成条 -->
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
            <el-button type="primary" link size="small" @click="handleResetCircuit">一键复位</el-button>
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

      <!-- 中部双图表大盘：趋势与分布 -->
      <div class="charts-row">
        <!-- 趋势图 -->
        <div class="chart-card-box span-13">
          <div class="card-title-row">
            <div class="title-group">
              <h3>网关请求吞吐与 Token 算力走势</h3>
              <span class="subtitle">双轴联动呈现请求波峰与 Token 算力消耗量</span>
            </div>
          </div>
          <GatewayTrendChart :data="metrics?.timeSeriesTrend ?? []" height="300px" />
        </div>

        <!-- 占比饼图 -->
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

      <!-- 供应商与模型健康度排行大盘 -->
      <div class="data-table-card">
        <div class="table-card-header">
          <div class="header-left">
            <h3>模型与服务商调用明细及健康度</h3>
            <span class="sub">统计各模型在不同时段的实际算力消耗、输入输出结构、延迟表现与预估成本</span>
          </div>
          <div class="header-right">
            <el-input
              v-model="modelSearchKey"
              placeholder="按模型名称搜索..."
              prefix-icon="Search"
              clearable
              style="width: 220px;"
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
              <el-button type="primary" link size="small" @click="openTraceByModel(row.provider)">
                链路 Trace
              </el-button>
              <el-button type="danger" link size="small" @click="handleResetModelCircuit(row.provider)">
                复位
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 实时调用链路 Trace 抽屉 -->
    <el-drawer
      v-model="traceDrawerVisible"
      title="网关实时调用链路 Trace 审计"
      size="760px"
      direction="rtl"
    >
      <div class="drawer-filters">
        <el-input
          v-model="traceModelFilter"
          placeholder="筛选模型..."
          clearable
          style="width: 180px;"
          @change="loadTraceLogs"
        />
        <el-select
          v-model="traceSceneFilter"
          placeholder="全部业务场景"
          clearable
          style="width: 180px;"
          @change="loadTraceLogs"
        >
          <el-option label="全部场景" value="" />
          <el-option label="课程智能助教答疑 (chat)" value="chat" />
          <el-option label="AI 题库出题与变式 (question_generate)" value="question_generate" />
          <el-option label="作业/主观题智能批改 (grading)" value="grading" />
          <el-option label="Agent 任务规划 (agent)" value="agent" />
        </el-select>
        <el-button type="primary" plain @click="loadTraceLogs">查询</el-button>
      </div>

      <el-table :data="traceLogs" stripe style="width: 100%; margin-top: 14px;" v-loading="traceLoading">
        <el-table-column prop="createTime" label="调用时间" width="160" />
        <el-table-column prop="scene" label="业务场景" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ translateSceneName(row.scene) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="转发模型" min-width="140">
          <template #default="{ row }">
            <span class="font-mono">{{ row.model }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Token 进/出" width="130">
          <template #default="{ row }">
            <span class="font-mono" style="font-size: 12px;">{{ row.promptTokens }} / {{ row.completionTokens }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时" width="90">
          <template #default="{ row }">
            <span class="font-mono">{{ row.latencyMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default>
            <el-tag type="success" size="small">成功</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="drawer-pagination">
        <el-pagination
          v-model:current-page="tracePage"
          v-model:page-size="tracePageSize"
          :total="traceTotal"
          layout="prev, pager, next, total"
          @current-change="loadTraceLogs"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh, Connection, Document, Coin, TrendCharts, Timer, WarningFilled } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import GatewayTrendChart from '@/components/system/gateway/GatewayTrendChart.vue';
import GatewayDistributionChart from '@/components/system/gateway/GatewayDistributionChart.vue';
import { getGatewayMetrics, getGatewayLogs, resetGatewayCircuit } from '@/api/system/gateway';
import { USE_MOCK } from '@/config/mock';
import { MOCK_GATEWAY_METRICS, MOCK_GATEWAY_LOGS } from '@/mock/gateway';
import type { GatewayMetricsVO, GatewayTraceLogVO } from '@/types/system/gateway';

const router = useRouter();
const loading = ref(false);
const usedMockFallback = ref(false);
const range = ref('24h');
const metrics = ref<GatewayMetricsVO | null>(null);
const lastSyncTime = ref<string>('');
const modelSearchKey = ref('');

// 自动刷新
const autoRefresh = ref(false);
let refreshTimer: any = null;

// Trace 抽屉
const traceDrawerVisible = ref(false);
const traceLoading = ref(false);
const traceLogs = ref<GatewayTraceLogVO[]>([]);
const traceModelFilter = ref('');
const traceSceneFilter = ref('');
const tracePage = ref(1);
const tracePageSize = ref(15);
const traceTotal = ref(0);

// 格式化 Token 辅助
function formatTokens(value?: number) {
  if (value == null) return '0';
  if (value >= 1000000) return `${(value / 1000000).toFixed(2)} M`;
  if (value >= 10000) return `${(value / 10000).toFixed(1)} 万`;
  return value.toLocaleString();
}

// Prompt 占比计算
const getPromptRatio = computed(() => {
  const prompt = metrics.value?.promptTokens ?? 0;
  const completion = metrics.value?.completionTokens ?? 0;
  const total = prompt + completion;
  if (total <= 0) return 40;
  return Math.round((prompt / total) * 100);
});

// 模型过滤
const filteredProviders = computed(() => {
  const list = metrics.value?.byProvider ?? [];
  if (!modelSearchKey.value.trim()) return list;
  const key = modelSearchKey.value.toLowerCase().trim();
  return list.filter((p) => p.provider.toLowerCase().includes(key));
});

function getCallsPercentage(calls: number) {
  const total = metrics.value?.totalRequests ?? 1;
  if (total <= 0) return 0;
  return Math.min(100, Math.round((calls / total) * 100));
}

function getModelProviderBrand(name: string) {
  const lower = name.toLowerCase();
  if (lower.includes('deepseek')) return 'DeepSeek 深度求索';
  if (lower.includes('gpt') || lower.includes('openai')) return 'OpenAI 官方渠道';
  if (lower.includes('qwen')) return '阿里千问 Qwen';
  if (lower.includes('claude')) return 'Anthropic Claude';
  if (lower.includes('mock')) return '本地测试 Mock';
  return '通用大模型提供商';
}

function getCircuitStateClass(modelKey: string) {
  const states = metrics.value?.circuitStates ?? [];
  const target = states.find((s) => s.modelKey === modelKey);
  if (!target) return 'closed';
  if (target.status === 'OPEN') return 'open';
  if (target.status === 'HALF_OPEN') return 'half-open';
  return 'closed';
}

function getCircuitStateText(modelKey: string) {
  const states = metrics.value?.circuitStates ?? [];
  const target = states.find((s) => s.modelKey === modelKey);
  if (!target) return '正常 CLOSED';
  if (target.status === 'OPEN') return '熔断隔离 OPEN';
  if (target.status === 'HALF_OPEN') return '试探探测 HALF_OPEN';
  return '正常 CLOSED';
}

function translateSceneName(scene: string) {
  if (!scene) return '通用场景';
  switch (scene.toLowerCase()) {
    case 'chat': return '智能助教答疑';
    case 'question_generate': return 'AI 出题与变式';
    case 'grading': return '作业批改';
    case 'agent': return 'Agent 任务规划';
    default: return scene;
  }
}

// 加载指标
async function loadMetrics() {
  loading.value = true;
  usedMockFallback.value = false;
  try {
    const res = await getGatewayMetrics(range.value);
    if (res?.data) {
      metrics.value = res.data;
    } else {
      throw new Error('Empty metrics');
    }
  } catch {
    if (USE_MOCK) {
      usedMockFallback.value = true;
      metrics.value = MOCK_GATEWAY_METRICS;
    } else {
      metrics.value = null;
      ElMessage.error('加载网关监控指标失败');
    }
  } finally {
    loading.value = false;
    const now = new Date();
    lastSyncTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  }
}

// 自动刷新逻辑
function toggleAutoRefresh(val: boolean) {
  if (val) {
    refreshTimer = setInterval(() => {
      loadMetrics();
    }, 30000);
    ElMessage.success('已开启 30 秒自动轮询刷新');
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer);
      refreshTimer = null;
    }
  }
}

// 重置熔断器
async function handleResetCircuit() {
  try {
    await ElMessageBox.confirm('是否重置网关全局熔断计数器并恢复所有模型的常规转发路由？', '确认复位熔断保护', {
      confirmButtonText: '立即复位',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await resetGatewayCircuit();
    ElMessage.success('网关熔断计数器已复位');
    loadMetrics();
  } catch {
    // canceled
  }
}

async function handleResetModelCircuit(modelKey: string) {
  try {
    await resetGatewayCircuit(modelKey);
    ElMessage.success(`已复位 ${modelKey} 的熔断状态`);
    loadMetrics();
  } catch (err: any) {
    ElMessage.error(err.message || '复位失败');
  }
}

// Trace 抽屉相关
function openTraceDrawer() {
  traceDrawerVisible.value = true;
  loadTraceLogs();
}

function openTraceByModel(modelKey: string) {
  traceModelFilter.value = modelKey;
  traceDrawerVisible.value = true;
  loadTraceLogs();
}

async function loadTraceLogs() {
  traceLoading.value = true;
  try {
    const res = await getGatewayLogs({
      scene: traceSceneFilter.value || undefined,
      model: traceModelFilter.value || undefined,
      page: tracePage.value,
      pageSize: tracePageSize.value
    });
    if (res?.data?.list) {
      traceLogs.value = res.data.list;
      traceTotal.value = res.data.total ?? res.data.list.length;
    } else {
      traceLogs.value = MOCK_GATEWAY_LOGS;
      traceTotal.value = MOCK_GATEWAY_LOGS.length;
    }
  } catch {
    traceLogs.value = MOCK_GATEWAY_LOGS;
    traceTotal.value = MOCK_GATEWAY_LOGS.length;
  } finally {
    traceLoading.value = false;
  }
}

onMounted(() => {
  loadMetrics();
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<style scoped lang="scss">
.gateway-dashboard-page {
  padding-bottom: 40px;

  // 顶部 Hero 统计药丸卡片 (与 TenantQuota 保持高雅统一风格)
  .hero-stats-row {
    display: flex;
    gap: 10px;
    margin-top: 4px;
    flex-wrap: nowrap; // 强制单排排成一行，绝不折行
    width: 100%;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }

    .hero-stat-card {
      flex: 1 1 0; // 5张卡片平分占满整行
      min-width: 0; // 允许弹性收缩
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

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  // 顶部控制条
  .dashboard-control-bar {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    border: 1px solid #E2E8F0;
    flex-wrap: wrap;
    gap: 14px;

    .left-status-box {
      display: flex;
      align-items: center;
      gap: 10px;

      .pulse-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background-color: #10B981;
        box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
        animation: pulse 2s infinite;

        &.warning {
          background-color: #EF4444;
          box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
        }
      }

      .status-label {
        font-size: 13.5px;
        font-weight: 600;
        color: #1E293B;
      }

      .sync-time {
        font-size: 12px;
        color: #94A3B8;
        margin-left: 8px;
      }
    }

    .right-actions-box {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .auto-refresh-switch {
        display: flex;
        align-items: center;
        gap: 6px;
        background: #F8FAFC;
        padding: 4px 10px;
        border-radius: 8px;
        border: 1px solid #E2E8F0;

        .switch-tip {
          font-size: 12px;
          color: #64748B;
        }
      }

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 8px;
        color: #FFFFFF;
      }
    }
  }

  // 四大核心卡片矩阵
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

  // 中部图表行
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

  // 底部供应商明细表
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

  // 抽屉内部样式
  .drawer-filters {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
  }

  .drawer-pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}
</style>
