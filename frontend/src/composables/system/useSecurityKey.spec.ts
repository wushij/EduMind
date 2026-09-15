import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';
import {
  filterSecurityKeys,
  paginateList,
  getActiveKeyByAlias,
  getActiveVerByAlias,
  getDistinctAliases
} from './useSecurityKey';

vi.mock('@/api/system/security-key', () => ({
  listSecurityKeys: vi.fn(),
  rotateSecurityKey: vi.fn(),
  testSecurityKeyCrypto: vi.fn()
}));

vi.mock('@/stores/system/tenant', () => ({
  useTenantStore: vi.fn(() => ({
    activeTenantName: '华东师范大学附属实验学校',
    activeCampusName: '主校区'
  }))
}));

const sampleKeys: SecurityKeyVersionVO[] = [
  {
    id: 1,
    tenantId: 1001,
    keyAlias: 'edumind-data-key',
    keyVersion: 2,
    algorithm: 'SM4-GCM',
    status: 'ACTIVE',
    keyFingerprint: 'SM4-GCM#7F8A-3C2B',
    usageScope: '学生错题 / 认知画像',
    activatedTime: '2026-01-01 10:00:00',
    createTime: '2026-01-01 10:00:00'
  },
  {
    id: 2,
    tenantId: 1001,
    keyAlias: 'edumind-data-key',
    keyVersion: 1,
    algorithm: 'SM4-GCM',
    status: 'DEPRECATED',
    keyFingerprint: 'SM4-GCM#1A2B-3C4D',
    usageScope: '学生错题 / 认知画像',
    activatedTime: '2025-06-01 10:00:00',
    createTime: '2025-06-01 10:00:00'
  },
  {
    id: 3,
    tenantId: 1001,
    keyAlias: 'edumind-model-key',
    keyVersion: 1,
    algorithm: 'SM4-GCM',
    status: 'ACTIVE',
    keyFingerprint: 'SM4-GCM#8B12-4E90',
    usageScope: 'DeepSeek API Key',
    activatedTime: '2026-02-01 10:00:00',
    createTime: '2026-02-01 10:00:00'
  }
];

describe('useSecurityKey helpers', () => {
  it('filters keys by alias', () => {
    const result = filterSecurityKeys(sampleKeys, 'edumind-data-key', '', '');
    expect(result).toHaveLength(2);
    expect(result.every(item => item.keyAlias === 'edumind-data-key')).toBe(true);
  });

  it('filters keys by status', () => {
    const result = filterSecurityKeys(sampleKeys, '', 'ACTIVE', '');
    expect(result).toHaveLength(2);
    expect(result.every(item => item.status === 'ACTIVE')).toBe(true);
  });

  it('filters keys by search keyword across alias, version, fingerprint and algorithm', () => {
    expect(filterSecurityKeys(sampleKeys, '', '', 'model')).toHaveLength(1);
    expect(filterSecurityKeys(sampleKeys, '', '', 'v2')).toHaveLength(1);
    expect(filterSecurityKeys(sampleKeys, '', '', '8b12')).toHaveLength(1);
    expect(filterSecurityKeys(sampleKeys, '', '', 'sm4-gcm')).toHaveLength(3);
    expect(filterSecurityKeys(sampleKeys, '', '', 'missing')).toHaveLength(0);
  });

  it('paginates list slices', () => {
    expect(paginateList(sampleKeys, 1, 2)).toHaveLength(2);
    expect(paginateList(sampleKeys, 2, 2)).toHaveLength(1);
    expect(paginateList(sampleKeys, 3, 2)).toHaveLength(0);
  });

  it('resolves active key and version by alias', () => {
    expect(getActiveKeyByAlias(sampleKeys, 'edumind-data-key')?.keyVersion).toBe(2);
    expect(getActiveKeyByAlias(sampleKeys, 'edumind-model-key')?.keyVersion).toBe(1);
    expect(getActiveKeyByAlias(sampleKeys, 'unknown')).toBeNull();
    expect(getActiveVerByAlias(sampleKeys, 'edumind-data-key')).toBe(2);
    expect(getActiveVerByAlias(sampleKeys)).toBe(1);
  });

  it('collects distinct aliases', () => {
    expect(getDistinctAliases(sampleKeys)).toEqual(['edumind-data-key', 'edumind-model-key']);
  });
});

describe('useSecurityKey composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads key list via fetchList', async () => {
    const { listSecurityKeys } = await import('@/api/system/security-key');
    vi.mocked(listSecurityKeys).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: sampleKeys,
      timestamp: Date.now()
    });

    const { useSecurityKey } = await import('./useSecurityKey');
    const { keyList, activeDataKey, filteredKeyList, fetchList } = useSecurityKey();

    await fetchList();

    expect(keyList.value).toHaveLength(3);
    expect(activeDataKey.value?.keyVersion).toBe(2);
    expect(filteredKeyList.value).toHaveLength(3);
    expect(listSecurityKeys).toHaveBeenCalled();
  });
});
