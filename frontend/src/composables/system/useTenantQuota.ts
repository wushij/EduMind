import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listTenantQuotas, updateTenantQuota, listOrgQuotas, updateOrgQuota } from '@/api/system/tenant';

export { listTenantQuotas, updateTenantQuota } from '@/api/system/tenant';
import type { OrgQuotaVO } from '@/types/system/tenant';
import { getAuditLogs, getAvailableAiModels } from '@/composables/system/useAudit';
import type { AIAuditLog } from '@/types/system/audit';
import { useTenantStore } from '@/stores/system/tenant';
import { normalizeAvatarUrl } from '@/utils/format/file';

export interface TenantQuotasState {
  tokenUsed: number;
  tokenLimit: number;
  tokenUsagePercent: number;
  tokenWarningThreshold: number;
  tokenIsWarning: boolean;
  storageUsed: number;
  storageLimit: number;
  qpsPeak: number;
  qpsLimit: number;
  concurrencyUsed: number;
  concurrencyLimit: number;
}

export function computeTokenPercentage(quotas: TenantQuotasState): number {
  if (typeof quotas.tokenUsagePercent === 'number') {
    return quotas.tokenUsagePercent;
  }
  return quotas.tokenLimit > 0 ? Math.round((quotas.tokenUsed / quotas.tokenLimit) * 100) : 0;
}

export function computeIsTokenWarning(quotas: TenantQuotasState, tokenPercentage: number): boolean {
  if (typeof quotas.tokenIsWarning === 'boolean') {
    return quotas.tokenIsWarning;
  }
  return tokenPercentage >= quotas.tokenWarningThreshold;
}

export function computeStoragePercentage(quotas: Pick<TenantQuotasState, 'storageUsed' | 'storageLimit'>): number {
  return quotas.storageLimit > 0 ? Math.round((quotas.storageUsed / quotas.storageLimit) * 100) : 0;
}

export function filterDeptQuotaList(
  list: OrgQuotaVO[],
  searchKeyword: string,
  statusFilter: string
): OrgQuotaVO[] {
  return list.filter(item => {
    if (searchKeyword.trim() && !item.name.includes(searchKeyword.trim())) {
      return false;
    }
    const threshold = item.warningThreshold || 80;
    if (statusFilter === 'NORMAL' && item.usagePercent >= threshold) return false;
    if (statusFilter === 'WARNING' && (item.usagePercent < threshold || item.usagePercent >= 100)) return false;
    if (statusFilter === 'EXCEEDED' && item.usagePercent < 100) return false;
    return true;
  });
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
    CHAT_RAG: '知识增强对话',
    PREP: '智能备课',
    RAG: '知识问答',
    GRADING: '作业智能批改',
    QUESTION_GENERATE: 'AI 试卷/题库生成',
    AGENT: '智能体多步执行',
    GLOBAL_ASSISTANT: '全局智能助手',
    COURSE_OBJECTIVE: '课程教学目标 AI 推荐',
    COURSE_DESCRIPTION: '课程简介 AI 生成',
    COURSE_KNOWLEDGE_POINT: '课程知识点 AI 推荐'
  };
  return map[s] || scene;
}

export function getSceneStyleClass(scene?: string) {
  const s = (scene || '').toUpperCase();
  if (s === 'CHAT' || s === 'CHAT_RAG') return 'badge-chat';
  if (s === 'PREP') return 'badge-prep';
  if (s === 'RAG') return 'badge-rag';
  if (s === 'GRADING') return 'badge-grading';
  if (s === 'QUESTION_GENERATE') return 'badge-question';
  if (s === 'AGENT') return 'badge-agent';
  return 'badge-default';
}

