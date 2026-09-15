import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  pageTenants,
  getTenantOverviewStats,
  updateTenantStatus,
  deleteTenant
} from '@/api/system/tenant';

export {
  createTenant,
  updateTenant,
  listCampuses,
  createCampus,
  updateCampus,
  updateCampusStatus,
  deleteCampus
} from '@/api/system/tenant';
import { useTenantStore } from '@/stores/system/tenant';
import type { TenantListVO, TenantOverviewStatsVO } from '@/types/system/tenant';

export const TENANT_STATUS_TABS = [
  { label: '全部状态', value: '' as const },
  { label: '正常服务', value: 1 as const },
  { label: '已冻结', value: 0 as const }
];

export function createEmptyOverviewStats(): TenantOverviewStatsVO {
  return {
    totalTenants: 0,
    activeTenants: 0,
    totalCampuses: 0,
    totalMembers: 0,
    totalStudents: 0,
    totalTeachers: 0,
    complianceRate: 99.98,
    totalTokenQuota: 0,
    usedTokenQuota: 0
  };
}

export function filterTenants(
  tenants: TenantListVO[],
  searchKeyword: string,
  statusFilter: number | '',
  planFilter: string
): TenantListVO[] {
  return tenants.filter((t) => {
    const kw = searchKeyword.toLowerCase();
    const matchKw =
      !searchKeyword ||
      t.name.toLowerCase().includes(kw) ||
      (t.tenantCode && t.tenantCode.toLowerCase().includes(kw)) ||
      (t.domain && t.domain.toLowerCase().includes(kw));
    const matchStatus = statusFilter === '' || t.status === statusFilter;
    const matchPlan =
      !planFilter || (t.planCode && t.planCode.toUpperCase() === planFilter.toUpperCase());
    return matchKw && matchStatus && matchPlan;
  });
}

export function resolvePlanName(code?: string) {
  if (!code) return '标准方案';
  switch (code.toUpperCase()) {
    case 'FLAGSHIP':
      return '尊享旗舰版';
    case 'PRO':
      return '高配专业版';
    default:
      return '敏捷标准版';
  }
}

export function isExpiringSoon(expireTime?: string) {
  if (!expireTime) return false;
  const expireDate = new Date(expireTime).getTime();
  const now = Date.now();
  const diffDays = (expireDate - now) / (1000 * 3600 * 24);
  return diffDays > 0 && diffDays <= 30;
}

