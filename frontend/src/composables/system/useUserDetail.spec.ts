import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  isUserEnabled,
  getRoleLabel,
  getRoleDescription,
  parseOperSummary
} from './useUserDetail';

vi.mock('@/api/system/user', () => ({
  getUserDetail: vi.fn(),
  updateUserStatus: vi.fn(),
  resetUserPassword: vi.fn()
}));

vi.mock('@/api/system/oper-log', () => ({
  getUserOperLogs: vi.fn()
}));

vi.mock('@/stores/auth/auth', () => ({
  useAuthStore: () => ({
    currentUser: { id: 99 },
    setUser: vi.fn()
  })
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '2' } })
}));

describe('useUserDetail helpers', () => {
  it('detects enabled user status', () => {
    expect(isUserEnabled({ status: 'ENABLE' })).toBe(true);
    expect(isUserEnabled({ status: 1 })).toBe(true);
    expect(isUserEnabled({ status: 'DISABLE' })).toBe(false);
  });

  it('maps role labels and descriptions', () => {
    expect(getRoleLabel('ROLE_ADMIN')).toBe('系统管理员');
    expect(getRoleLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(getRoleDescription('ROLE_TEACHER')).toContain('教学大纲');
    expect(getRoleDescription('CUSTOM')).toBe('拥有对应领域标准业务操作与资源访问权限。');
  });

  it('parses oper log summary from operParam', () => {
    expect(parseOperSummary({ title: '用户管理' })).toBe('用户管理');
    expect(
      parseOperSummary({
        title: '更新用户',
        operParam: JSON.stringify({ action: '冻结账号' })
      })
    ).toBe('冻结账号');
    expect(
      parseOperSummary({
        title: '创建用户',
        operParam: JSON.stringify({ params: { username: 'alice' } })
      })
    ).toBe('创建用户「alice」');
  });
});

