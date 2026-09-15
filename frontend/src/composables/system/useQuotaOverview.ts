import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getRoleQuotas, updateRoleQuota } from '@/api/system/quota';
import type { RoleQuotaConfig } from '@/types/system/quota';

export function computeQuotaUsagePercent(used: number, limit: number, isUnlimited?: boolean): number {
  if (isUnlimited || limit <= 0) return 0;
  return Math.min(100, Math.round((used / limit) * 100));
}

export function useQuotaOverview() {
  const loading = ref(false);
  const userQuotas = ref<RoleQuotaConfig[]>([]);
  const editDialogVisible = ref(false);
  const editingQuota = ref<RoleQuotaConfig | null>(null);

  async function fetchQuotas() {
    loading.value = true;
    try {
      userQuotas.value = await getRoleQuotas();
    } catch {
      userQuotas.value = [];
      ElMessage.error('加载配额数据失败');
    } finally {
      loading.value = false;
    }
  }

  function editQuota(row: RoleQuotaConfig) {
    editingQuota.value = JSON.parse(JSON.stringify(row));
    editDialogVisible.value = true;
  }

  async function saveUserQuota() {
    if (!editingQuota.value) return;
    try {
      await updateRoleQuota(editingQuota.value.id, editingQuota.value);
      ElMessage.success('用户配额已更新');
      editDialogVisible.value = false;
      await fetchQuotas();
    } catch {
      ElMessage.error('保存失败');
    }
  }

  onMounted(fetchQuotas);

  return {
    loading,
    userQuotas,
    editDialogVisible,
    editingQuota,
    fetchQuotas,
    editQuota,
    saveUserQuota,
    computeQuotaUsagePercent
  };
}