export function useTenant() {
  const router = useRouter();
  const tenantStore = useTenantStore();

  const loading = ref(false);
  const searchKeyword = ref('');
  const statusFilter = ref<number | ''>('');
  const planFilter = ref<string>('');

  const overviewStats = ref<TenantOverviewStatsVO>(createEmptyOverviewStats());
  const tenants = ref<TenantListVO[]>([]);

  const campusDrawerVisible = ref(false);
  const quotaDrawerVisible = ref(false);
  const editDialogVisible = ref(false);
  const switchDialogVisible = ref(false);

  const activeDrawerTenant = ref<TenantListVO | null>(null);
  const activeEditingTenant = ref<TenantListVO | null>(null);
  const activeSwitchTenant = ref<TenantListVO | null>(null);

  const campusDrawerRef = ref();
  const quotaDrawerRef = ref();
  const editDialogRef = ref();

  const filteredTenants = computed(() =>
    filterTenants(tenants.value, searchKeyword.value, statusFilter.value, planFilter.value)
  );

  const loadOverviewStats = async () => {
    try {
      const res = await getTenantOverviewStats();
      if (res?.data) {
        overviewStats.value = res.data;
      }
    } catch {
      // 保障性兜底
    }
  };

  const loadTenants = async () => {
    try {
      loading.value = true;
      const res = await pageTenants({ page: 1, pageSize: 100 });
      if (res?.data?.list) {
        tenants.value = res.data.list;
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '加载租户列表失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  };

  const handleSearch = () => {
    // computed handles filtering
  };

  const handleStatusTabChange = (val: number | '') => {
    statusFilter.value = val;
  };

  const openCreateDialog = () => {
    activeEditingTenant.value = null;
    editDialogVisible.value = true;
    nextTick(() => {
      editDialogRef.value?.initForm();
    });
  };

  const openCampusDrawer = (tenant: TenantListVO) => {
    activeDrawerTenant.value = tenant;
    campusDrawerVisible.value = true;
    nextTick(() => {
      campusDrawerRef.value?.loadCampuses();
    });
  };

  const openQuotaDrawer = (tenant: TenantListVO) => {
    activeDrawerTenant.value = tenant;
    quotaDrawerVisible.value = true;
    nextTick(() => {
      quotaDrawerRef.value?.loadQuotas();
    });
  };

  const jumpToOrgTree = async (tenant: TenantListVO) => {
    if (tenantStore.currentTenant?.id !== tenant.id) {
      await tenantStore.switchTenant(tenant.id);
    }
    router.push({
      path: '/system/organizations',
      query: { tenantId: tenant.id }
    });
  };

  const openSwitchDialog = (tenant: TenantListVO) => {
    if (tenantStore.currentTenant?.id === tenant.id) {
      ElMessage.info(`您当前已处于【${tenant.name}】学校租户环境中`);
      return;
    }
    activeSwitchTenant.value = tenant;
    switchDialogVisible.value = true;
  };

  const handleSwitched = () => {
    switchDialogVisible.value = false;
  };

  const copyDomain = (domain: string) => {
    navigator.clipboard.writeText(domain);
    ElMessage.success('域名已复制到剪贴板');
  };

  const handleMoreCommand = async (cmd: string, tenant: TenantListVO) => {
    if (cmd === 'edit') {
      activeEditingTenant.value = tenant;
      editDialogVisible.value = true;
      nextTick(() => {
        editDialogRef.value?.initForm();
      });
    } else if (cmd === 'toggleStatus') {
      const nextStatus = tenant.status === 1 ? 0 : 1;
      const actionText = nextStatus === 1 ? '启用' : '冻结';
      try {
        await ElMessageBox.confirm(
          `确定${actionText}租户【${tenant.name}】吗？${nextStatus === 0 ? '冻结后该租户师生将无法登录系统。' : ''}`,
          `${actionText}确认`,
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: nextStatus === 0 ? 'warning' : 'info'
          }
        );
        await updateTenantStatus(tenant.id, nextStatus);
        ElMessage.success(`租户已${actionText}`);
        loadTenants();
        loadOverviewStats();
      } catch (e: unknown) {
        if (e !== 'cancel') {
          const message = e instanceof Error ? e.message : '操作失败';
          ElMessage.error(message);
        }
      }
    } else if (cmd === 'delete') {
      try {
        await ElMessageBox.confirm(
          `确定注销并彻底删除学校租户【${tenant.name}】吗？此操作不可逆！`,
          '注销确认',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'error'
          }
        );
        await deleteTenant(tenant.id);
        ElMessage.success('租户已成功注销');
        loadTenants();
        loadOverviewStats();
      } catch (e: unknown) {
        if (e !== 'cancel') {
          const message = e instanceof Error ? e.message : '删除失败';
          ElMessage.error(message);
        }
      }
    }
  };

  const handleSaved = () => {
    loadTenants();
    loadOverviewStats();
  };

  const handleCampusChanged = () => {
    loadTenants();
    loadOverviewStats();
  };

  const handleQuotaChanged = () => {
    loadTenants();
    loadOverviewStats();
  };

  onMounted(() => {
    loadOverviewStats();
    loadTenants();
  });

  return {
    tenantStore,
    loading,
    searchKeyword,
    statusFilter,
    planFilter,
    statusTabs: TENANT_STATUS_TABS,
    overviewStats,
    tenants,
    campusDrawerVisible,
    quotaDrawerVisible,
    editDialogVisible,
    switchDialogVisible,
    activeDrawerTenant,
    activeEditingTenant,
    activeSwitchTenant,
    campusDrawerRef,
    quotaDrawerRef,
    editDialogRef,
    filteredTenants,
    loadOverviewStats,
    loadTenants,
    handleSearch,
    handleStatusTabChange,
    openCreateDialog,
    openCampusDrawer,
    openQuotaDrawer,
    jumpToOrgTree,
    openSwitchDialog,
    handleSwitched,
    copyDomain,
    resolvePlanName,
    isExpiringSoon,
    handleMoreCommand,
    handleSaved,
    handleCampusChanged,
    handleQuotaChanged
  };
}
