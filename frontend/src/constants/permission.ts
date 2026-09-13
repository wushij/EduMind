import type { PermissionVO } from '@/types/system/rbac';

export interface SidebarModuleMeta {
  key: string;
  name: string;
  icon: string;
  path: string;
  order: number;
}

/**
 * 严格对齐 AppSidebar.vue 侧边栏的 8 大核心业务模块规范
 */
export const SIDEBAR_MODULES: Record<string, SidebarModuleMeta> = {
  course: {
    key: 'course',
    name: '课程中心',
    icon: 'Reading',
    path: '/course',
    order: 1
  },
  'ai-teaching': {
    key: 'ai-teaching',
    name: 'AI 教学',
    icon: 'MagicStick',
    path: '/ai',
    order: 2
  },
  knowledge: {
    key: 'knowledge',
    name: '知识库',
    icon: 'FolderOpened',
    path: '/knowledge',
    order: 3
  },
  question: {
    key: 'question',
    name: '题库与作业',
    icon: 'Document',
    path: '/question',
    order: 4
  },
  analytics: {
    key: 'analytics',
    name: '教学分析',
    icon: 'DataAnalysis',
    path: '/analytics',
    order: 5
  },
  resource: {
    key: 'resource',
    name: '教学资源',
    icon: 'Files',
    path: '/resource',
    order: 6
  },
  system: {
    key: 'system',
    name: '系统管理',
    icon: 'Setting',
    path: '/system',
    order: 7
  },
  notice: {
    key: 'notice',
    name: '消息通知',
    icon: 'Bell',
    path: '/notice',
    order: 8
  }
};

export interface SysMenuNode {
  id: number | string;
  rawId?: number;
  rowKey: string;
  name: string;
  icon: string;
  type: 1 | 2 | 3; // 1: 目录, 2: 菜单, 3: 按钮/功能
  path?: string;
  permission?: string;
  sort: number;
  children?: SysMenuNode[];
}

/** 映射权限码到对应的侧边栏顶级模块 Key */
export function mapCodeToSidebarModuleKey(code: string): string {
  const lower = code.toLowerCase();
  if (lower.startsWith('course:')) return 'course';
  if (lower.startsWith('ai:')) return 'ai-teaching';
  if (lower.startsWith('knowledge:')) return 'knowledge';
  if (lower.startsWith('question:') || lower.startsWith('exam:') || lower.startsWith('assignment:')) {
    return 'question';
  }
  if (lower.startsWith('analytics:')) return 'analytics';
  if (lower.startsWith('resource:')) return 'resource';
  if (lower.startsWith('system:')) return 'system';
  if (lower.startsWith('notice:')) return 'notice';
  return 'system';
}

/** 推断路由路径与类型 (对齐 AppSidebar.vue 菜单项) */
export function getPermissionRouteMeta(code: string): {
  type: 1 | 2 | 3;
  path: string;
  icon: string;
  sort: number;
} {
  const map: Record<string, { type: 1 | 2 | 3; path: string; icon: string; sort: number }> = {
    // 课程中心
    'course:view': { type: 2, path: '/course', icon: 'Collection', sort: 1 },
    'course:create': { type: 3, path: '/course/create', icon: 'DocumentAdd', sort: 2 },
    'course:edit': { type: 3, path: '/course/edit', icon: 'EditPen', sort: 3 },

    // AI 教学
    'ai:chat': { type: 2, path: '/ai/assistant/chat', icon: 'Service', sort: 1 },
    'ai:question': { type: 3, path: '/ai/question/generate', icon: 'EditPen', sort: 2 },
    'ai:exam': { type: 3, path: '/ai/exam/generate', icon: 'Tickets', sort: 3 },
    'ai:grading': { type: 3, path: '/ai/grading', icon: 'CircleCheck', sort: 4 },
    'ai:tool:use': { type: 3, path: '/ai/agent', icon: 'Cpu', sort: 5 },

    // 知识库
    'knowledge:view': { type: 2, path: '/knowledge', icon: 'Folder', sort: 1 },
    'knowledge:edit': { type: 3, path: '/knowledge/create', icon: 'FolderAdd', sort: 2 },
    'knowledge:rag:debug': { type: 3, path: '/knowledge/rag-debug', icon: 'Operation', sort: 3 },

    // 题库与作业
    'question:view': { type: 2, path: '/question/list', icon: 'Memo', sort: 1 },
    'question:edit': { type: 3, path: '/question/edit', icon: 'EditPen', sort: 2 },
    'exam:view': { type: 2, path: '/question/exams', icon: 'DocumentChecked', sort: 3 },
    'exam:edit': { type: 3, path: '/question/exams/edit', icon: 'Tickets', sort: 4 },
    'assignment:view': { type: 2, path: '/question/assignments', icon: 'Notebook', sort: 5 },
    'assignment:create': { type: 3, path: '/question/assignments/create', icon: 'DocumentAdd', sort: 6 },
    'assignment:grade': { type: 3, path: '/question/assignments/grade', icon: 'CircleCheck', sort: 7 },

    // 教学分析
    'analytics:view': { type: 2, path: '/analytics/learning', icon: 'TrendCharts', sort: 1 },

    // 教学资源
    'resource:view': { type: 2, path: '/resource', icon: 'FolderOpened', sort: 1 },
    'resource:upload': { type: 3, path: '/resource/upload', icon: 'Upload', sort: 2 },

    // 系统管理
    'system:tenant:view': { type: 2, path: '/system/tenants', icon: 'School', sort: 1 },
    'system:org:view': { type: 2, path: '/system/organizations', icon: 'Connection', sort: 2 },
    'system:user:view': { type: 2, path: '/system/users', icon: 'User', sort: 3 },
    'system:user:edit': { type: 3, path: '/system/users/edit', icon: 'EditPen', sort: 4 },
    'system:role:view': { type: 2, path: '/system/roles', icon: 'Lock', sort: 5 },
    'system:role:edit': { type: 3, path: '/system/roles/edit', icon: 'Key', sort: 6 },
    'system:model:view': { type: 2, path: '/system/models', icon: 'Cpu', sort: 7 },
    'system:prompt:view': { type: 2, path: '/system/prompts', icon: 'ChatLineSquare', sort: 8 },
    'system:quota:view': { type: 2, path: '/system/quotas', icon: 'Money', sort: 9 },

    // 消息通知
    'notice:view': { type: 2, path: '/notice', icon: 'Bell', sort: 1 }
  };

  if (map[code]) return map[code];

  const lower = code.toLowerCase();
  const isButton =
    lower.endsWith(':edit') ||
    lower.endsWith(':create') ||
    lower.endsWith(':delete') ||
    lower.endsWith(':upload') ||
    lower.endsWith(':grade') ||
    lower.endsWith(':use');
  return {
    type: isButton ? 3 : 2,
    path: `/${code.replace(/:/g, '/')}`,
    icon: isButton ? 'Pointer' : 'Document',
    sort: 10
  };
}

