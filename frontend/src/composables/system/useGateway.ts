import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getGatewayMetrics, getGatewayLogs, resetGatewayCircuit, listGatewayRoutes, updateGatewayRoutes } from '@/api/system/gateway';
import { resolveModels } from '@/composables/system/useAIModel';
import { USE_MOCK } from '@/config/mock';
import { MOCK_GATEWAY_METRICS, MOCK_GATEWAY_LOGS, MOCK_GATEWAY_ROUTES } from '@/mock/gateway';
import type { GatewayMetricsVO, GatewayTraceLogVO, GatewayRouteVO } from '@/types/system/gateway';

export const GATEWAY_RANGE_OPTIONS = [
  { label: '近 1 小时', value: '1h' },
  { label: '近 24 小时', value: '24h' },
  { label: '近 7 天', value: '7d' },
  { label: '近 30 天', value: '30d' }
];

export interface ModelOption {
  label: string;
  value: string;
}

export function formatTokens(value?: number) {
  if (value == null) return '0';
  if (value >= 1000000) return `${(value / 1000000).toFixed(2)} M`;
  if (value >= 10000) return `${(value / 10000).toFixed(1)} 万`;
  return value.toLocaleString();
}

export function computePromptRatio(prompt?: number, completion?: number) {
  const p = prompt ?? 0;
  const c = completion ?? 0;
  const total = p + c;
  if (total <= 0) return 40;
  return Math.round((p / total) * 100);
}

export function filterProviders<T extends { provider: string }>(list: T[], searchKey: string): T[] {
  if (!searchKey.trim()) return list;
  const key = searchKey.toLowerCase().trim();
  return list.filter((p) => p.provider.toLowerCase().includes(key));
}

export function getCallsPercentage(calls: number, totalRequests?: number) {
  const total = totalRequests ?? 1;
  if (total <= 0) return 0;
  return Math.min(100, Math.round((calls / total) * 100));
}

export function getModelProviderBrand(name: string) {
  const lower = name.toLowerCase();
  if (lower.includes('deepseek')) return 'DeepSeek 深度求索';
  if (lower.includes('gpt') || lower.includes('openai')) return 'OpenAI 官方渠道';
  if (lower.includes('qwen')) return '阿里千问 Qwen';
  if (lower.includes('claude')) return 'Anthropic Claude';
  if (lower.includes('mock')) return '本地测试 Mock';
  return '通用大模型提供商';
}

export function getCircuitStateClass(
  modelKey: string,
  states: Array<{ modelKey: string; status: string }> = []
) {
  const target = states.find((s) => s.modelKey === modelKey);
  if (!target) return 'closed';
  if (target.status === 'OPEN') return 'open';
  if (target.status === 'HALF_OPEN') return 'half-open';
  return 'closed';
}

export function getCircuitStateText(
  modelKey: string,
  states: Array<{ modelKey: string; status: string }> = []
) {
  const target = states.find((s) => s.modelKey === modelKey);
  if (!target) return '正常 CLOSED';
  if (target.status === 'OPEN') return '熔断隔离 OPEN';
  if (target.status === 'HALF_OPEN') return '试探探测 HALF_OPEN';
  return '正常 CLOSED';
}

export function translateSceneName(scene: string) {
  if (!scene) return '通用场景';
  switch (scene.toLowerCase()) {
    case 'chat':
      return '智能助教答疑';
    case 'question_generate':
      return 'AI 出题与变式';
    case 'grading':
      return '作业批改';
    case 'agent':
      return 'Agent 任务规划';
    default:
      return scene;
  }
}

export function getSceneName(scene: string) {
  if (!scene) return '通用场景';
  switch (scene.toLowerCase()) {
    case 'chat':
      return '课程智能助教答疑';
    case 'rag':
    case 'chat_rag':
      return '课程知识库问答';
    case 'question':
    case 'question_generate':
      return 'AI 题库出题与变式';
    case 'grading':
      return '作业/主观题智能批改';
    case 'agent':
      return 'Agent 多步任务规划';
    case 'embedding':
      return '向量知识库切片嵌入';
    case 'stream':
      return '流式长文本启发对话';
    default:
      return scene;
  }
}

export function buildModelOption(model: {
  name?: string;
  modelKey?: string;
  modelName?: string;
}): ModelOption {
  const configName = model.name || model.modelKey || model.modelName || '未命名模型';
  const modelName = model.modelName || configName;
  const label = modelName !== configName ? `${configName} · ${modelName}` : configName;
  return {
    label,
    value: model.modelKey || model.name || modelName
  };
}

