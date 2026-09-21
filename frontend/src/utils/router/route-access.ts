import type { Router } from 'vue-router';
import { useAuthStore } from '@/stores/auth/auth';

/**
 * 判断当前登录用户是否有权访问目标路由（依据路由 meta.roles / meta.permissions）。
 *
 * 用途：把「能力可见性」与路由守卫的判定口径统一起来。
 * 直接读路由 meta 而不是在业务代码里再维护一份角色名单，避免两边不一致；
 * 同时可在跳转前做前置校验，避免把用户送到守卫那里再被弹「权限不足」并踢回工作台。
 */
export function canAccessRoute(router: Router, path: string): boolean {
  const resolved = router.resolve(path);
  const roles = resolved.meta?.roles as string[] | undefined;
  if (roles?.length && !useAuthStore().hasAnyRole(roles)) {
    return false;
  }
  const permissions = resolved.meta?.permissions as string[] | undefined;
  if (permissions?.length && !useAuthStore().hasAnyPermission(permissions)) {
    return false;
  }
  return true;
}
