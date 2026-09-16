import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyAiUsage } from '@/api/profile/ai-usage';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

export const AI_USAGE_CHANGED_EVENT = 'edumind:ai-usage-changed';
export const AI_CONFIG_CHANGED_EVENT = 'edumind:ai-config-changed';
export const AI_USAGE_REFRESH_INTERVAL_MS = 15000;

export function resolveQuotaStatusText(remainingPercent: number) {
  if (remainingPercent >= 50) return '今日额度充足';
  if (remainingPercent >= 20) return '今日额度紧张';
  if (remainingPercent > 0) return '今日额度即将用尽';
  return '今日额度已用尽';
}

function getInitialDailyTokenLimit(): number {
  if (typeof window !== 'undefined' && window.localStorage) {
    const cached = Number(localStorage.getItem('edumind_sys_ai_tokens_per_user_daily'));
    if (cached > 0) return cached;
  }
  return 100000;
}

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

export type AiUsageTimePeriod = 'today' | '7d' | '30d' | 'all';

export function useAIUsage() {
  const loading = ref(false);
  const logLoading = ref(false);
  const silentLoading = ref(false);
  const lastUpdatedAt = ref<Date | null>(null);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const logTotal = ref(0);
  const selectedPeriod = ref<AiUsageTimePeriod>('today');
  const selectedLogDays = ref<number>(7);
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  const usage = reactive<PersonalAiUsageVO>({
    todayTokensUsed: 0,
    weekTokensUsed: 0,
    monthTokensUsed: 0,
    totalTokensUsed: 0,
    todayCalls: 0,
    weekCalls: 0,
    monthCalls: 0,
    totalCalls: 0,
    todayCostRMB: 0,
    weekCostRMB: 0,
    monthCostRMB: 0,
    totalCostRMB: 0,
    dailyTokenLimit: getInitialDailyTokenLimit(),
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

  const weekTokensUsed = computed(() => {
    if (typeof usage.weekTokensUsed === 'number' && usage.weekTokensUsed > 0) {
      return usage.weekTokensUsed;
    }
    const logsTokens = usage.recentLogs.reduce((sum, l) => sum + (l.totalTokens || 0), 0);
    return Math.max(usage.todayTokensUsed, logsTokens);
  });

  const monthTokensUsed = computed(() => {
    if (typeof usage.monthTokensUsed === 'number' && usage.monthTokensUsed > 0) {
      return usage.monthTokensUsed;
    }
    return Math.max(weekTokensUsed.value, usage.todayTokensUsed);
  });

  const totalTokensUsed = computed(() => {
    if (typeof usage.totalTokensUsed === 'number' && usage.totalTokensUsed > 0) {
      return usage.totalTokensUsed;
    }
    return Math.max(monthTokensUsed.value, usage.todayTokensUsed);
  });

  const todayCalls = computed(() => {
    if (typeof usage.todayCalls === 'number' && usage.todayCalls > 0) {
      return usage.todayCalls;
    }
    return usage.todayTokensUsed > 0 ? Math.max(1, Math.round(usage.todayTokensUsed / 250)) : 0;
  });

  const weekCalls = computed(() => {
    if (typeof usage.weekCalls === 'number' && usage.weekCalls > 0) {
      return usage.weekCalls;
    }
    return logTotal.value || usage.totalQaAndGenerateCalls;
  });

  const monthCalls = computed(() => {
    if (typeof usage.monthCalls === 'number' && usage.monthCalls > 0) {
      return usage.monthCalls;
    }
    return logTotal.value || usage.totalQaAndGenerateCalls;
  });

  const totalCalls = computed(() => {
    if (typeof usage.totalCalls === 'number' && usage.totalCalls > 0) {
      return usage.totalCalls;
    }
    return usage.totalQaAndGenerateCalls;
  });

  const todayCostRMB = computed(() => {
    if (typeof usage.todayCostRMB === 'number') return usage.todayCostRMB;
    return Number(((usage.todayTokensUsed / 1000) * 0.002).toFixed(2));
  });

  const weekCostRMB = computed(() => {
    if (typeof usage.weekCostRMB === 'number') return usage.weekCostRMB;
    return Number(((weekTokensUsed.value / 1000) * 0.002).toFixed(2));
  });

  const monthCostRMB = computed(() => {
    if (typeof usage.monthCostRMB === 'number') return usage.monthCostRMB;
    return Number(((monthTokensUsed.value / 1000) * 0.002).toFixed(2));
  });

  const totalCostRMB = computed(() => {
    if (typeof usage.totalCostRMB === 'number') return usage.totalCostRMB;
    return usage.semesterEstimatedCostRMB || Number(((totalTokensUsed.value / 1000) * 0.002).toFixed(2));
  });

  const activePeriodInfo = computed(() => {
    switch (selectedPeriod.value) {
      case '7d': {
        const used = weekTokensUsed.value;
        const limit = usage.dailyTokenLimit * 7;
        const percent = calcTodayUsagePercent(used, limit);
        return {
          key: '7d',
          label: '近 7 天累计消耗 Token',
          usedTokens: used,
          limitTokens: limit,
          percent,
          quotaClass: 'is-success',
          status: '周周期正常',
          desc: `近 7 天交互 ${weekCalls.value} 次 · 日均约 ${formatUsageNumber(Math.round(used / 7))} toks`
        };
      }
      case '30d': {
        const used = monthTokensUsed.value;
        const limit = usage.dailyTokenLimit * 30;
        const percent = calcTodayUsagePercent(used, limit);
        return {
          key: '30d',
          label: '近 30 天累计消耗 Token',
          usedTokens: used,
          limitTokens: limit,
          percent,
          quotaClass: 'is-success',
          status: '月周期健康',
          desc: `近 30 天交互 ${monthCalls.value} 次 · 预估成本约 ¥${monthCostRMB.value.toFixed(2)}`
        };
      }
      case 'all': {
        const used = totalTokensUsed.value;
        const limit = usage.dailyTokenLimit * 90;
        const percent = calcTodayUsagePercent(used, limit);
        return {
          key: 'all',
          label: '历史全部累计消耗 Token',
          usedTokens: used,
          limitTokens: limit,
          percent,
          quotaClass: 'is-success',
          status: '平台全额资助',
          desc: `历史总交互 ${totalCalls.value} 次 · 平台全额资助 ¥${totalCostRMB.value.toFixed(2)}`
        };
      }
      case 'today':
      default: {
        return {
          key: 'today',
          label: '今日 Token 额度',
          usedTokens: usage.todayTokensUsed,
          limitTokens: usage.dailyTokenLimit,
          percent: todayUsagePercent.value,
          quotaClass: quotaPillClass.value,
          status: usage.quotaStatus,
          desc: `日限额 ${formatUsageNumber(usage.dailyTokenLimit)} toks / 日`
        };
      }
    }
  });

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
        logDays: selectedLogDays.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      });
      if (res?.data) {
        Object.assign(usage, res.data);
        const cachedLimit = Number(localStorage.getItem('edumind_sys_ai_tokens_per_user_daily'));
        // 若本地系统配置已设定最新配额（如 200,000），且接口因后端服务未重启仍返回旧的静态兜底值 100,000，优先采用最新配额
        if (cachedLimit > 0 && res.data.dailyTokenLimit === 100000 && cachedLimit !== 100000) {
          usage.dailyTokenLimit = cachedLimit;
          const todayTokens = usage.todayTokensUsed || 0;
          usage.remainingPercent = Math.max(0, Math.min(100, Math.round(((cachedLimit - todayTokens) / cachedLimit) * 100)));
          usage.quotaStatus = resolveQuotaStatusText(usage.remainingPercent);
        }
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

  const changeLogDays = (days: number) => {
    selectedLogDays.value = days;
    pageNum.value = 1;
    loadUsage(false, { tableOnly: true });
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

  const handleAiConfigChanged = (e: Event) => {
    const customEvent = e as CustomEvent;
    const newLimit = customEvent.detail?.tokensPerUserDaily;
    if (newLimit && typeof newLimit === 'number' && newLimit > 0) {
      usage.dailyTokenLimit = newLimit;
      const todayTokens = usage.todayTokensUsed || 0;
      usage.remainingPercent = Math.max(0, Math.min(100, Math.round(((newLimit - todayTokens) / newLimit) * 100)));
      usage.quotaStatus = resolveQuotaStatusText(usage.remainingPercent);
    }
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
    window.addEventListener(AI_CONFIG_CHANGED_EVENT, handleAiConfigChanged);
  });

  onUnmounted(() => {
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener(AI_USAGE_CHANGED_EVENT, handleAiUsageChanged);
    window.removeEventListener(AI_CONFIG_CHANGED_EVENT, handleAiConfigChanged);
  });

  return {
    loading,
    logLoading,
    usage,
    pageNum,
    pageSize,
    logTotal,
    selectedPeriod,
    selectedLogDays,
    todayUsagePercent,
    quotaPillClass,
    weekTokensUsed,
    monthTokensUsed,
    totalTokensUsed,
    todayCalls,
    weekCalls,
    monthCalls,
    totalCalls,
    todayCostRMB,
    weekCostRMB,
    monthCostRMB,
    totalCostRMB,
    activePeriodInfo,
    lastUpdatedText,
    tableHeaderStyle: AI_USAGE_TABLE_HEADER_STYLE,
    formatNumber: formatUsageNumber,
    formatDateTime: formatUsageDateTime,
    loadUsage,
    changeLogDays
  };
}
