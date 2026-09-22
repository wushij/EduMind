import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { storage } from '@/core/storage/local';
import { getCurrentTenant, getAvailableTenants, switchTenant as switchTenantApi } from '@/api/system/tenant';
import type { TenantDetailVO, TenantListVO, CampusVO } from '@/types/system/tenant';
import { ElMessage } from 'element-plus';
import { tokenUtil } from '@/core/auth/token';
import {
  TENANT_ID_KEY,
  CAMPUS_ID_KEY,
  getStoredTenantId,
  setStoredTenantId,
  setStoredCampusId,
  getStoredCampusId,
  clearTenantContext
} from '@/constants/tenant';

export { TENANT_ID_KEY, CAMPUS_ID_KEY };

/** 切换租户结果：供调用方决定是否需要重载页面/重新拉取权限 */
export interface TenantSwitchResult {
  tenantId: number;
  tenantName?: string;
  isDelegated?: boolean;
}

export const useTenantStore = defineStore('tenant', () => {
  const currentTenant = ref<TenantDetailVO | null>(null);
  const availableTenants = ref<TenantListVO[]>([]);
  const currentCampusId = ref<number | null>(getStoredCampusId());
  const loading = ref<boolean>(false);
  /** 切换租户进行中：用于切换器/弹窗的交互反馈与按钮禁用，避免重复提交 */
  const switching = ref<boolean>(false);

  const activeTenantName = computed(() => currentTenant.value?.name || 'EduMind 示范租户');
  const activeCampusName = computed(() => {
    if (!currentTenant.value?.campuses || !currentCampusId.value) {
      return currentTenant.value?.campuses?.[0]?.name || '主校区';
    }
    const found = currentTenant.value.campuses.find((c: CampusVO) => c.id === currentCampusId.value);
    return found ? found.name : '主校区';
  });

  const fetchCurrent = async () => {
    try {
      loading.value = true;
      const res = await getCurrentTenant();
      if (res?.data) {
        currentTenant.value = res.data;
        if (res.data.id) {
          setStoredTenantId(res.data.id);
        }
        if (res.data.campuses && res.data.campuses.length > 0) {
          // 已有校区不属于当前租户时回落到第一个可用校区，避免沿用其它租户的校区
          const stillValid = currentCampusId.value
            && res.data.campuses.some((c: CampusVO) => c.id === currentCampusId.value);
          if (!stillValid) {
            currentCampusId.value = res.data.campuses[0].id;
            setStoredCampusId(res.data.campuses[0].id);
          }
        }
      }
    } catch (e: any) {
      currentTenant.value = null;
      ElMessage.error(e?.message || '获取当前租户信息失败');
    } finally {
      loading.value = false;
    }
  };

  const fetchAvailable = async () => {
    try {
      const res = await getAvailableTenants();
      availableTenants.value = res?.data || [];
    } catch (e: any) {
      availableTenants.value = [];
      ElMessage.error(e?.message || '获取可用租户列表失败');
    }
  };

  /** 选择校区（仅写本地上下文，不触发租户切换） */
  const selectCampus = (campusId: number) => {
    currentCampusId.value = campusId;
    setStoredCampusId(campusId);
  };

  /**
   * 切换租户上下文。
   * 关键点：切换成功后必须同步本地租户 ID（供 X-Tenant-Id 请求头使用）、
   * 清空上一租户的校区、并同步后端可能刷新的会话 token，
   * 保证「切租户后权限与数据都真正按新租户重算」。
   */
  const switchTenant = async (tenantId: number, reason?: string): Promise<TenantSwitchResult> => {
    if (switching.value) {
      return { tenantId };
    }
    switching.value = true;
    loading.value = true;
    try {
      const res = await switchTenantApi({ targetTenantId: tenantId, reason });
      const data = res?.data as (TenantSwitchResult & { token?: string }) | undefined;
      const effectiveTenantId = data?.tenantId ?? tenantId;
      if (data?.token) {
        tokenUtil.set(data.token);
      }
      setStoredTenantId(effectiveTenantId);
      // 租户变了，旧校区归属随之失效，清空后由 fetchCurrent 重新选择
      currentCampusId.value = null;
      storage.remove(CAMPUS_ID_KEY);
      await fetchCurrent();
      ElMessage.success('已切换至目标租户上下文');
      // 租户切换后必须整体重载：清空会话内缓存的业务数据与权限，避免残留上一租户的数据视图
      window.location.reload();
      return {
        tenantId: effectiveTenantId,
        tenantName: currentTenant.value?.name,
        isDelegated: data?.isDelegated
      };
    } finally {
      switching.value = false;
      loading.value = false;
    }
  };

  /** 清空租户/校区上下文（登出、登录态失效时调用） */
  const reset = () => {
    currentTenant.value = null;
    availableTenants.value = [];
    currentCampusId.value = null;
    clearTenantContext();
  };

  return {
    currentTenant,
    availableTenants,
    currentCampusId,
    activeTenantName,
    activeCampusName,
    loading,
    switching,
    fetchCurrent,
    fetchAvailable,
    selectCampus,
    switchTenant,
    reset
  };
});

/** 供非 Store 场景（axios 拦截器）直接读取当前租户 ID */
export function readTenantIdFromStorage(): number | null {
  return getStoredTenantId();
}
