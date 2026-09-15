import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { OperLogVO } from '@/types/system/oper-log';
import {
  getRoleLabel,
  getSceneLabel,
  getSceneStyleClass,
  getLatencyClass,
  businessTypeLabel,
  getMethodBadgeClass,
  getActionSummary,
  formatJson,
  parseDiffItems,
  buildRecentDateRange
} from './useAudit';

vi.mock('@/api/system/audit', () => ({
  getAuditSummary: vi.fn(),
  getAuditLogs: vi.fn(),
  getDailyTrend: vi.fn(),
  getAvailableAiModels: vi.fn()
}));

vi.mock('@/api/system/oper-log', () => ({
  pageOperLog: vi.fn(),
  getOperLogStats: vi.fn(),
  deleteOperLog: vi.fn(),
  batchDeleteOperLog: vi.fn(),
  cleanOperLog: vi.fn()
}));

describe('useAudit shared helpers', () => {
  it('maps role and scene labels', () => {
    expect(getRoleLabel('ADMIN')).toBe('管理员');
    expect(getSceneLabel('CHAT')).toContain('CHAT');
    expect(getSceneStyleClass('GRADING')).toBe('badge-grading');
    expect(getLatencyClass(200)).toBe('lat-fast');
    expect(getLatencyClass(5000)).toBe('lat-danger');
  });

  it('labels business types and HTTP methods', () => {
    expect(businessTypeLabel(1)).toBe('新增');
    expect(getMethodBadgeClass('GET')).toBe('method--get');
  });

  it('builds action summary from oper log row', () => {
    const row: OperLogVO = {
      id: 1,
      title: '用户管理',
      businessType: 1,
      operParam: JSON.stringify({ params: { username: 'alice' } }),
      requestMethod: 'POST',
      method: 'POST',
      operName: 'admin',
      operUrl: '/system/users',
      operIp: '127.0.0.1',
      status: 0,
      costTime: 10,
      operTime: '2026-01-01'
    };
    expect(getActionSummary(row)).toContain('alice');
  });

  it('formats json and parses diff items', () => {
    expect(formatJson(JSON.stringify({ params: { a: 1 } }))).toContain('"a": 1');
    expect(parseDiffItems(JSON.stringify({ diffItems: ['name: a -> b'] }))).toHaveLength(1);
  });

  it('builds recent date range', () => {
    const range = buildRecentDateRange(7);
    expect(range).toHaveLength(2);
    expect(range[0]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

describe('useAudit composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads audit logs', async () => {
    const { getAuditLogs } = await import('@/api/system/audit');
    vi.mocked(getAuditLogs).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        list: [{ id: 1, model: 'gpt-test', totalTokens: 100 }],
        total: 1
      },
      timestamp: Date.now()
    });

    const { useAudit } = await import('./useAudit');
    const { logs, total, loadLogs } = useAudit();

    await loadLogs();

    expect(logs.value).toHaveLength(1);
    expect(total.value).toBe(1);
  });
});
