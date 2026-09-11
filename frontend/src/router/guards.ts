import { Router } from 'vue-router';
import { tokenUtil } from '@/core/auth/token';
import { useAuthStore } from '@/stores/auth/auth';
import { ElMessage } from 'element-plus';

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    document.title = (to.meta.title ? `${to.meta.title} - ` : '') + '智教云 · EduMind';

    const token = tokenUtil.get();
    const authStore = useAuthStore();

    if (to.meta.requiresAuth && !token) {
      next({
        path: '/auth/login',
        query: { redirect: to.fullPath }
      });
      return;
    }

    if (token && !authStore.userInfoLoaded && to.path !== '/auth/login') {
      try {
        await authStore.fetchUserInfo();
      } catch {
        authStore.logout();
        next({
          path: '/auth/login',
          query: { redirect: to.fullPath }
        });
        return;
      }
    }

    const requiredRoles = to.meta.roles as string[] | undefined;
    if (requiredRoles && requiredRoles.length > 0) {
      if (!authStore.hasAnyRole(requiredRoles)) {
        ElMessage.warning(`权限不足：访问该模块需要 [${requiredRoles.join(' / ')}] 权限`);
        next('/dashboard');
        return;
      }
    }

    const requiredPermissions = to.meta.permissions as string[] | undefined;
    if (requiredPermissions && requiredPermissions.length > 0) {
      if (!authStore.hasAnyPermission(requiredPermissions)) {
        ElMessage.warning('权限不足：您没有访问该功能的操作权限');
        next('/dashboard');
        return;
      }
    }

    next();
  });
}