export function useGateway() {
  const router = useRouter();
  const loading = ref(false);
  const usedMockFallback = ref(false);
  const range = ref('24h');
  const metrics = ref<GatewayMetricsVO | null>(null);
  const lastSyncTime = ref<string>('');
  const modelSearchKey = ref('');

  const autoRefresh = ref(false);
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  const traceDrawerVisible = ref(false);
  const traceLoading = ref(false);
  const traceLogs = ref<GatewayTraceLogVO[]>([]);
  const traceModelFilter = ref('');
  const traceSceneFilter = ref('');
  const tracePage = ref(1);
  const tracePageSize = ref(15);
  const traceTotal = ref(0);

  const getPromptRatio = computed(() =>
    computePromptRatio(metrics.value?.promptTokens, metrics.value?.completionTokens)
  );

  const filteredProviders = computed(() =>
    filterProviders(metrics.value?.byProvider ?? [], modelSearchKey.value)
  );

  function getCircuitClass(modelKey: string) {
    return getCircuitStateClass(modelKey, metrics.value?.circuitStates ?? []);
  }

  function getCircuitText(modelKey: string) {
    return getCircuitStateText(modelKey, metrics.value?.circuitStates ?? []);
  }

  function getProviderCallsPercentage(calls: number) {
    return getCallsPercentage(calls, metrics.value?.totalRequests);
  }

  function selectRange(value: string) {
    if (range.value === value) return;
    range.value = value;
    loadMetrics();
  }

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

  function toggleAutoRefresh(val: boolean) {
    if (val) {
      refreshTimer = setInterval(() => {
        loadMetrics();
      }, 30000);
      ElMessage.success('已开启 30 秒自动轮询刷新');
    } else if (refreshTimer) {
      clearInterval(refreshTimer);
      refreshTimer = null;
    }
  }

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
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '复位失败';
      ElMessage.error(message);
    }
  }

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

  return {
    router,
    loading,
    usedMockFallback,
    range,
    rangeOptions: GATEWAY_RANGE_OPTIONS,
    metrics,
    lastSyncTime,
    modelSearchKey,
    autoRefresh,
    traceDrawerVisible,
    traceLoading,
    traceLogs,
    traceModelFilter,
    traceSceneFilter,
    tracePage,
    tracePageSize,
    traceTotal,
    getPromptRatio,
    filteredProviders,
    formatTokens,
    getCallsPercentage: getProviderCallsPercentage,
    getModelProviderBrand,
    getCircuitStateClass: getCircuitClass,
    getCircuitStateText: getCircuitText,
    translateSceneName,
    selectRange,
    loadMetrics,
    toggleAutoRefresh,
    handleResetCircuit,
    handleResetModelCircuit,
    openTraceDrawer,
    openTraceByModel,
    loadTraceLogs
  };
}

export function useGatewayRoutes() {
  const router = useRouter();
  const loading = ref(false);
  const saving = ref(false);
  const usedMockFallback = ref(false);
  const routes = ref<GatewayRouteVO[]>([]);
  const modelOptions = ref<ModelOption[]>([]);

  async function loadData() {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const models = await resolveModels();
      const opts: ModelOption[] = (models || [])
        .filter((m) => (m.configType === 'chat' || !m.configType) && m.status !== 'disabled')
        .map((m) => buildModelOption(m));

      const knownValues = new Set(opts.map((o) => o.value));
      if (!knownValues.has('mock')) {
        opts.push({ label: 'mock', value: 'mock' });
      }
      modelOptions.value = opts;

      const res = await listGatewayRoutes();
      routes.value = res.data ?? [];
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        routes.value = MOCK_GATEWAY_ROUTES.map((item) => ({ ...item }));
        if (modelOptions.value.length === 0) {
          modelOptions.value = [
            buildModelOption({ name: 'Flash', modelKey: 'Flash', modelName: 'deepseek-v4-flash' }),
            buildModelOption({ name: 'deepseek-chat', modelKey: 'deepseek-chat', modelName: 'deepseek-chat' }),
            { label: 'mock', value: 'mock' }
          ];
        }
      } else {
        routes.value = [];
        ElMessage.error('加载网关路由规则失败');
      }
    } finally {
      loading.value = false;
    }
  }

  async function saveRoutes() {
    saving.value = true;
    try {
      await updateGatewayRoutes(routes.value);
      ElMessage.success('网关路由调度规则已成功保存并立即生效');
      usedMockFallback.value = false;
    } catch {
      if (USE_MOCK) {
        ElMessage.success('Mock 模式：配置已本地保存');
      } else {
        ElMessage.error('保存路由规则失败');
      }
    } finally {
      saving.value = false;
    }
  }

  onMounted(loadData);

  return {
    router,
    loading,
    saving,
    usedMockFallback,
    routes,
    modelOptions,
    getSceneName,
    loadData,
    saveRoutes
  };
}
