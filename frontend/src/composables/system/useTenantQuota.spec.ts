import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  computeTokenPercentage,
  computeIsTokenWarning,
  computeStoragePercentage,
  filterDeptQuotaList,
  getRoleLabel,
  getSceneLabel,
  getSceneStyleClass,
  getUserAvatarUrl
} from './useTenantQuota';
import type { OrgQuotaVO } from '@/types/system/tenant';

vi.mock('@/api/system/tenant', () => ({
  listTenantQuotas: vi.fn(),
  updateTenantQuota: vi.fn(),
  listOrgQuotas: vi.fn(),
  updateOrgQuota: vi.fn()
}));

vi.mock('@/api/system/audit', () => ({
  getAuditLogs: vi.fn(),
  getAvailableAiModels: vi.fn()
}));

vi.mock('@/stores/system/tenant', () => ({
  useTenantStore: () => ({
    activeTenantName: '测试租户',
    activeCampusName: '主校区'
  })
}));

vi.mock('@/utils/format/file', () => ({
  normalizeAvatarUrl: (url: string) => url
}));

describe('useTenantQuota helpers', () => {
  const baseQuotas = {
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
  };

  it('computes token percentage from usage percent when provided', () => {
    expect(computeTokenPercentage(baseQuotas)).toBe(48);
  });

  it('computes token percentage from used/limit when usage percent missing', () => {
    expect(computeTokenPercentage({
      ...baseQuotas,
      tokenUsagePercent: undefined as unknown as number
    })).toBe(48);
  });

  it('computes token warning state', () => {
    expect(computeIsTokenWarning(baseQuotas, 48)).toBe(false);
    expect(computeIsTokenWarning({ ...baseQuotas, tokenIsWarning: true }, 48)).toBe(true);
    expect(computeIsTokenWarning({ ...baseQuotas, tokenIsWarning: undefined as unknown as boolean }, 90)).toBe(true);
  });

  it('computes storage percentage', () => {
    expect(computeStoragePercentage({ storageUsed: 1820, storageLimit: 5120 })).toBe(36);
    expect(computeStoragePercentage({ storageUsed: 0, storageLimit: 0 })).toBe(0);
  });

  it('filters department quota list by keyword and status', () => {
    const list: OrgQuotaVO[] = [
      { orgId: 1, name: '计算机学院', tokenUsed: 100, tokenLimit: 1000, usagePercent: 50, warningThreshold: 80 } as OrgQuotaVO,
      { orgId: 2, name: '数学学院', tokenUsed: 900, tokenLimit: 1000, usagePercent: 90, warningThreshold: 80 } as OrgQuotaVO,
      { orgId: 3, name: '物理学院', tokenUsed: 1000, tokenLimit: 1000, usagePercent: 100, warningThreshold: 80 } as OrgQuotaVO
    ];

    expect(filterDeptQuotaList(list, '计算机', '')).toHaveLength(1);
    expect(filterDeptQuotaList(list, '', 'NORMAL')).toHaveLength(1);
    expect(filterDeptQuotaList(list, '', 'WARNING')).toHaveLength(1);
    expect(filterDeptQuotaList(list, '', 'EXCEEDED')).toHaveLength(1);
  });

  it('maps role labels', () => {
    expect(getRoleLabel('ADMIN')).toBe('管理员');
    expect(getRoleLabel('TEACHER')).toBe('教师');
    expect(getRoleLabel(undefined)).toBe('用户');
    expect(getRoleLabel('CUSTOM')).toBe('CUSTOM');
  });

  it('maps scene labels and style classes', () => {
    expect(getSceneLabel('CHAT')).toBe('智能对话');
    expect(getSceneLabel(undefined)).toBe('智能对话');
    expect(getSceneStyleClass('CHAT')).toBe('badge-chat');
    expect(getSceneStyleClass('AGENT')).toBe('badge-agent');
    expect(getSceneStyleClass('UNKNOWN')).toBe('badge-default');
  });

  it('builds user avatar url with fallback', () => {
    expect(getUserAvatarUrl(null)).toBe('');
    expect(getUserAvatarUrl({ avatar: 'https://cdn.test/a.png', username: 'u1', userId: 1 } as any)).toBe('https://cdn.test/a.png');
    expect(getUserAvatarUrl({ username: 'teacher', userId: 2 } as any)).toContain('teacher');
  });
});

describe('useTenantQuota API wrappers', () => {
  it('re-exports tenant quota API functions', async () => {
    const api = await import('@/api/system/tenant');
    const composable = await import('./useTenantQuota');

    expect(composable.listTenantQuotas).toBe(api.listTenantQuotas);
    expect(composable.updateTenantQuota).toBe(api.updateTenantQuota);
  });
});

describe('useTenantQuota composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads quota data via refresh handler', async () => {
    const { listTenantQuotas, listOrgQuotas } = await import('@/api/system/tenant');
    const { getAuditLogs } = await import('@/api/system/audit');

    vi.mocked(listTenantQuotas).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [{
        tenantId: 1,
        quotaType: 'TOKEN',
        usedValue: 1000,
        limitValue: 2000,
        usagePercent: 50,
        warningThreshold: 85,
        isWarning: false
      }],
      timestamp: Date.now()
    });
    vi.mocked(listOrgQuotas).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [{
        orgId: 1,
        name: '计算机学院',
        orgType: 'DEPT',
        orgTypeLabel: '院系',
        campusName: '主校区',
        tokenUsed: 100,
        tokenLimit: 1000,
        usagePercent: 10,
        warningThreshold: 85,
        storageLimit: 100,
        storageUsed: 20,
        seatsLimit: 50,
        seatsUsed: 5,
        status: 'NORMAL'
      }],
      timestamp: Date.now()
    });
    vi.mocked(getAuditLogs).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: [], total: 0 },
      timestamp: Date.now()
    });

    const { useTenantQuota } = await import('./useTenantQuota');
    const { deptQuotaList, total, tokenPercentage, handleRefreshAll } = useTenantQuota();

    await handleRefreshAll();

    expect(deptQuotaList.value).toHaveLength(1);
    expect(total.value).toBe(0);
    expect(tokenPercentage.value).toBe(50);
  });
});
