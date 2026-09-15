import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyAiUsage } from '@/api/profile/ai-usage';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

export const AI_USAGE_CHANGED_EVENT = 'edumind:ai-usage-changed';
export const AI_USAGE_REFRESH_INTERVAL_MS = 15000;

export const AI_USAGE_TABLE_HEADER_STYLE = {
  background: '#F8FAFC',
  color: '#64748B',
  fontWeight: '600',
  fontSize: '12px',
  borderBottom: '1px solid #EEF2F7'
};

export function formatUsageNumber(value: number) {
  return value.toLocaleString('zh-CN');
}

export function formatUsageDateTime(value: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

export function calcTodayUsagePercent(todayTokensUsed: number, dailyTokenLimit: number) {
  if (!dailyTokenLimit) return 0;
  return Math.min(100, Math.round((todayTokensUsed / dailyTokenLimit) * 100));
}

export function resolveQuotaPillClass(remainingPercent: number) {
  if (remainingPercent >= 50) return 'is-success';
  if (remainingPercent >= 20) return 'is-warning';
  return 'is-danger';
}

export function useAIUsage() {
  const loading = ref(false);
  const logLoading = ref(false);
  const silentLoading = ref(false);
  const lastUpdatedAt = ref<Date | null>(null);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const logTotal = ref(0);
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  const usage = reactive<PersonalAiUsageVO>({
    todayTokensUsed: 0,
    dailyTokenLimit: 100000,
    remainingPercent: 100,
    quotaStatus: '今日额度充足',
    totalQaAndGenerateCalls: 0,
    avgLatencyMs: 0,
    semesterEstimatedCostRMB: 0,
    recentLogs: [],
    totalLogCount: 0,
    logPageNum: 1,
    logPageSize: 10
  });

  const todayUsagePercent = computed(() =>
    calcTodayUsagePercent(usage.todayTokensUsed, usage.dailyTokenLimit)
  );

  const quotaPillClass = computed(() => resolveQuotaPillClass(usage.remainingPercent));

  const lastUpdatedText = computed(() => {
    if (!lastUpdatedAt.value) return '';
    return `更新于 ${lastUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`;
  });

  const loadUsage = async (manual = false, options?: { tableOnly?: boolean }) => {
    if (loading.value || silentLoading.value || logLoading.value) return;
    if (manual) {
      loading.value = true;
    } else if (options?.tableOnly) {
      logLoading.value = true;
    } else if (usage.recentLogs.length > 0) {
      silentLoading.value = true;
    } else {
      logLoading.value = true;
    }
    try {
      const res = await getMyAiUsage({
        logDays: 7,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      });
      if (res?.data) {
        Object.assign(usage, res.data);
        logTotal.value = res.data.totalLogCount ?? 0;
        pageNum.value = res.data.logPageNum ?? pageNum.value;
        pageSize.value = res.data.logPageSize ?? pageSize.value;
        lastUpdatedAt.value = new Date();
      }
    } catch (err) {
      if (manual) {
        ElMessage.error('加载 AI 使用统计失败');
      }
      console.error('[AIUsage] load failed', err);
    } finally {
      loading.value = false;
      silentLoading.value = false;
      logLoading.value = false;
    }
  };

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      loadUsage();
    }
  };

  const handleAiUsageChanged = () => {
    pageNum.value = 1;
    loadUsage();
  };

  const startAutoRefresh = () => {
    refreshTimer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadUsage();
      }
    }, AI_USAGE_REFRESH_INTERVAL_MS);
  };

  onMounted(() => {
    loadUsage(true);
    startAutoRefresh();
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener(AI_USAGE_CHANGED_EVENT, handleAiUsageChanged);
  });

  onUnmounted(() => {
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener(AI_USAGE_CHANGED_EVENT, handleAiUsageChanged);
  });

  return {
    loading,
    logLoading,
    usage,
    pageNum,
    pageSize,
    logTotal,
    todayUsagePercent,
    quotaPillClass,
    lastUpdatedText,
    tableHeaderStyle: AI_USAGE_TABLE_HEADER_STYLE,
    formatNumber: formatUsageNumber,
    formatDateTime: formatUsageDateTime,
    loadUsage
  };
}
