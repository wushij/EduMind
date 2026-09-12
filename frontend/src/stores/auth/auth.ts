import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { UserInfo } from '@/types/auth/auth';
import { RoleEnum, USER_INFO_KEY } from '@/constants/auth';
import { tokenUtil } from '@/core/auth/token';
import { storage } from '@/core/storage/local';
import { getUserInfo, logout as logoutApi, login as loginApi } from '@/api/auth/auth';
import { USE_MOCK } from '@/config/mock';
import { MOCK_USERS } from '@/mock/users';
import { normalizeAvatarUrl } from '@/utils/format/file';

const DEFAULT_ROLE_ACCOUNTS: Record<'ADMIN' | 'TEACHER' | 'STUDENT', { username: string; password: string }> = {
  ADMIN: { username: 'admin', password: 'admin123' },
  TEACHER: { username: 'teacher', password: 'admin123' },
  STUDENT: { username: 'student', password: 'admin123' }
};

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(tokenUtil.get());
  const rawStoredUser = storage.get(USER_INFO_KEY) as UserInfo | null;
  if (rawStoredUser?.avatar) {
    const norm = normalizeAvatarUrl(rawStoredUser.avatar);
    if (norm && norm !== rawStoredUser.avatar) {
      rawStoredUser.avatar = norm;
      storage.set(USER_INFO_KEY, rawStoredUser);
    }
  }
  const currentUser = ref<UserInfo | null>(rawStoredUser);
  const permissions = ref<string[]>(rawStoredUser?.permissions || []);
  const userInfoLoaded = ref(!!rawStoredUser);

  const currentRole = computed(() => currentUser.value?.roles?.[0] || null);

  const setToken = (newToken: string) => {
    token.value = newToken;
    tokenUtil.set(newToken);
  };

  const setUser = (user: UserInfo) => {
    if (user.avatar) {
      const norm = normalizeAvatarUrl(user.avatar);
      if (norm) {
        user.avatar = norm;
      }
    }
    currentUser.value = user;
    permissions.value = user.permissions || [];
    userInfoLoaded.value = true;
    storage.set(USER_INFO_KEY, user);
  };

  const hasRole = (role: string) => {
    return currentUser.value?.roles?.includes(role as RoleEnum) ?? false;
  };

  const hasAnyRole = (requiredRoles?: string[]) => {
    if (!requiredRoles || requiredRoles.length === 0) return true;
    if (!currentUser.value?.roles) return false;
    if (currentUser.value.roles.includes(RoleEnum.ADMIN)) return true;
    return requiredRoles.some(r => currentUser.value?.roles?.includes(r as RoleEnum));
  };

  const hasPermission = (code?: string | string[]) => {
    if (!code) return true;
    const codes = Array.isArray(code) ? code : [code];
    if (codes.length === 0) return true;
    if (currentUser.value?.roles?.includes(RoleEnum.ADMIN)) return true;
    return codes.some(c => permissions.value.includes(c));
  };

  const hasAnyPermission = (codes?: string[]) => {
    if (!codes || codes.length === 0) return true;
    return hasPermission(codes);
  };

  async function fetchUserInfo(): Promise<UserInfo | null> {
    if (!token.value) {
      return null;
    }
    try {
      const res = await getUserInfo();
      if (res?.data) {
        const user: UserInfo = {
          id: res.data.id,
          username: res.data.username,
          realName: res.data.realName || res.data.username,
          avatar: res.data.avatar || '',
          roles: (res.data.roles || []) as RoleEnum[],
          permissions: res.data.permissions || [],
          department: res.data.department,
          email: res.data.email
        };
        setUser(user);
        return user;
      }
    } catch {
      if (USE_MOCK) {
        return null;
      }
      logout();
    }
    return null;
  }

  /**
   * 角色快速切换（支持联调真实后端登录与纯 Mock 降级）
   */
  async function switchRole(role: 'ADMIN' | 'TEACHER' | 'STUDENT') {
    const creds = DEFAULT_ROLE_ACCOUNTS[role];
    if (!USE_MOCK && creds) {
      try {
        const res = await loginApi({
          username: creds.username,
          password: creds.password
        });
        if (res?.data?.token) {
          setToken(res.data.token);
          if (res.data.userInfo) {
            setUser({
              id: res.data.userInfo.id,
              username: res.data.userInfo.username,
              realName: res.data.userInfo.realName || res.data.userInfo.username,
              avatar: res.data.userInfo.avatar || '',
              roles: (res.data.userInfo.roles || []) as RoleEnum[],
              permissions: res.data.userInfo.permissions || [],
              department: res.data.userInfo.department,
              email: res.data.userInfo.email
            });
          } else {
            await fetchUserInfo();
          }
          return;
        }
      } catch (error) {
        console.warn('后端一键切换角色接口调用失败，降级使用本地 Mock 用户信息:', error);
      }
    }

    // Mock 模式或联调请求异常降级
    const mock = MOCK_USERS[role];
    if (mock) {
      setUser({
        id: mock.id,
        username: mock.username,
        realName: mock.realName,
        avatar: mock.avatar,
        roles: [RoleEnum[role]],
        permissions: mock.permissions || [],
        department: mock.department
      });
      setToken(mock.token);
    }
  }

  const switchMockRole = (role: 'ADMIN' | 'TEACHER' | 'STUDENT') => {
    return switchRole(role);
  };

  async function logout() {
    try {
      if (token.value) {
        await logoutApi();
      }
    } catch {
      // 忽略登出接口异常，仍清除本地态
    }
    token.value = null;
    currentUser.value = null;
    permissions.value = [];
    userInfoLoaded.value = false;
    tokenUtil.remove();
    storage.remove(USER_INFO_KEY);
  }

  return {
    token,
    currentUser,
    permissions,
    userInfoLoaded,
    currentRole,
    setToken,
    setUser,
    hasRole,
    hasAnyRole,
    hasPermission,
    hasAnyPermission,
    fetchUserInfo,
    switchRole,
    switchMockRole,
    logout
  };
});
