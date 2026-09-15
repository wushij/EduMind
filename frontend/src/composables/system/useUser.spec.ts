import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  canDeleteUser,
  getDefaultUsers,
  getRoleLabel,
  getRoleTagType,
  isUserEnabled
} from './useUser';

vi.mock('@/api/system/user', () => ({
  getUsers: vi.fn(),
  createUser: vi.fn(),
  updateUserStatus: vi.fn(),
  deleteUser: vi.fn()
}));

vi.mock('@/api/system/role', () => ({
  getRoles: vi.fn()
}));

vi.mock('@/stores/auth/auth', () => ({
  useAuthStore: () => ({
    currentUser: { id: 99 }
  })
}));

describe('useUser helpers', () => {
  it('maps role labels and tag types', () => {
    expect(getRoleLabel('ADMIN')).toBe('管理员');
    expect(getRoleLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(getRoleTagType('TEACHER')).toBe('primary');
    expect(getRoleTagType('CUSTOM')).toBe('info');
  });

  it('detects enabled user status', () => {
    expect(isUserEnabled({ status: 'ENABLE' })).toBe(true);
    expect(isUserEnabled({ status: 'ENABLED' })).toBe(true);
    expect(isUserEnabled({ status: 'DISABLED' })).toBe(false);
  });

  it('prevents deleting admin and current user', () => {
    expect(canDeleteUser({ id: 1, username: 'admin' }, 99)).toBe(false);
    expect(canDeleteUser({ id: 99, username: 'teacher' }, 99)).toBe(false);
    expect(canDeleteUser({ id: 2, username: 'teacher' }, 99)).toBe(true);
  });

  it('provides default mock users with four entries', () => {
    const users = getDefaultUsers();
    expect(users).toHaveLength(4);
    expect(users.map((u) => u.username)).toEqual(['admin', 'teacher', 'student', 'student2']);
  });
});

describe('useUser composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initializes reactive state', async () => {
    const { getUsers } = await import('@/api/system/user');
    const { getRoles } = await import('@/api/system/role');

    vi.mocked(getUsers).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: [{ id: 1, username: 'admin', status: 'ENABLE' }], total: 1 },
      timestamp: Date.now()
    });
    vi.mocked(getRoles).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [{ id: 1, roleCode: 'ADMIN', roleName: 'Admin', permissions: [] }],
      timestamp: Date.now()
    });

    const { useUser } = await import('./useUser');
    const { users, total, pageNum, pageSize, loadUsers } = useUser();

    await loadUsers();

    expect(users.value.length).toBeGreaterThan(0);
    expect(total.value).toBe(1);
    expect(pageNum.value).toBe(1);
    expect(pageSize.value).toBe(10);
  });
});
