import { get, put } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { RoleQuotaConfig } from '@/types/system/quota';

interface BackendUserQuota {
  id?: number;
  userId: number;
  dailyTokenLimit?: number;
  dailyCallLimit?: number;
  usedTokensToday?: number;
  usedCallsToday?: number;
}

export const mockRoleQuotas: RoleQuotaConfig[] = [];

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

export const getRoleQuotas = async (): Promise<RoleQuotaConfig[]> => {
  try {
    const res = await get<BackendUserQuota[]>('/system/ai-quota');
    if (res?.data && Array.isArray(res.data)) {
      return res.data.map(mapUserQuota);
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Quota API] Fallback mockRoleQuotas', err);
  }
  return mockRoleQuotas;
};

export const updateRoleQuota = async (userId: number, data: Partial<RoleQuotaConfig>): Promise<boolean> => {
  try {
    await put(`/system/ai-quota/${userId}`, {
      dailyTokenLimit: data.dailyTokenLimit,
      dailyCallLimit: data.qpsLimit
    });
    return true;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Quota API] Fallback update mock', err);
  }
  return true;
};
