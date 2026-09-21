import { ref, reactive, computed, onMounted, onUnmounted, shallowRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as echarts from 'echarts';
import { USE_MOCK } from '@/config/mock';
import { mockAuditLogs, mockAuditSummary } from '@/mock/audit';
import {
  getAuditSummary as getAuditSummaryRaw,
  getAuditLogs as getAuditLogsRaw,
  getDailyTrend as getDailyTrendRaw,
  listAvailableAiModels
} from '@/api/system/audit';
import { mapAuditLog, mapDailyTrendItem, mapSummary } from '@/utils/system/map-audit';
import {
  pageOperLog,
  getOperLogStats,
  deleteOperLog,
  batchDeleteOperLog,
  cleanOperLog
} from '@/api/system/oper-log';
import type { AIAuditLog, AuditSummaryVO, AuditDailyTrendItem } from '@/types/system/audit';
import type { OperLogVO, OperLogPageQuery, OperLogStatsVO } from '@/types/system/oper-log';
import { normalizeAvatarUrl } from '@/utils/format/file';

// --- Shared pure helpers ---

export function getRoleLabel(role?: string) {
  if (!role) return '用户';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[role.toUpperCase()] || role;
}

export function getSceneLabel(scene?: string) {
  if (!scene) return '智能对话';
  const s = scene.toUpperCase();
  const map: Record<string, string> = {
    CHAT: '智能对话',
    CHAT_RAG: '知识对话',
    PREP: '智能备课',
    LEARNING: '自适应学习辅导',
    MEMORY: '学情记忆沉淀',
    MEMORY_EXTRACT: '学情记忆沉淀',
    RAG: '知识问答',
    KB_RETRIEVAL: '知识库检索',
    GRADING: '作业批改',
    SUBJECTIVE_GRADING: '主观题评阅',
    AGENT: '智能体协作',
    GLOBAL_ASSISTANT: '全局助手',
    QUESTION: '智能出题',
    QUESTION_GENERATE: '智能出题',
    EXAM: '智能组卷',
    EVALUATION: '学情诊断评估',
    TEACHING_ADVICE: '学情教学建议',
    ADVICE: '学情教学建议',
    COURSE_OBJECTIVE: '教学目标生成',
    COURSE_DESCRIPTION: '课程简介生成',
    COURSE_KNOWLEDGE_POINT: '知识点推荐',
    STREAM: '流式对话',
    OCR: 'OCR 识别',
    EMBEDDING: '向量嵌入',
    RERANK: '语义重排'
  };
  const raw = map[s] || scene;
  return raw.replace(/\s*\([A-Za-z0-9_-]+\)\s*$/, '').trim();
}

export function getStatusLabel(status?: string) {
  if (!status) return '未知状态';
  const s = status.toUpperCase();
  const map: Record<string, string> = {
    SUCCESS: '调用成功',
    OK: '调用成功',
    200: '调用成功',
    ERROR: '调用异常',
    FAIL: '调用失败',
    FAILED: '调用失败',
    500: '服务异常',
    CIRCUIT_BREAK: '熔断保护',
    BLOCKED: '限流拦截',
    RATE_LIMIT: '限流拦截',
    TIMEOUT: '响应超时'
  };
  return map[s] || status;
}

export function getSceneStyleClass(scene?: string) {
  const s = (scene || '').toUpperCase();
  if (s === 'CHAT' || s === 'CHAT_RAG' || s === 'LEARNING' || s === 'STREAM') return 'badge-chat';
  if (s === 'PREP') return 'badge-prep';
  if (s === 'RAG' || s === 'KB_RETRIEVAL') return 'badge-rag';
  if (s === 'GRADING' || s === 'SUBJECTIVE_GRADING') return 'badge-grading';
  if (s === 'QUESTION' || s === 'QUESTION_GENERATE' || s === 'EXAM') return 'badge-question';
  if (s === 'AGENT') return 'badge-agent';
  if (s === 'MEMORY' || s === 'MEMORY_EXTRACT') return 'badge-memory';
  if (s === 'TEACHING_ADVICE' || s === 'ADVICE' || s === 'EVALUATION') return 'badge-advice';
  return 'badge-default';
}

export function getLatencyClass(ms: number) {
  if (ms < 300) return 'lat-fast';
  if (ms < 1000) return 'lat-normal';
  if (ms < 3000) return 'lat-warning';
  return 'lat-danger';
}

export function getUserAvatarUrl(row?: AIAuditLog | null): string {
  if (!row) return '';
  if (row.avatar) {
    const normalized = normalizeAvatarUrl(row.avatar);
    if (normalized) return normalized;
  }
  const seed = row.username || `User_${row.userId}`;
  return `https://api.dicebear.com/7.x/bottts/svg?seed=${seed}&backgroundColor=e0e7ff`;
}

export const BUSINESS_TYPE_MAP: Record<number, string> = {
  0: '其它',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '查询',
  5: '导出',
  6: '导入',
  7: '授权/变更',
  8: '清空'
};

export function businessTypeLabel(type: number | undefined): string {
  if (type == null) return '其它';
  return BUSINESS_TYPE_MAP[type] ?? '其它';
}

export function getMethodBadgeClass(method: string | undefined): string {
  const m = (method || '').toUpperCase();
  if (m === 'GET') return 'method--get';
  if (m === 'POST') return 'method--post';
  if (m === 'PUT') return 'method--put';
  if (m === 'DELETE') return 'method--delete';
  return 'method--other';
}

export function formatLocalDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function buildRecentDateRange(days: number): [string, string] {
  const end = new Date();
  end.setHours(0, 0, 0, 0);
  const start = new Date(end);
  start.setDate(start.getDate() - (days - 1));
  return [formatLocalDate(start), formatLocalDate(end)];
}

export function getActionSummary(row: OperLogVO): string {
  const cleanTitle = (t: string | undefined) => (t ? (t.endsWith('管理') ? t.slice(0, -2) : t) : '');
  const t = cleanTitle(row.title);

  if (!row.operParam) {
    if (t) {
      const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
      return `${typeLabel}${t}`;
    }
    return '常规业务处理';
  }

  try {
    const obj = JSON.parse(row.operParam);
    let name = '';
    if (obj && typeof obj === 'object') {
      if (obj.action) {
        return String(obj.action);
      }
      const params = obj.params || obj;
      if (Array.isArray(params)) {
        name = 'ID: ' + params.slice(0, 3).join(', ') + (params.length > 3 ? ' 等' : '');
      } else if (params && typeof params === 'object') {
        name =
          params.username ||
          params.realName ||
          params.nickname ||
          params.name ||
          params.title ||
          params.courseName ||
          params.configName ||
          params.roleName ||
          params.id ||
          '';
        if (typeof name === 'object') {
          name = '';
        }
      } else if (params != null && typeof params !== 'function') {
        name = String(params);
      }
    }
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
    if (t) {
      return `${typeLabel}${t}${name ? `「${name}」` : ''}`;
    }
  } catch {
    // ignore parse errors
  }

  if (t) {
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
    return `${typeLabel}${t}`;
  }
  return '常规业务处理';
}

export function parseDiffItems(operParam?: string): string[] {
  if (!operParam) return [];
  try {
    const obj = JSON.parse(operParam);
    if (obj && typeof obj === 'object' && Array.isArray(obj.diffItems)) {
      return obj.diffItems.map(String);
    }
  } catch {
    // ignore
  }
  return [];
}

export function formatJson(str: string | undefined): string {
  if (!str) return '';
  try {
    const obj = JSON.parse(str);
    if (obj && typeof obj === 'object' && 'params' in obj) {
      return JSON.stringify(obj.params, null, 2);
    }
    return JSON.stringify(obj, null, 2);
  } catch {
    return str;
  }
}

export function createEmptyAuditSummary(): AuditSummaryVO {
  return {
    totalCalls: 0,
    totalTokens: 0,
    totalCostRMB: 0,
    avgLatencyMs: 0,
    successRate: 100,
    dailyTrend: [],
    modelDistribution: []
  };
}

export async function getAvailableAiModels(): Promise<string[]> {
  try {
    const res = await listAvailableAiModels();
    const records = res?.data?.list || [];
    const models = records
      .map((m) => m.modelName || m.modelKey || m.configName)
      .filter((m): m is string => Boolean(m));
    if (models.length > 0) {
      return Array.from(new Set(models));
    }
  } catch (err) {
    console.warn('Failed to load system models', err);
  }
  return USE_MOCK ? ['deepseek-v4-flash'] : [];
}

export async function getAuditSummary(): Promise<AuditSummaryVO> {
  try {
    const res = await getAuditSummaryRaw();
    if (res?.data) {
      return mapSummary(res.data);
    }
    if (USE_MOCK) return mockAuditSummary;
    return createEmptyAuditSummary();
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Audit] Fallback mockAuditSummary', err);
    return mockAuditSummary;
  }
}

