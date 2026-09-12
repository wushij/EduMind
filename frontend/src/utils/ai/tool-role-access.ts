import type { AITool } from '@/types/ai/tool';
import { RoleEnum } from '@/constants/auth';

/** 当前角色可见的工具分类（ADMIN 返回 null 表示不过滤） */
export function getVisibleToolCategories(role: string | null): string[] | null {
  if (!role || role === RoleEnum.ADMIN) {
    return null;
  }
  if (role === RoleEnum.TEACHER) {
    return ['TEACHER', 'GENERAL'];
  }
  if (role === RoleEnum.STUDENT) {
    return ['STUDENT', 'GENERAL'];
  }
  return ['GENERAL'];
}

export function isToolVisibleForRole(tool: Pick<AITool, 'category'>, role: string | null): boolean {
  const allowed = getVisibleToolCategories(role);
  if (!allowed) return true;
  return allowed.includes(tool.category);
}

export function filterToolsByRole<T extends Pick<AITool, 'category'>>(tools: T[], role: string | null): T[] {
  const allowed = getVisibleToolCategories(role);
  if (!allowed) return tools;
  return tools.filter((tool) => allowed.includes(tool.category));
}

/** AI 广场分类 Tab / 侧边栏子菜单可见性（与 AppSidebar 规则一致） */
export function canAccessMarketplaceCategory(category: string, role: string | null): boolean {
  if (!role || role === RoleEnum.ADMIN) return true;
  if (category === 'TEACHER') {
    return role === RoleEnum.TEACHER;
  }
  if (category === 'STUDENT') {
    return role === RoleEnum.STUDENT;
  }
  return true;
}
