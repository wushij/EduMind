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
    } catch (e) {
      // Fallback default for demo
      if (!currentTenant.value) {
        currentTenant.value = {
          id: 1,
          tenantCode: 'DEFAULT_SCHOOL',
          name: '智教云示范第一中学',
          domain: 'demo.edumind.edu.cn',
          status: 1,
          campuses: [
            { id: 1, tenantId: 1, campusCode: 'MAIN', name: '本部校区', isMain: true, status: 1 },
            { id: 2, tenantId: 1, campusCode: 'EAST', name: '东校区', isMain: false, status: 1 }
          ]
        };
      }
    } finally {
      loading.value = false;
    }
  };

  const fetchAvailable = async () => {
    try {
      const res = await getAvailableTenants();
      if (res?.data && res.data.length > 0) {
        availableTenants.value = res.data;
      } else {
        availableTenants.value = [
          { id: 1, tenantCode: 'DEFAULT_SCHOOL', name: '智教云示范第一中学', status: 1, campusCount: 2, memberCount: 1250 },
          { id: 2, tenantCode: 'TECH_COLLEGE', name: '前沿软件技术职业学院', status: 1, campusCount: 1, memberCount: 860 }
        ];
      }
    } catch (e) {
      availableTenants.value = [
        { id: 1, tenantCode: 'DEFAULT_SCHOOL', name: '智教云示范第一中学', status: 1, campusCount: 2, memberCount: 1250 },
        { id: 2, tenantCode: 'TECH_COLLEGE', name: '前沿软件技术职业学院', status: 1, campusCount: 1, memberCount: 860 }
      ];
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