export async function getDailyTrend(days = 7): Promise<AuditDailyTrendItem[]> {
  try {
    const res = await getDailyTrendRaw(days);
    if (res?.data) {
      return res.data.map((item) => mapDailyTrendItem(item));
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
  }
  return [];
}

export async function getAuditLogs(
  params?: Record<string, unknown>
): Promise<{ list: AIAuditLog[]; total: number }> {
  try {
    const res = await getAuditLogsRaw(params);
    if (res?.data?.list) {
      return {
        list: res.data.list.map(mapAuditLog),
        total: res.data.total ?? res.data.list.length
      };
    }
    if (!USE_MOCK) return { list: [], total: 0 };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Audit] Fallback mockAuditLogs', err);
  }
  return USE_MOCK ? { list: mockAuditLogs, total: 0 } : { list: [], total: 0 };
}

// --- Shared pure helpers ---

export function useAudit() {
  const summary = ref<AuditSummaryVO>(createEmptyAuditSummary());
  const trendDays = ref(7);
  const trendLoading = ref(false);
  const trendChartRef = ref<HTMLElement | null>(null);
  const chartInstance = shallowRef<echarts.ECharts | null>(null);

  const logs = ref<AIAuditLog[]>([]);
  const tableLoading = ref(false);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  const sceneFilter = ref('');
  const modelFilter = ref('');
  const statusFilter = ref('');
  const searchKeyword = ref('');
  const modelOptions = ref<string[]>([]);

  const detailDrawerVisible = ref(false);
  const selectedLog = ref<AIAuditLog | null>(null);

  function handleResize() {
    chartInstance.value?.resize();
  }

  async function loadModels() {
    try {
      const models = await getAvailableAiModels();
      modelOptions.value = models && models.length > 0 ? models : [];
    } catch {
      modelOptions.value = [];
    }
  }

  async function loadSummary() {
    try {
      summary.value = await getAuditSummary();
    } catch {
      summary.value = createEmptyAuditSummary();
      ElMessage.warning('审计摘要加载失败');
    }
  }

  async function changeTrendDays(days: number) {
    trendDays.value = days;
    await loadTrendData(days);
  }

  async function loadTrendData(days: number) {
    trendLoading.value = true;
    try {
      const trendData = await getDailyTrend(days);
      renderTrendChart(trendData);
    } catch {
      // ignore
    } finally {
      trendLoading.value = false;
    }
  }

  function renderTrendChart(trendData: AuditDailyTrendItem[]) {
    if (!trendChartRef.value) return;

    if (!chartInstance.value) {
      chartInstance.value = echarts.init(trendChartRef.value);
    }

    const dates = trendData.map((d) => d.date.slice(5));
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
        formatter: (params: unknown) => {
          const items = params as Array<{ dataIndex: number }>;
          if (!items || !items.length) return '';
          const idx = items[0].dataIndex;
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
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
      logs.value = res.list;
      total.value = res.total;

      const logModels = res.list.map((l) => l.model).filter(Boolean);
      if (logModels.length > 0) {
        modelOptions.value = Array.from(new Set([...modelOptions.value, ...logModels]));
      }
    } catch {
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

  function viewDetail(row: AIAuditLog) {
    selectedLog.value = row;
    detailDrawerVisible.value = true;
  }

  async function copyText(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success('已复制到剪贴板');
    } catch {
      ElMessage.error('复制失败，请手动选取复制');
    }
  }

  onMounted(async () => {
    await Promise.all([loadSummary(), loadTrendData(trendDays.value), loadLogs(), loadModels()]);
    window.addEventListener('resize', handleResize);
  });

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    chartInstance.value?.dispose();
    chartInstance.value = null;
  });

  return {
    summary,
    trendDays,
    trendLoading,
    trendChartRef,
    logs,
    tableLoading,
    pageNum,
    pageSize,
    total,
    sceneFilter,
    modelFilter,
    statusFilter,
    searchKeyword,
    modelOptions,
    detailDrawerVisible,
    selectedLog,
    loadSummary,
    changeTrendDays,
    loadTrendData,
    loadLogs,
    handleSearch,
    handleReset,
    viewDetail,
    copyText,
    getRoleLabel,
    getSceneLabel,
    getStatusLabel,
    getSceneStyleClass,
    getLatencyClass,
    getUserAvatarUrl
  };
}

