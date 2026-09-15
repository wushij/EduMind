import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { TenantListVO } from '@/types/system/tenant';
import {
  filterTenants,
  resolvePlanName,
  isExpiringSoon,
  createEmptyOverviewStats
} from './useTenant';

vi.mock('@/api/system/tenant', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/system/tenant')>();
  return {
    ...actual,
    pageTenants: vi.fn(),
    getTenantOverviewStats: vi.fn(),
    updateTenantStatus: vi.fn(),
    deleteTenant: vi.fn()
  };
});

vi.mock('@/stores/system/tenant', () => ({
  useTenantStore: () => ({
    currentTenant: { id: 1, name: 'Demo' },
    switchTenant: vi.fn()
  })
}));

const sampleTenants: TenantListVO[] = [
  {
    id: 1,
    name: '智教云大学',
    tenantCode: 'EDU001',
    status: 1,
    planCode: 'PRO',
    domain: 'edu.example.com'
  },
  {
    id: 2,
    name: '测试学校',
    tenantCode: 'TEST',
    status: 0,
    planCode: 'STANDARD'
  }
];

describe('useTenant helpers', () => {
  it('filters tenants by keyword, status and plan', () => {
    expect(filterTenants(sampleTenants, '', '', '')).toHaveLength(2);
    expect(filterTenants(sampleTenants, '智教', '', '')).toHaveLength(1);
    expect(filterTenants(sampleTenants, '', 1, '')).toHaveLength(1);
    expect(filterTenants(sampleTenants, '', '', 'PRO')).toHaveLength(1);
    expect(filterTenants(sampleTenants, 'none', '', '')).toHaveLength(0);
  });

  it('resolves plan display names', () => {
    expect(resolvePlanName('FLAGSHIP')).toBe('尊享旗舰版');
    expect(resolvePlanName('PRO')).toBe('高配专业版');
    expect(resolvePlanName()).toBe('标准方案');
  });

  it('detects expiring tenants within 30 days', () => {
    const soon = new Date();
    soon.setDate(soon.getDate() + 10);
    expect(isExpiringSoon(soon.toISOString())).toBe(true);
    expect(isExpiringSoon()).toBe(false);
  });

  it('creates empty overview stats with defaults', () => {
    const stats = createEmptyOverviewStats();
    expect(stats.totalTenants).toBe(0);
    expect(stats.complianceRate).toBe(99.98);
  });
});

describe('useTenant API wrappers', () => {
  it('re-exports tenant and campus API functions', async () => {
    const api = await import('@/api/system/tenant');
    const composable = await import('./useTenant');

    expect(composable.createTenant).toBe(api.createTenant);
    expect(composable.updateTenant).toBe(api.updateTenant);
    expect(composable.listCampuses).toBe(api.listCampuses);
    expect(composable.createCampus).toBe(api.createCampus);
    expect(composable.updateCampus).toBe(api.updateCampus);
    expect(composable.updateCampusStatus).toBe(api.updateCampusStatus);
    expect(composable.deleteCampus).toBe(api.deleteCampus);
  });
});

describe('useTenant composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initializes and loads tenants', async () => {
    const { pageTenants } = await import('@/api/system/tenant');
    vi.mocked(pageTenants).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: sampleTenants, total: 2 },
      timestamp: Date.now()
    });

    const { useTenant } = await import('./useTenant');
    const { tenants, filteredTenants, loadTenants } = useTenant();

    await loadTenants();

    expect(tenants.value).toHaveLength(2);
    expect(filteredTenants.value).toHaveLength(2);
  });
});
