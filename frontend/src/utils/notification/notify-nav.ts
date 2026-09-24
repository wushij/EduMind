import type { NotificationType } from '@/types/notification';
import { RoleEnum } from '@/constants/auth';
import { useAuthStore } from '@/stores/auth/auth';

/**
 * 当前用户是否属于「教学管理侧」（教师 / 管理员）。
 * 与路由 meta.roles 判定口径保持一致：教师管理页对纯学生不可达。
 */
function isTeachingSide(): boolean {
  const authStore = useAuthStore();
  return authStore.hasAnyRole([RoleEnum.TEACHER]);
}

/**
 * 根据通知类型与关联 ID 推断跳转路径
 */
export function getNotificationNavigatePath(type: NotificationType, refId?: number): string | null {
  if (!refId) return null;
  switch (type) {
    case 'KNOWLEDGE_INDEX':
      return `/knowledge/${refId}`;
    case 'COURSE':
      return `/course/${refId}/overview`;
    case 'ASSIGNMENT':
      // 作业通知按角色分流：/question/assignments/:id 是 ADMIN/TEACHER 专属路由，
      // 学生收到「作业催交提醒」若跳该地址会被路由守卫拦截并踢回工作台。
      return isTeachingSide()
        ? `/question/assignments/${refId}`
        : `/learning/assignments/${refId}/take`;
    case 'EXAM':
      return `/question/exams/${refId}`;
    case 'AI_TASK':
      return '/course/ai';
    default:
      return null;
  }
}
