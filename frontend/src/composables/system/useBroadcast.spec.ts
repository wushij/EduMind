import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { NotificationBroadcastVO } from '@/types/notification/broadcast';
import {
  priorityLabel,
  priorityTagType,
  audienceLabel,
  audienceTagType,
  calcPercent,
  getReachProgressColor,
  filterBroadcasts,
  resolveSenderAvatar,
  submitBroadcast,
  estimateAudience,
  fetchBroadcastRecipients
} from './useBroadcast';

vi.mock('@/api/notification/broadcast', () => ({
  listBroadcasts: vi.fn(),
  getBroadcastStats: vi.fn(),
  deleteBroadcast: vi.fn(),
  clearAllBroadcasts: vi.fn(),
  createBroadcast: vi.fn(),
  estimateBroadcastAudience: vi.fn(),
  getBroadcastRecipients: vi.fn()
}));

vi.mock('@/stores/auth/auth', () => ({
  useAuthStore: () => ({
    currentUser: { id: 1, username: 'admin', avatar: 'https://example.com/a.png' },
    hasPermission: () => true
  })
}));

const broadcasts: NotificationBroadcastVO[] = [
  { id: 1, title: '开学通知', content: '欢迎返校', priority: 0, targetType: 'all', notifyType: 'BROADCAST', senderId: 1, senderName: '系统', createTime: '2026-01-01', readCount: 80, totalCount: 100 },
  { id: 2, title: '紧急停课', content: '台风预警', priority: 2, targetType: 'role', targetPayload: 'STUDENT', notifyType: 'BROADCAST', senderId: 1, senderName: '系统', createTime: '2026-01-01', readCount: 10, totalCount: 50 }
];

describe('useBroadcast helpers', () => {
  it('maps priority labels and tag types', () => {
    expect(priorityLabel(2)).toBe('紧急公告');
    expect(priorityTagType(1)).toBe('warning');
  });

  it('maps audience labels and tag types', () => {
    expect(audienceLabel('all')).toBe('全体用户');
    expect(audienceLabel('role', 'TEACHER')).toBe('教师');
    expect(audienceTagType('role', 'STUDENT')).toBe('warning');
  });

  it('calculates read percent and progress color', () => {
    expect(calcPercent(80, 100)).toBe(80);
    expect(calcPercent(0, 0)).toBe(0);
    expect(getReachProgressColor(90, 100)).toBe('#10b981');
    expect(getReachProgressColor(20, 100)).toBe('#f59e0b');
  });

  it('filters broadcasts by keyword and priority', () => {
    expect(filterBroadcasts(broadcasts, '', '')).toHaveLength(2);
    expect(filterBroadcasts(broadcasts, '台风', '')).toHaveLength(1);
    expect(filterBroadcasts(broadcasts, '', 2)).toHaveLength(1);
  });

  it('resolves sender avatar from current user', () => {
    const row = { id: 1, title: 't', senderId: 1, senderName: 'admin' } as NotificationBroadcastVO;
    expect(resolveSenderAvatar(row, { id: 1, username: 'admin', avatar: 'https://x.png' })).toBe('https://x.png');
  });

  it('exports broadcast action wrappers', async () => {
    const {
      createBroadcast,
      estimateBroadcastAudience,
      getBroadcastRecipients
    } = await import('@/api/notification/broadcast');

    vi.mocked(createBroadcast).mockResolvedValue({ code: 200, message: 'ok', data: broadcasts[0], timestamp: Date.now() });
    vi.mocked(estimateBroadcastAudience).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { estimatedCount: 10, targetType: 'all', formattedDesc: '预计触达 10 人' },
      timestamp: Date.now()
    });
    vi.mocked(getBroadcastRecipients).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        broadcastId: 1,
        broadcastTitle: 't',
        totalCount: 0,
        readCount: 0,
        unreadCount: 0,
        readRate: 0,
        recipients: { list: [], total: 0 }
      },
      timestamp: Date.now()
    });

    await submitBroadcast({
      title: 't',
      content: 'c',
      targetType: 'all',
      priority: 0
    });
    await estimateAudience({ targetType: 'all' });
    await fetchBroadcastRecipients(1, { page: 1, pageSize: 10 });

    expect(createBroadcast).toHaveBeenCalled();
    expect(estimateBroadcastAudience).toHaveBeenCalled();
    expect(getBroadcastRecipients).toHaveBeenCalledWith(1, { page: 1, pageSize: 10 });
  });
});

describe('useBroadcast composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads broadcast list', async () => {
    const { listBroadcasts } = await import('@/api/notification/broadcast');
    vi.mocked(listBroadcasts).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: broadcasts, total: 2 },
      timestamp: Date.now()
    });

    const { useBroadcast } = await import('./useBroadcast');
    const { rawTableData, filteredTableData, fetchData } = useBroadcast();

    await fetchData();

    expect(rawTableData.value).toHaveLength(2);
    expect(filteredTableData.value).toHaveLength(2);
  });
});
