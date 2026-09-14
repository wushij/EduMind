import type { PermissionVO } from '@/types/system/rbac';

export interface SidebarModuleMeta {
  key: string;
  name: string;
  icon: string;
  path: string;
  order: number;
}

/**
 * 严格对齐 AppSidebar.vue 侧边栏的 9 大核心业务模块规范（第 1 级：顶级目录）
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
  'ai-compute': {
    key: 'ai-compute',
    name: 'AI 智算中心',
    icon: 'Cpu',
    path: '/system/models',
    order: 7
  },
  system: {
    key: 'system',
    name: '系统管理',
    icon: 'Setting',
    path: '/system/users',
    order: 8
  },
  notice: {
    key: 'notice',
    name: '消息通知',
    icon: 'Bell',
    path: '/notice',
    order: 9
  }
};

export interface SidebarSubMenuMeta {
  key: string;
  moduleKey: string;
  name: string;
  path: string;
  icon: string;
  sort: number;
}

/**
 * 严格对齐 AppSidebar.vue 侧边栏的页面级二级菜单规范（第 2 级：页面菜单）
 */
export const SIDEBAR_SUBMENUS: Record<string, SidebarSubMenuMeta> = {
  // 1. 课程中心 (course)
  'course-list': {
    key: 'course-list',
    moduleKey: 'course',
    name: '我的课程',
    path: '/course',
    icon: 'Collection',
    sort: 1
  },
  'course-create': {
    key: 'course-create',
    moduleKey: 'course',
    name: '创建课程',
    path: '/course/create',
    icon: 'DocumentAdd',
    sort: 2
  },
  'course-ai': {
    key: 'course-ai',
    moduleKey: 'course',
    name: '课程 AI 助手',
    path: '/course/ai-assistant',
    icon: 'ChatDotRound',
    sort: 3
  },

  // 2. AI 教学 (ai-teaching)
  'ai-chat': {
    key: 'ai-chat',
    moduleKey: 'ai-teaching',
    name: 'AI 助手',
    path: '/ai/assistant/chat',
    icon: 'Service',
    sort: 1
  },
  'ai-question': {
    key: 'ai-question',
    moduleKey: 'ai-teaching',
    name: 'AI 出题',
    path: '/ai/question/generate',
    icon: 'EditPen',
    sort: 2
  },
  'ai-exam': {
    key: 'ai-exam',
    moduleKey: 'ai-teaching',
    name: 'AI 组卷',
    path: '/ai/exam/generate',
    icon: 'Tickets',
    sort: 3
  },
  'ai-grading': {
    key: 'ai-grading',
    moduleKey: 'ai-teaching',
    name: 'AI 批改',
    path: '/ai/grading',
    icon: 'CircleCheck',
    sort: 4
  },
  'ai-agent': {
    key: 'ai-agent',
    moduleKey: 'ai-teaching',
    name: 'Agent 中心',
    path: '/ai/agent',
    icon: 'Cpu',
    sort: 5
  },

  // 3. 知识库 (knowledge)
  'knowledge-list': {
    key: 'knowledge-list',
    moduleKey: 'knowledge',
    name: '知识库管理',
    path: '/knowledge',
    icon: 'Folder',
    sort: 1
  },
  'knowledge-create': {
    key: 'knowledge-create',
    moduleKey: 'knowledge',
    name: '创建知识库',
    path: '/knowledge/create',
    icon: 'FolderAdd',
    sort: 2
  },
  'knowledge-rag': {
    key: 'knowledge-rag',
    moduleKey: 'knowledge',
    name: 'RAG 检索诊断',
    path: '/knowledge/rag-debug',
    icon: 'Operation',
    sort: 3
  },

  // 4. 题库与作业 (question)
  'question-list': {
    key: 'question-list',
    moduleKey: 'question',
    name: '题目管理',
    path: '/question/list',
    icon: 'Memo',
    sort: 1
  },
  'question-exam': {
    key: 'question-exam',
    moduleKey: 'question',
    name: '试卷管理',
    path: '/question/exams',
    icon: 'DocumentChecked',
    sort: 2
  },
  'question-assignment': {
    key: 'question-assignment',
    moduleKey: 'question',
    name: '作业任务',
    path: '/question/assignments',
    icon: 'Notebook',
    sort: 3
  },

  // 5. 教学分析 (analytics)
  'analytics-learning': {
    key: 'analytics-learning',
    moduleKey: 'analytics',
    name: '学情分析',
    path: '/analytics/learning',
    icon: 'TrendCharts',
    sort: 1
  },

  // 6. 教学资源 (resource)
  'resource-list': {
    key: 'resource-list',
    moduleKey: 'resource',
    name: '课件资源',
    path: '/resource',
    icon: 'FolderOpened',
    sort: 1
  },

  // 7. AI 智算中心 (ai-compute)
  'ai-models': {
    key: 'ai-models',
    moduleKey: 'ai-compute',
    name: 'AI 模型接入',
    path: '/system/models',
    icon: 'Cpu',
    sort: 1
  },
  'ai-prompts': {
    key: 'ai-prompts',
    moduleKey: 'ai-compute',
    name: 'Prompt 模板库',
    path: '/system/prompts',
    icon: 'ChatLineSquare',
    sort: 2
  },
  'ai-tools': {
    key: 'ai-tools',
    moduleKey: 'ai-compute',
    name: 'AI 教学工具',
    path: '/system/tools',
    icon: 'Operation',
    sort: 3
  },
  'ai-gateway': {
    key: 'ai-gateway',
    moduleKey: 'ai-compute',
    name: 'AI 网关监控',
    path: '/system/gateway',
    icon: 'Connection',
    sort: 4
  },
  'ai-quotas': {
    key: 'ai-quotas',
    moduleKey: 'ai-compute',
    name: '算力与配额管控',
    path: '/system/quotas',
    icon: 'Money',
    sort: 5
  },

  // 8. 系统管理 (system) - 用户、角色、菜单、权限在前，配置在后
  'system-users': {
    key: 'system-users',
    moduleKey: 'system',
    name: '用户管理',
    path: '/system/users',
    icon: 'User',
    sort: 1
  },
  'system-roles': {
    key: 'system-roles',
    moduleKey: 'system',
    name: '角色权限',
    path: '/system/roles',
    icon: 'Lock',
    sort: 2
  },
  'system-menus': {
    key: 'system-menus',
    moduleKey: 'system',
    name: '菜单管理',
    path: '/system/menus',
    icon: 'Operation',
    sort: 3
  },
  'system-permissions': {
    key: 'system-permissions',
    moduleKey: 'system',
    name: '权限分配',
    path: '/system/permissions',
    icon: 'Key',
    sort: 4
  },
  'system-tenants': {
    key: 'system-tenants',
    moduleKey: 'system',
    name: '租户与校区',
    path: '/system/tenants',
    icon: 'School',
    sort: 5
  },
  'system-organizations': {
    key: 'system-organizations',
    moduleKey: 'system',
    name: '组织架构',
    path: '/system/organizations',
    icon: 'Connection',
    sort: 6
  },
  'system-audit': {
    key: 'system-audit',
    moduleKey: 'system',
    name: 'AI 审计日志',
    path: '/system/audit',
    icon: 'Clock',
    sort: 7
  },
  'system-oper-log': {
    key: 'system-oper-log',
    moduleKey: 'system',
    name: '操作日志',
    path: '/system/oper-log',
    icon: 'Memo',
    sort: 8
  },
  'system-config': {
    key: 'system-config',
    moduleKey: 'system',
    name: '系统配置',
    path: '/system/config',
    icon: 'Monitor',
    sort: 9
  },

  // 9. 消息通知 (notice)
  'notice-list': {
    key: 'notice-list',
    moduleKey: 'notice',
    name: '系统通知',
    path: '/notice',
    icon: 'Bell',
    sort: 1
  },
  'notice-broadcast': {
    key: 'notice-broadcast',
    moduleKey: 'notice',
    name: '消息广播',
    path: '/system/notification-broadcast',
    icon: 'Promotion',
    sort: 2
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

/** 映射权限码到对应的二级页面菜单 Key */
export function mapCodeToSubMenuKey(code: string): string {
  const lower = code.toLowerCase();

  // 系统管理
  if (lower.startsWith('system:user')) return 'system-users';
  if (lower.startsWith('system:role')) return 'system-roles';
  if (lower.startsWith('system:menu')) return 'system-menus';
  if (lower.startsWith('system:permission')) return 'system-permissions';
  if (lower.startsWith('system:tenant')) return 'system-tenants';
  if (lower.startsWith('system:org') || lower.startsWith('system:organization')) return 'system-organizations';
  if (lower.startsWith('system:audit')) return 'system-audit';
  if (lower.startsWith('system:operlog') || lower.startsWith('system:oper-log') || lower.startsWith('system:oper')) return 'system-oper-log';
  if (lower.startsWith('system:config')) return 'system-config';

  // AI 智算中心
  if (lower.startsWith('system:model')) return 'ai-models';
  if (lower.startsWith('system:prompt')) return 'ai-prompts';
  if (lower.startsWith('system:tool')) return 'ai-tools';
  if (lower.startsWith('system:gateway')) return 'ai-gateway';
  if (lower.startsWith('system:quota')) return 'ai-quotas';

  // 课程中心
  if (lower.startsWith('course:create')) return 'course-create';
  if (lower.startsWith('course:ai')) return 'course-ai';
  if (lower.startsWith('course:')) return 'course-list';

  // AI 教学
  if (lower.startsWith('ai:chat')) return 'ai-chat';
  if (lower.startsWith('ai:question')) return 'ai-question';
  if (lower.startsWith('ai:exam')) return 'ai-exam';
  if (lower.startsWith('ai:grading')) return 'ai-grading';
  if (lower.startsWith('ai:tool')) return 'ai-agent';

  // 知识库
  if (lower.startsWith('knowledge:rag')) return 'knowledge-rag';
  if (lower.startsWith('knowledge:create') || lower.startsWith('knowledge:edit')) return 'knowledge-create';
  if (lower.startsWith('knowledge:')) return 'knowledge-list';

  // 题库与作业
  if (lower.startsWith('question:')) return 'question-list';
  if (lower.startsWith('exam:')) return 'question-exam';
  if (lower.startsWith('assignment:')) return 'question-assignment';

  // 教学分析
  if (lower.startsWith('analytics:')) return 'analytics-learning';

  // 教学资源
  if (lower.startsWith('resource:')) return 'resource-list';

  // 消息通知
  if (lower.startsWith('notice:broadcast')) return 'notice-broadcast';
  if (lower.startsWith('notice:')) return 'notice-list';

  return 'system-config';
}

/** 映射权限码到对应的侧边栏顶级模块 Key */
export function mapCodeToSidebarModuleKey(code: string): string {
  const subKey = mapCodeToSubMenuKey(code);
  const subMeta = SIDEBAR_SUBMENUS[subKey];
  return subMeta ? subMeta.moduleKey : 'system';
}

/** 推断按钮级操作元数据（第 3 级：按钮/权限节点） */
export function getPermissionActionMeta(code: string): {
  icon: string;
  sort: number;
} {
  const lower = code.toLowerCase();

  if (lower.endsWith(':view') || lower.endsWith(':list') || lower.endsWith(':query')) {
    return { icon: 'Pointer', sort: 1 };
  }
  if (lower.endsWith(':add') || lower.endsWith(':create') || lower.endsWith(':upload')) {
    return { icon: 'Plus', sort: 2 };
  }
  if (lower.endsWith(':edit') || lower.endsWith(':update') || lower.endsWith(':grade')) {
    return { icon: 'EditPen', sort: 3 };
  }
  if (lower.endsWith(':delete') || lower.endsWith(':remove') || lower.endsWith(':clear')) {
    return { icon: 'Delete', sort: 4 };
  }
  if (lower.endsWith(':export')) {
    return { icon: 'Download', sort: 5 };
  }
  if (lower.endsWith(':assign')) {
    return { icon: 'User', sort: 6 };
  }
  if (lower.endsWith(':use') || lower.endsWith(':chat')) {
    return { icon: 'Cpu', sort: 7 };
  }
  if (lower.endsWith(':send')) {
    return { icon: 'Promotion', sort: 8 };
  }

  return { icon: 'Key', sort: 9 };
}

/** 兼容旧接口的元数据推断函数 */
export function getPermissionRouteMeta(code: string): {
  type: 1 | 2 | 3;
  path: string;
  icon: string;
  sort: number;
} {
  const subKey = mapCodeToSubMenuKey(code);
  const subMeta = SIDEBAR_SUBMENUS[subKey];
  const actMeta = getPermissionActionMeta(code);
  const path = subMeta ? subMeta.path : `/${code.replace(/:/g, '/')}`;
  return {
    type: 3,
    path,
    icon: actMeta.icon,
    sort: actMeta.sort
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
 * 将权限数据构建为 100% 对应侧边栏导航架构的三级目录树形结构：
 * Level 1: 顶级模块（目录，Type 1）- 如「系统管理」、「AI 智算中心」
 * Level 2: 页面菜单（菜单，Type 2）- 如「用户管理」、「角色权限」、「AI 模型接入」
 * Level 3: 按钮与操作权限（按钮，Type 3）- 如「用户档案查看」、「用户档案编辑」
 */
export function buildSidebarMenuTree(
  permissions: PermissionVO[],
  keyword?: string,
  typeFilter?: number
): SysMenuNode[] {
  const flat = flattenPermissions(permissions);
  const kw = (keyword || '').trim().toLowerCase();

  // 1. 将所有细粒度权限归类到所属二级菜单 (SubMenu)
  const subMenuPermissionsMap = new Map<string, PermissionVO[]>();
  for (const item of flat) {
    const subKey = mapCodeToSubMenuKey(item.permissionCode);
    if (!subMenuPermissionsMap.has(subKey)) {
      subMenuPermissionsMap.set(subKey, []);
    }
    subMenuPermissionsMap.get(subKey)!.push(item);
  }

  // 2. 构建三级树形数据
  const tree: SysMenuNode[] = [];

  // 遍历所有顶层模块
  const sortedModules = Object.values(SIDEBAR_MODULES).sort((a, b) => a.order - b.order);

  for (const modMeta of sortedModules) {
    // 找出该模块下的所有二级页面菜单
    const subMenusOfModule = Object.values(SIDEBAR_SUBMENUS)
      .filter((s) => s.moduleKey === modMeta.key)
      .sort((a, b) => a.sort - b.sort);

    const level2Children: SysMenuNode[] = [];

    for (const subMeta of subMenusOfModule) {
      const items = subMenuPermissionsMap.get(subMeta.key) || [];

      // 构建 Level 3 按钮节点
      const level3Nodes: SysMenuNode[] = items.map((p) => {
        const actMeta = getPermissionActionMeta(p.permissionCode);
        return {
          id: p.id,
          rawId: p.id,
          rowKey: `btn-${p.id}`,
          name: p.permissionName,
          icon: actMeta.icon,
          type: 3, // 按钮 / 操作权限
          path: subMeta.path,
          permission: p.permissionCode,
          sort: actMeta.sort
        };
      });

      // 排序 Level 3 按钮
      level3Nodes.sort((a, b) => a.sort - b.sort);

      // 搜索过滤与类型过滤检查
      const subMatchesKw = !kw ||
        subMeta.name.toLowerCase().includes(kw) ||
        subMeta.path.toLowerCase().includes(kw) ||
        modMeta.name.toLowerCase().includes(kw);

      const filteredLevel3 = kw
        ? level3Nodes.filter((btn) =>
            btn.name.toLowerCase().includes(kw) ||
            (btn.permission && btn.permission.toLowerCase().includes(kw))
          )
        : level3Nodes;

      // 如果有关键字搜索：要么子节点匹配，要么二级菜单本身匹配
      const shouldKeepSubMenu = kw
        ? subMatchesKw || filteredLevel3.length > 0
        : true;

      if (!shouldKeepSubMenu) continue;

      // 类型过滤：如果要求只看某个类型
      const finalLevel3 = typeFilter === 1 ? [] : filteredLevel3;

      if (typeFilter === 3 && finalLevel3.length === 0) continue;

      level2Children.push({
        id: `menu-${subMeta.key}`,
        rowKey: `sub-${subMeta.key}`,
        name: subMeta.name,
        icon: subMeta.icon,
        type: 2, // 页面菜单
        path: subMeta.path,
        permission: '',
        sort: subMeta.sort,
        children: finalLevel3.length > 0 ? finalLevel3 : undefined
      });
    }

    if (level2Children.length === 0) continue;

    tree.push({
      id: `mod-${modMeta.key}`,
      rowKey: `mod-${modMeta.key}`,
      name: modMeta.name,
      icon: modMeta.icon,
      type: 1, // 顶级目录
      path: modMeta.path,
      permission: '',
      sort: modMeta.order,
      children: level2Children
    });
  }

  return tree;
}