// --- Operation Log composable ---

export const OPER_LOG_DATE_SHORTCUTS = [
  {
    text: '今天',
    value: () => {
      const today = formatLocalDate(new Date());
      return [today, today] as [string, string];
    }
  },
  {
    text: '近7天',
    value: () => buildRecentDateRange(7)
  },
  {
    text: '近30天',
    value: () => buildRecentDateRange(30)
  }
];

export function syncDateRangeToQuery(
  query: OperLogPageQuery,
  range: [string, string] | null
) {
  if (range && range.length === 2) {
    query.startTime = range[0] + ' 00:00:00';
    query.endTime = range[1] + ' 23:59:59';
  } else {
    query.startTime = undefined;
    query.endTime = undefined;
  }
}

export function useOperLog() {
  const loading = ref(false);
  const total = ref(0);
  const tableData = ref<OperLogVO[]>([]);
  const selectedRowIds = ref<number[]>([]);
  const detailVisible = ref(false);
  const detail = ref<Partial<OperLogVO>>({});

  const dateRange = ref<[string, string] | null>(buildRecentDateRange(7));

  const stats = reactive<OperLogStatsVO>({
    totalCount: 0,
    todayCount: 0,
    successRate: 100,
    errorCount: 0,
    avgCostTime: 0
  });

  const queryParams = reactive<OperLogPageQuery>({
    pageNo: 1,
    pageSize: 10,
    title: '',
    operName: '',
    businessType: null,
    status: null,
    startTime: undefined,
    endTime: undefined
  });

  syncDateRangeToQuery(queryParams, dateRange.value);

  const parsedAction = computed(() => {
    if (!detail.value) return '';
    return getActionSummary(detail.value as OperLogVO);
  });

  const parsedDiffItems = computed<string[]>(() => parseDiffItems(detail.value?.operParam));

  function handleDateRangeChange(range: [string, string] | null) {
    syncDateRangeToQuery(queryParams, range);
  }

  async function loadStats() {
    try {
      const res = await getOperLogStats();
      if (res.data) {
        stats.totalCount = res.data.totalCount || 0;
        stats.todayCount = res.data.todayCount || 0;
        stats.successRate = res.data.successRate ?? 100;
        stats.errorCount = res.data.errorCount || 0;
        stats.avgCostTime = res.data.avgCostTime || 0;
      }
    } catch {
      // ignore
    }
  }

  async function loadData() {
    loading.value = true;
    try {
      const [res] = await Promise.all([
        pageOperLog({
          pageNo: queryParams.pageNo,
          pageSize: queryParams.pageSize,
          title: queryParams.title || undefined,
          operName: queryParams.operName || undefined,
          businessType: queryParams.businessType,
          status: queryParams.status,
          startTime: queryParams.startTime,
          endTime: queryParams.endTime
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
      tableData.value = res.data?.list || [];
      total.value = Number(res.data?.total) || 0;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '加载操作日志失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1;
    loadData();
    loadStats();
  }

  function resetQuery() {
    queryParams.title = '';
    queryParams.operName = '';
    queryParams.businessType = null;
    queryParams.status = null;
    dateRange.value = buildRecentDateRange(7);
    syncDateRangeToQuery(queryParams, dateRange.value);
    handleQuery();
  }

  function handleSelectionChange(rows: OperLogVO[]) {
    selectedRowIds.value = rows.map((r) => r.id);
  }

  function openDetail(row: OperLogVO) {
    detail.value = { ...row };
    detailVisible.value = true;
  }

  function copyText(text: string) {
    if (!text) return;
    navigator.clipboard.writeText(text).then(
      () => ElMessage.success('已复制到剪贴板'),
      () => ElMessage.error('复制失败')
    );
  }

  async function handleDelete(row: OperLogVO) {
    try {
      await ElMessageBox.confirm(`确定要删除 ID 为 #${row.id} 的操作日志记录吗？`, '删除日志确认', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      });
      await deleteOperLog(row.id);
      ElMessage.success('操作日志已成功删除');
      loadData();
      loadStats();
    } catch (err: unknown) {
      if (err !== 'cancel' && err !== 'close') {
        const e = err as { response?: { data?: { message?: string } }; message?: string };
        ElMessage.error(e?.response?.data?.message || e?.message || '删除日志失败，请检查操作权限');
      }
    }
  }

  async function handleBatchDelete() {
    if (selectedRowIds.value.length === 0) return;
    try {
      await ElMessageBox.confirm(
        `确定要批量删除选中的 ${selectedRowIds.value.length} 条操作日志吗？`,
        '批量删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await batchDeleteOperLog(selectedRowIds.value);
      ElMessage.success(`已成功批量删除 ${selectedRowIds.value.length} 条日志`);
      selectedRowIds.value = [];
      loadData();
      loadStats();
    } catch (err: unknown) {
      if (err !== 'cancel' && err !== 'close') {
        const e = err as { response?: { data?: { message?: string } }; message?: string };
        ElMessage.error(e?.response?.data?.message || e?.message || '批量删除日志失败，请检查操作权限');
      }
    }
  }

  async function handleClean() {
    try {
      await ElMessageBox.confirm(
        '确定要清空平台当前租户下的全部业务操作日志吗？此操作不可逆！',
        '清空操作日志警告',
        {
          confirmButtonText: '确认清空',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await cleanOperLog();
      ElMessage.success('当前租户操作日志已全部清空');
      loadData();
      loadStats();
    } catch (err: unknown) {
      if (err !== 'cancel' && err !== 'close') {
        const e = err as { response?: { data?: { message?: string } }; message?: string };
        ElMessage.error(e?.response?.data?.message || e?.message || '清空日志失败，请检查操作权限');
      }
    }
  }

  function exportCsv() {
    if (tableData.value.length === 0) {
      ElMessage.warning('当前无数据可导出');
      return;
    }
    const headers = ['ID', '模块', '业务类型', '操作摘要', '方式', '操作人员', '客户端IP', '状态', '耗时(ms)', '操作时间'];
    const rows = tableData.value.map((r) => [
      r.id,
      `"${(r.title || '').replace(/"/g, '""')}"`,
      `"${businessTypeLabel(r.businessType)}"`,
      `"${getActionSummary(r).replace(/"/g, '""')}"`,
      r.requestMethod || 'POST',
      `"${(r.operName || '').replace(/"/g, '""')}"`,
      r.operIp || '',
      r.status === 0 ? '成功' : '失败',
      r.costTime || 0,
      r.operTime || ''
    ]);
    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `EduMind_OperLog_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    ElMessage.success('操作日志已成功导出');
  }

  onMounted(() => {
    loadStats();
    loadData();
  });

  return {
    loading,
    total,
    tableData,
    selectedRowIds,
    detailVisible,
    detail,
    dateRange,
    dateRangeShortcuts: OPER_LOG_DATE_SHORTCUTS,
    stats,
    queryParams,
    parsedAction,
    parsedDiffItems,
    handleDateRangeChange,
    loadStats,
    loadData,
    handleQuery,
    resetQuery,
    handleSelectionChange,
    openDetail,
    copyText,
    handleDelete,
    handleBatchDelete,
    handleClean,
    exportCsv,
    businessTypeLabel,
    getMethodBadgeClass,
    getActionSummary,
    formatJson
  };
}