/** 递归平铺权限节点 */
export function flattenPermissions(nodes: PermissionVO[]): PermissionVO[] {
  const list: PermissionVO[] = [];
  const walk = (items: PermissionVO[]) => {
    for (const item of items) {
      list.push({
        id: item.id,
        permissionCode: item.permissionCode,
        permissionName: item.permissionName,
        permissionType: item.permissionType,
        parentId: item.parentId
      });
      if (item.children && item.children.length > 0) {
        walk(item.children);
      }
    }
  };
  walk(nodes);
  return list;
}

/**
 * 将权限数据构建为 100% 对齐侧边栏菜单架构的树形结构
 */
export function buildSidebarMenuTree(
  permissions: PermissionVO[],
  keyword?: string,
  typeFilter?: number
): SysMenuNode[] {
  const flat = flattenPermissions(permissions);
  const kw = (keyword || '').trim().toLowerCase();

  // 1. 过滤叶子节点
  const filtered = flat.filter((item) => {
    const meta = getPermissionRouteMeta(item.permissionCode);
    if (typeFilter && typeFilter !== 0 && meta.type !== typeFilter) {
      return false;
    }
    if (!kw) return true;
    const matchName = item.permissionName.toLowerCase().includes(kw);
    const matchCode = item.permissionCode.toLowerCase().includes(kw);
    const matchPath = meta.path.toLowerCase().includes(kw);
    return matchName || matchCode || matchPath;
  });

  // 2. 按侧边栏模块聚类
  const map = new Map<string, PermissionVO[]>();
  for (const item of filtered) {
    const moduleKey = mapCodeToSidebarModuleKey(item.permissionCode);
    if (!map.has(moduleKey)) {
      map.set(moduleKey, []);
    }
    map.get(moduleKey)!.push(item);
  }

  // 3. 构建顶级目录节点与子菜单/按钮节点
  const tree: SysMenuNode[] = [];
  for (const [moduleKey, items] of map.entries()) {
    const modMeta = SIDEBAR_MODULES[moduleKey] || {
      key: moduleKey,
      name: moduleKey.toUpperCase(),
      icon: 'Setting',
      path: `/${moduleKey}`,
      order: 99
    };

    const children: SysMenuNode[] = items.map((p) => {
      const pMeta = getPermissionRouteMeta(p.permissionCode);
      return {
        id: p.id,
        rawId: p.id,
        rowKey: `p-${p.id}`,
        name: p.permissionName,
        icon: pMeta.icon,
        type: pMeta.type,
        path: pMeta.path,
        permission: p.permissionCode,
        sort: pMeta.sort
      };
    });

    children.sort((a, b) => a.sort - b.sort);

    tree.push({
      id: `mod-${moduleKey}`,
      rowKey: `m-${moduleKey}`,
      name: modMeta.name,
      icon: modMeta.icon,
      type: 1, // 顶级目录
      path: modMeta.path,
      permission: '',
      sort: modMeta.order,
      children
    });
  }

  tree.sort((a, b) => a.sort - b.sort);
  return tree;
}
