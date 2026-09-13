import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { storage } from '@/core/storage/local';
import { getCurrentTenant, getAvailableTenants, switchTenant as switchTenantApi } from '@/api/system/tenant';
import type { TenantDetailVO, TenantListVO, CampusVO } from '@/types/system/tenant';
import { ElMessage } from 'element-plus';

export const TENANT_ID_KEY = 'edumind_tenant_id';
export const CAMPUS_ID_KEY = 'edumind_campus_id';

export const useTenantStore = defineStore('tenant', () => {
  const currentTenant = ref<TenantDetailVO | null>(null);
  const availableTenants = ref<TenantListVO[]>([]);
  const currentCampusId = ref<number | null>(storage.get(CAMPUS_ID_KEY) || null);
  const loading = ref<boolean>(false);

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
          storage.set(TENANT_ID_KEY, res.data.id);
        }
        if (res.data.campuses && res.data.campuses.length > 0 && !currentCampusId.value) {
          currentCampusId.value = res.data.campuses[0].id;
          storage.set(CAMPUS_ID_KEY, res.data.campuses[0].id);
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

  const switchTenant = async (tenantId: number, reason?: string) => {
    try {
      loading.value = true;
      await switchTenantApi({ targetTenantId: tenantId, reason });
      storage.set(TENANT_ID_KEY, tenantId);
      ElMessage.success('已切换至目标租户上下文');
      await fetchCurrent();
      // 重新加载页面以刷新当前租户业务数据
      window.location.reload();
    } catch (e: any) {
      ElMessage.error(e.message || '切换租户失败');
    } finally {
      loading.value = false;
    }
  };

  return {
    currentTenant,
    availableTenants,
    currentCampusId,
    activeTenantName,
    activeCampusName,
    loading,
    fetchCurrent,
    fetchAvailable,
    switchTenant
  };
});
