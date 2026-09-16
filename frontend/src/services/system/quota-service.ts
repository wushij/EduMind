import { USE_MOCK } from '@/config/mock';
import type { RoleQuotaConfig } from '@/types/system/quota';
import * as quotaApi from '@/api/system/quota';

interface BackendUserQuota {
  id?: number;
  userId: number;
  dailyTokenLimit?: number;
  dailyCallLimit?: number;
  usedTokensToday?: number;
  usedCallsToday?: number;
}

const mockRoleQuotas: RoleQuotaConfig[] = [];

function mapUserQuota(raw: BackendUserQuota): RoleQuotaConfig {
  const dailyLimit = raw.dailyTokenLimit ?? 0;
  const usedToday = raw.usedTokensToday ?? 0;
  return {
    id: raw.userId,
    role: 'STUDENT',
    roleName: `用户 #${raw.userId}`,
    dailyTokenLimit: dailyLimit,
    monthlyTokenLimit: dailyLimit > 0 ? dailyLimit * 30 : 0,
    qpsLimit: raw.dailyCallLimit ?? 0,
    usedTokensToday: usedToday,
    usedTokensMonth: usedToday,
    totalUsers: 1,
    isUnlimited: dailyLimit === 0
  };
}

export async function getRoleQuotas(): Promise<RoleQuotaConfig[]> {
  try {
    const res = await quotaApi.fetchAiQuotas();
    if (res?.data && Array.isArray(res.data)) {
      return res.data.map(mapUserQuota);
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Quota Service] Fallback mockRoleQuotas', err);
  }
  return mockRoleQuotas;
}

export async function updateRoleQuota(
  userId: number,
  data: Partial<RoleQuotaConfig>
): Promise<boolean> {
  try {
    await quotaApi.updateAiQuotaHttp(userId, {
      dailyTokenLimit: data.dailyTokenLimit,
      dailyCallLimit: data.qpsLimit
    });
    return true;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Quota Service] Fallback update mock', err);
  }
  return true;
}