export function useTenantQuota() {
  const tenantStore = useTenantStore();
  const loading = ref(false);
  const refreshing = ref(false);
  const savingConfig = ref(false);
  const configDialogVisible = ref(false);

  const activeTab = ref<'allocation' | 'ledger'>('allocation');

  const quotas = ref<TenantQuotasState>({
    tokenUsed: 4820000,
    tokenLimit: 10000000,
    tokenUsagePercent: 48,
    tokenWarningThreshold: 85,
    tokenIsWarning: false,
    storageUsed: 1820,
    storageLimit: 5120,
    qpsPeak: 42,
    qpsLimit: 80,
    concurrencyUsed: 16,
    concurrencyLimit: 50
  });

  const editForm = ref({
    warningThreshold: 85,
    tokenLimit: 10000000,
    qpsLimit: 80
  });

  const tokenPercentage = computed(() => computeTokenPercentage(quotas.value));

  const isTokenWarning = computed(() => computeIsTokenWarning(quotas.value, tokenPercentage.value));

  const storagePercentage = computed(() => computeStoragePercentage(quotas.value));

  const deptQuotaList = ref<OrgQuotaVO[]>([]);
  const deptSearchKeyword = ref('');
  const deptStatusFilter = ref('');
  const deptPageNum = ref(1);
  const deptPageSize = ref(10);
  const savingDeptQuota = ref(false);

  const totalAllocatedTokens = computed(() => {
    return deptQuotaList.value.reduce((sum, item) => sum + item.tokenLimit, 0);
  });

  const totalAllocatedUsed = computed(() => {
    return deptQuotaList.value.reduce((sum, item) => sum + item.tokenUsed, 0);
  });

  const warningDeptCount = computed(() => {
    return deptQuotaList.value.filter(d => d.usagePercent >= (d.warningThreshold || 85)).length;
  });

  const filteredDeptQuotaList = computed(() => {
    return filterDeptQuotaList(deptQuotaList.value, deptSearchKeyword.value, deptStatusFilter.value);
  });

  const paginatedDeptQuotaList = computed(() => {
    const start = (deptPageNum.value - 1) * deptPageSize.value;
    return filteredDeptQuotaList.value.slice(start, start + deptPageSize.value);
  });

  const adjustDeptModalVisible = ref(false);
  const currentEditingDept = ref<OrgQuotaVO | null>(null);

  function openAdjustDeptQuota(row: OrgQuotaVO) {
    currentEditingDept.value = { ...row };
    adjustDeptModalVisible.value = true;
  }

  async function saveDeptQuotaAdjustment() {
    if (!currentEditingDept.value) return;
    savingDeptQuota.value = true;
    try {
      await updateOrgQuota({
        orgId: currentEditingDept.value.orgId,
        tokenLimit: currentEditingDept.value.tokenLimit,
        storageLimit: currentEditingDept.value.storageLimit,
        seatsLimit: currentEditingDept.value.seatsLimit,
        warningThreshold: currentEditingDept.value.warningThreshold || 85
      });
      ElMessage.success(`已成功更新【${currentEditingDept.value.name}】的算力配额指标`);
      adjustDeptModalVisible.value = false;
      await fetchOrgQuotas();
    } catch (err: any) {
      ElMessage.error(err?.message || '保存院系配额失败');
    } finally {
      savingDeptQuota.value = false;
    }
  }

  const auditLogs = ref<AIAuditLog[]>([]);
  const tableLoading = ref(false);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  const dateRange = ref<[string, string] | null>(null);
  const sceneFilter = ref('');
  const modelFilter = ref('');
  const searchKeyword = ref('');
  const modelOptions = ref<string[]>([]);

  const detailDrawerVisible = ref(false);
  const selectedLog = ref<AIAuditLog | null>(null);

  const pageTotalTokens = computed(() => {
    return auditLogs.value.reduce((sum, item) => sum + (item.totalTokens || 0), 0);
  });

  const pageEstimatedCost = computed(() => {
    return auditLogs.value.reduce((sum, item) => sum + (item.estimatedCost || 0), 0);
  });

  async function copyText(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success('已复制到剪贴板');
    } catch {
      ElMessage.error('复制失败，请手动选取复制');
    }
  }

  async function loadAvailableModels() {
    try {
      const models = await getAvailableAiModels();
      modelOptions.value = models && models.length > 0 ? Array.from(new Set(models)) : [];
    } catch {
      modelOptions.value = [];
    }
  }

  async function fetchQuotas() {
    try {
      const res = await listTenantQuotas();
      if (res?.data && res.data.length > 0) {
        const tokenItem = res.data.find(q => q.quotaType === 'TOKEN');
        if (tokenItem) {
          quotas.value.tokenUsed = tokenItem.usedValue;
          quotas.value.tokenLimit = tokenItem.limitValue;
          quotas.value.tokenUsagePercent = tokenItem.usagePercent ?? (tokenItem.limitValue > 0 ? Math.round((tokenItem.usedValue / tokenItem.limitValue) * 100) : 0);
          quotas.value.tokenWarningThreshold = tokenItem.warningThreshold;
          quotas.value.tokenIsWarning = tokenItem.isWarning ?? (quotas.value.tokenUsagePercent >= tokenItem.warningThreshold);
        }
        const storageItem = res.data.find(q => q.quotaType === 'STORAGE');
        if (storageItem) {
          quotas.value.storageUsed = storageItem.usedValue;
          quotas.value.storageLimit = storageItem.limitValue;
        }
        const qpsItem = res.data.find(q => q.quotaType === 'QPS');
        if (qpsItem) {
          quotas.value.qpsPeak = qpsItem.usedValue;
          quotas.value.qpsLimit = qpsItem.limitValue;
        }
        const seatsItem = res.data.find(q => q.quotaType === 'SEATS');
        if (seatsItem) {
          quotas.value.concurrencyUsed = seatsItem.usedValue;
          quotas.value.concurrencyLimit = seatsItem.limitValue;
        }
      }
    } catch {
      // Keep baseline default
    }
  }

  async function fetchOrgQuotas() {
    try {
      const res = await listOrgQuotas();
      if (res?.data && Array.isArray(res.data)) {
        deptQuotaList.value = res.data;
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '获取组织配额失败');
    }
  }

  async function loadAuditLogs() {
    tableLoading.value = true;
    try {
      let startDate: string | undefined;
      let endDate: string | undefined;
      if (dateRange.value && dateRange.value.length === 2) {
        startDate = dateRange.value[0];
        endDate = dateRange.value[1];
      }

      const [res] = await Promise.all([
        getAuditLogs({
          page: pageNum.value,
          pageSize: pageSize.value,
          scene: sceneFilter.value || undefined,
          model: modelFilter.value || undefined,
          keyword: searchKeyword.value.trim() || undefined,
          startDate,
          endDate
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);

      auditLogs.value = res.list;
      total.value = res.total;

      const foundModels = res.list.map(l => l.model).filter(Boolean);
      if (foundModels.length > 0) {
        modelOptions.value = Array.from(new Set([...modelOptions.value, ...foundModels]));
      }
    } catch (e: any) {
      ElMessage.error(e.message || '加载算力流水账单失败');
    } finally {
      tableLoading.value = false;
    }
  }

  async function handleRefreshAll() {
    refreshing.value = true;
    try {
      await Promise.all([fetchQuotas(), fetchOrgQuotas(), loadAuditLogs()]);
      ElMessage.success('租户用量大盘与算力账单已同步');
    } finally {
      refreshing.value = false;
    }
  }

  function handleSearch() {
    pageNum.value = 1;
    loadAuditLogs();
  }

  function handleFilterChange() {
    pageNum.value = 1;
    loadAuditLogs();
  }

  function handleReset() {
    dateRange.value = null;
    sceneFilter.value = '';
    modelFilter.value = '';
    searchKeyword.value = '';
    pageNum.value = 1;
    loadAuditLogs();
  }

  function viewDetail(row: AIAuditLog) {
    selectedLog.value = row;
    detailDrawerVisible.value = true;
  }

  const openConfigModal = () => {
    editForm.value = {
      warningThreshold: quotas.value.tokenWarningThreshold,
      tokenLimit: quotas.value.tokenLimit,
      qpsLimit: quotas.value.qpsLimit
    };
    configDialogVisible.value = true;
  };

  const saveConfig = async () => {
    try {
      savingConfig.value = true;
      await updateTenantQuota({
        quotaType: 'TOKEN',
        limitValue: editForm.value.tokenLimit,
        warningThreshold: editForm.value.warningThreshold
      });
      quotas.value.tokenLimit = editForm.value.tokenLimit;
      quotas.value.tokenWarningThreshold = editForm.value.warningThreshold;
      quotas.value.qpsLimit = editForm.value.qpsLimit;
      ElMessage.success('全局配额策略已成功应用');
      configDialogVisible.value = false;
    } catch (e: any) {
      ElMessage.error(e.message || '保存全局配额失败');
    } finally {
      savingConfig.value = false;
    }
  };

  onMounted(async () => {
    loading.value = true;
    try {
      await Promise.all([fetchQuotas(), fetchOrgQuotas(), loadAuditLogs(), loadAvailableModels()]);
    } finally {
      loading.value = false;
    }
  });

  return {
    tenantStore,
    loading,
    refreshing,
    savingConfig,
    configDialogVisible,
    activeTab,
    quotas,
    editForm,
    tokenPercentage,
    isTokenWarning,
    storagePercentage,
    deptQuotaList,
    deptSearchKeyword,
    deptStatusFilter,
    deptPageNum,
    deptPageSize,
    savingDeptQuota,
    totalAllocatedTokens,
    totalAllocatedUsed,
    warningDeptCount,
    filteredDeptQuotaList,
    paginatedDeptQuotaList,
    adjustDeptModalVisible,
    currentEditingDept,
    openAdjustDeptQuota,
    saveDeptQuotaAdjustment,
    auditLogs,
    tableLoading,
    pageNum,
    pageSize,
    total,
    dateRange,
    sceneFilter,
    modelFilter,
    searchKeyword,
    modelOptions,
    detailDrawerVisible,
    selectedLog,
    pageTotalTokens,
    pageEstimatedCost,
    getUserAvatarUrl,
    getRoleLabel,
    getSceneLabel,
    getSceneStyleClass,
    copyText,
    loadAuditLogs,
    handleRefreshAll,
    handleSearch,
    handleFilterChange,
    handleReset,
    viewDetail,
    openConfigModal,
    saveConfig
  };
}
