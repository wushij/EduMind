import type { PermissionVO } from '@/types/system/rbac';

export interface SidebarModuleMeta {
  key: string;
  name: string;
  icon: string;
  path: string;
  order: number;
}

/**
 * 严格对齐 AppSidebar.vue / mock 菜单的 9 大核心业务模块规范（第 1 级：顶级目录）
 * 注：课件资源无独立顶级菜单，归属课程详情 /course/:id/resources
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
  learning: {
    key: 'learning',
    name: '学习中心',
    icon: 'TrendCharts',
    path: '/learning',
    order: 5
  },
  analytics: {
    key: 'analytics',
    name: '教学分析',
    icon: 'DataAnalysis',
    path: '/analytics',
    order: 6
  },
  'ai-compute': {
    key: 'ai-compute',
    name: 'AI 运维',
    icon: 'Monitor',
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
  profile: {
    key: 'profile',
    name: '个人中心',
    icon: 'User',
    path: '/profile',
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
    name: '课程 AI',
    path: '/course/ai',
    icon: 'ChatDotRound',
    sort: 3
  },

  // 2. AI 教学 (ai-teaching)
  'ai-chat': {
    key: 'ai-chat',
    moduleKey: 'ai-teaching',
    name: '全局 AI 副驾驶',
    path: '',
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
  'ai-lesson': {
    key: 'ai-lesson',
    moduleKey: 'ai-teaching',
    name: 'AI 教案',
    path: '/ai/lesson',
    icon: 'Notebook',
    sort: 5
  },
  'ai-summary': {
    key: 'ai-summary',
    moduleKey: 'ai-teaching',
    name: 'AI 总结',
    path: '/ai/summary',
    icon: 'DocumentCopy',
    sort: 6
  },
  'ai-recommendation': {
    key: 'ai-recommendation',
    moduleKey: 'ai-teaching',
    name: 'AI 推荐',
    path: '/ai/recommendation',
    icon: 'Promotion',
    sort: 7
  },
  'ai-agent': {
    key: 'ai-agent',
    moduleKey: 'ai-teaching',
    name: 'Agent 中心',
    path: '/ai/agent',
    icon: 'Cpu',
    sort: 8
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
  'knowledge-ocr': {
    key: 'knowledge-ocr',
    moduleKey: 'knowledge',
    name: 'OCR 试卷识别',
    path: '/knowledge/ocr',
    icon: 'Search',
    sort: 3
  },
  'knowledge-chunks': {
    key: 'knowledge-chunks',
    moduleKey: 'knowledge',
    name: '切片管理',
    path: '/knowledge/chunks',
    icon: 'Grid',
    sort: 4
  },
  'knowledge-embeddings': {
    key: 'knowledge-embeddings',
    moduleKey: 'knowledge',
    name: '向量状态',
    path: '/knowledge/embeddings',
    icon: 'PieChart',
    sort: 5
  },
  'knowledge-rag': {
    key: 'knowledge-rag',
    moduleKey: 'knowledge',
    name: 'RAG 检索诊断',
    path: '/knowledge/rag-debug',
    icon: 'Operation',
    sort: 6
  },
  'knowledge-graph': {
    key: 'knowledge-graph',
    moduleKey: 'knowledge',
    name: '知识图谱',
    path: '/knowledge/graph',
    icon: 'Connection',
    sort: 7
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
  'question-bank': {
    key: 'question-bank',
    moduleKey: 'question',
    name: '题库管理',
    path: '/question/banks',
    icon: 'Collection',
    sort: 2
  },
  'question-exam': {
    key: 'question-exam',
    moduleKey: 'question',
    name: '试卷管理',
    path: '/question/exams',
    icon: 'DocumentChecked',
    sort: 3
  },
  'question-export': {
    key: 'question-export',
    moduleKey: 'question',
    name: '试卷导出中心',
    path: '/question/exports',
    icon: 'DocumentCopy',
    sort: 4
  },
  'question-assignment': {
    key: 'question-assignment',
    moduleKey: 'question',
    name: '作业任务',
    path: '/question/assignments',
    icon: 'Notebook',
    sort: 5
  },
  'question-submissions': {
    key: 'question-submissions',
    moduleKey: 'question',
    name: '批改记录',
    path: '/question/submissions',
    icon: 'Finished',
    sort: 6
  },

  // 5. 学习中心 (learning)
  'learning-dashboard': {
    key: 'learning-dashboard',
    moduleKey: 'learning',
    name: '学习总览',
    path: '/learning',
    icon: 'DataBoard',
    sort: 1
  },
  'learning-tasks': {
    key: 'learning-tasks',
    moduleKey: 'learning',
    name: '学习任务',
    path: '/learning/tasks',
    icon: 'List',
    sort: 2
  },
  'learning-practice': {
    key: 'learning-practice',
    moduleKey: 'learning',
    name: 'AI 练习',
    path: '/learning/practice',
    icon: 'MagicStick',
    sort: 3
  },
  'learning-wrong': {
    key: 'learning-wrong',
    moduleKey: 'learning',
    name: '错题本',
    path: '/learning/wrong-questions',
    icon: 'Warning',
    sort: 4
  },
  'learning-report': {
    key: 'learning-report',
    moduleKey: 'learning',
    name: '学习报告',
    path: '/learning/report',
    icon: 'DataLine',
    sort: 5
  },
  'learning-path': {
    key: 'learning-path',
    moduleKey: 'learning',
    name: '学习路径',
    path: '/learning/path',
    icon: 'MapLocation',
    sort: 6
  },

  // 6. 教学分析 (analytics)
  'analytics-overview': {
    key: 'analytics-overview',
    moduleKey: 'analytics',
    name: '课程概览',
    path: '/analytics',
    icon: 'Histogram',
    sort: 1
  },
  'analytics-learning': {
    key: 'analytics-learning',
    moduleKey: 'analytics',
    name: '学情分析',
    path: '/analytics/learning',
    icon: 'TrendCharts',
    sort: 2
  },
  'analytics-mastery': {
    key: 'analytics-mastery',
    moduleKey: 'analytics',
    name: '知识点掌握',
    path: '/analytics/mastery',
    icon: 'PieChart',
    sort: 3
  },
  'analytics-wrong': {
    key: 'analytics-wrong',
    moduleKey: 'analytics',
    name: '错题分析',
    path: '/analytics/wrong-questions',
    icon: 'QuestionFilled',
    sort: 4
  },
  'analytics-intervention': {
    key: 'analytics-intervention',
    moduleKey: 'analytics',
    name: '教学干预决策',
    path: '/analytics/interventions',
    icon: 'Warning',
    sort: 5
  },
  'analytics-ai-usage': {
    key: 'analytics-ai-usage',
    moduleKey: 'analytics',
    name: 'AI 使用分析',
    path: '/analytics/ai-usage',
    icon: 'Coin',
    sort: 6
  },
  'analytics-report': {
    key: 'analytics-report',
    moduleKey: 'analytics',
    name: '教学报告',
    path: '/analytics/teaching-report',
    icon: 'DocumentCopy',
    sort: 7
  },

  // 7. AI 运维 (ai-compute)
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
    name: '教学工具配置',
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
  'ai-audit': {
    key: 'ai-audit',
    moduleKey: 'ai-compute',
    name: 'AI 审计日志',
    path: '/system/audit',
    icon: 'Clock',
    sort: 6
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
  'system-oper-log': {
    key: 'system-oper-log',
    moduleKey: 'system',
    name: '操作日志',
    path: '/system/oper-log',
    icon: 'Memo',
    sort: 8
  },
  'system-security-keys': {
    key: 'system-security-keys',
    moduleKey: 'system',
    name: '国密密钥',
    path: '/system/security/keys',
    icon: 'Key',
    sort: 9
  },
  'system-config': {
    key: 'system-config',
    moduleKey: 'system',
    name: '系统配置',
    path: '/system/config',
    icon: 'Monitor',
    sort: 11
  },

  // 9. 个人中心 (profile)
  'notice-list': {
    key: 'notice-list',
    moduleKey: 'profile',
    name: '消息通知',
    path: '/notice',
    icon: 'Bell',
    sort: 1
  },
  'profile-view': {
    key: 'profile-view',
    moduleKey: 'profile',
    name: '个人资料',
    path: '/profile',
    icon: 'Avatar',
    sort: 2
  },
  'profile-security': {
    key: 'profile-security',
    moduleKey: 'profile',
    name: '账号安全',
    path: '/profile/security',
    icon: 'Lock',
    sort: 3
  },
  'profile-memory': {
    key: 'profile-memory',
    moduleKey: 'profile',
    name: '记忆与隐私',
    path: '/ai/memory',
    icon: 'Key',
    sort: 4
  },
  'profile-ai-usage': {
    key: 'profile-ai-usage',
    moduleKey: 'profile',
    name: 'AI 消耗明细',
    path: '/profile/ai-usage',
    icon: 'CreditCard',
    sort: 5
  },
  'profile-preferences': {
    key: 'profile-preferences',
    moduleKey: 'profile',
    name: '偏好设置',
    path: '/profile/preferences',
    icon: 'Tools',
    sort: 6
  },

  // 系统管理 · 消息广播（入口在系统管理下）
  'notice-broadcast': {
    key: 'notice-broadcast',
    moduleKey: 'system',
    name: '消息广播',
    path: '/system/notification-broadcast',
    icon: 'Promotion',
    sort: 10
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
  if (lower.startsWith('system:operlog') || lower.startsWith('system:oper-log') || lower.startsWith('system:oper')) return 'system-oper-log';
  if (lower.startsWith('system:config')) return 'system-config';

  // AI 运维
  if (lower.startsWith('system:model')) return 'ai-models';
  if (lower.startsWith('system:prompt')) return 'ai-prompts';
  if (lower.startsWith('system:tool')) return 'ai-tools';
  if (lower.startsWith('system:gateway')) return 'ai-gateway';
  if (lower.startsWith('system:quota')) return 'ai-quotas';
  if (lower.startsWith('system:audit')) return 'ai-audit';

  // 课程中心（课件资源挂在课程详情 /course/:id/resources，无独立顶级菜单）
  if (lower.startsWith('course:create')) return 'course-create';
  if (lower.startsWith('course:ai')) return 'course-ai';
  if (lower.startsWith('resource:')) return 'course-list';
  if (lower.startsWith('course:')) return 'course-list';

  // AI 教学
  if (lower.startsWith('ai:chat')) return 'ai-chat';
  if (lower.startsWith('ai:question')) return 'ai-question';
  if (lower.startsWith('ai:exam')) return 'ai-exam';
  if (lower.startsWith('ai:grading')) return 'ai-grading';
  if (lower.startsWith('ai:lesson')) return 'ai-lesson';
  if (lower.startsWith('ai:summary')) return 'ai-summary';
  if (lower.startsWith('ai:recommendation')) return 'ai-recommendation';
  if (lower.startsWith('ai:tool')) return 'ai-agent';

  // 知识库
  if (lower.startsWith('knowledge:rag')) return 'knowledge-rag';
  if (lower.startsWith('knowledge:ocr')) return 'knowledge-ocr';
  if (lower.startsWith('knowledge:chunk')) return 'knowledge-chunks';
  if (lower.startsWith('knowledge:vector')) return 'knowledge-embeddings';
  if (lower.startsWith('knowledge:graph')) return 'knowledge-graph';
  if (lower.startsWith('knowledge:create') || lower.startsWith('knowledge:edit')) return 'knowledge-create';
  if (lower.startsWith('knowledge:')) return 'knowledge-list';

  // 题库与作业
  if (lower.startsWith('question:bank')) return 'question-bank';
  if (lower.startsWith('question:')) return 'question-list';
  if (lower.startsWith('exam:export')) return 'question-export';
  if (lower.startsWith('exam:')) return 'question-exam';
  if (lower.startsWith('assignment:grade')) return 'question-submissions';
  if (lower.startsWith('assignment:')) return 'question-assignment';

  // 学习中心
  if (lower.startsWith('learning:task')) return 'learning-tasks';
  if (lower.startsWith('learning:practice')) return 'learning-practice';
  if (lower.startsWith('learning:wrong')) return 'learning-wrong';
  if (lower.startsWith('learning:report')) return 'learning-report';
  if (lower.startsWith('learning:path')) return 'learning-path';
  if (lower.startsWith('learning:')) return 'learning-dashboard';

  // 教学分析
  if (lower === 'analytics:view' || lower.startsWith('analytics:view')) return 'analytics-overview';
  if (lower.startsWith('analytics:learning')) return 'analytics-learning';
  if (lower.startsWith('analytics:mastery')) return 'analytics-mastery';
  if (lower.startsWith('analytics:wrong')) return 'analytics-wrong';
  if (lower.startsWith('analytics:intervention')) return 'analytics-intervention';
  if (lower.startsWith('analytics:ai-usage')) return 'analytics-ai-usage';
  if (lower.startsWith('analytics:report')) return 'analytics-report';
  if (lower.startsWith('analytics:')) return 'analytics-overview';

  // 国密与安全
  if (lower.startsWith('security:key')) return 'system-security-keys';

  // 个人中心
  if (lower.startsWith('ai:memory')) return 'profile-memory';
  if (lower.startsWith('profile:security')) return 'profile-security';
  if (lower.startsWith('profile:preferences')) return 'profile-preferences';
  if (lower.startsWith('profile:')) return 'profile-view';

  // 消息通知 / 系统广播
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
 * Level 1: 顶级模块（目录，Type 1）- 如「系统管理」、「AI 运维」
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

      const primaryPermission =
        finalLevel3.find((node) => node.permission?.endsWith(':view'))?.permission
        || (finalLevel3.length === 1 ? finalLevel3[0].permission : '')
        || '';

      level2Children.push({
        id: `menu-${subMeta.key}`,
        rowKey: `sub-${subMeta.key}`,
        name: subMeta.name,
        icon: subMeta.icon,
        type: 2, // 页面菜单
        path: subMeta.path,
        permission: primaryPermission,
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

/**
 * 全量系统权限目录（对齐 mock 菜单 + init.sql）
 * 用于权限分配矩阵展示，并在后端数据不完整时自动补全缺失项
 */
export const DEFAULT_SYSTEM_PERMISSIONS: PermissionVO[] = [
  // 系统管理
  { id: 1, permissionCode: 'system:user:view', permissionName: '用户档案查看' },
  { id: 2, permissionCode: 'system:user:edit', permissionName: '用户档案编辑' },
  { id: 95, permissionCode: 'system:user:add', permissionName: '用户档案新增' },
  { id: 96, permissionCode: 'system:user:delete', permissionName: '用户档案删除' },
  { id: 3, permissionCode: 'system:role:view', permissionName: '角色权限查看' },
  { id: 4, permissionCode: 'system:role:edit', permissionName: '角色权限配置' },
  { id: 41, permissionCode: 'system:menu:view', permissionName: '菜单管理查看' },
  { id: 42, permissionCode: 'system:menu:edit', permissionName: '菜单管理编辑' },
  { id: 97, permissionCode: 'system:menu:add', permissionName: '菜单管理新增' },
  { id: 98, permissionCode: 'system:menu:delete', permissionName: '菜单管理删除' },
  { id: 43, permissionCode: 'system:permission:view', permissionName: '权限分配矩阵查看' },
  { id: 44, permissionCode: 'system:tenant:view', permissionName: '租户校区查看' },
  { id: 45, permissionCode: 'system:tenant:edit', permissionName: '租户校区编辑' },
  { id: 46, permissionCode: 'system:org:view', permissionName: '组织架构查看' },
  { id: 32, permissionCode: 'system:organization:list', permissionName: '组织列表' },
  { id: 33, permissionCode: 'system:organization:view', permissionName: '组织详情查看' },
  { id: 34, permissionCode: 'system:organization:create', permissionName: '组织创建' },
  { id: 35, permissionCode: 'system:organization:edit', permissionName: '组织编辑' },
  { id: 36, permissionCode: 'system:organization:update', permissionName: '组织更新' },
  { id: 37, permissionCode: 'system:organization:delete', permissionName: '组织删除' },
  { id: 38, permissionCode: 'system:organization:assign', permissionName: '组织成员分配' },
  { id: 58, permissionCode: 'system:operlog:query', permissionName: '操作日志查询' },
  { id: 59, permissionCode: 'system:operlog:delete', permissionName: '操作日志删除' },
  { id: 60, permissionCode: 'system:operlog:clear', permissionName: '操作日志清空' },
  { id: 61, permissionCode: 'system:operlog:export', permissionName: '操作日志导出' },
  { id: 68, permissionCode: 'security:key:view', permissionName: '国密密钥查看' },
  { id: 69, permissionCode: 'security:key:rotate', permissionName: '国密密钥轮换' },
  { id: 47, permissionCode: 'system:config:view', permissionName: '系统全局配置查看' },
  { id: 48, permissionCode: 'system:config:edit', permissionName: '系统全局配置编辑' },
  { id: 39, permissionCode: 'notice:broadcast:view', permissionName: '广播推送查看' },
  { id: 40, permissionCode: 'notice:broadcast:send', permissionName: '广播推送发送' },

  // AI 运维
  { id: 49, permissionCode: 'system:model:view', permissionName: 'AI 模型接入查看' },
  { id: 50, permissionCode: 'system:model:edit', permissionName: 'AI 模型接入配置' },
  { id: 26, permissionCode: 'system:prompt:view', permissionName: 'Prompt 模板查看' },
  { id: 27, permissionCode: 'system:prompt:edit', permissionName: 'Prompt 模板编辑' },
  { id: 51, permissionCode: 'system:tool:view', permissionName: 'AI 教学工具查看' },
  { id: 70, permissionCode: 'system:tool:edit', permissionName: 'AI 教学工具编辑' },
  { id: 52, permissionCode: 'system:gateway:view', permissionName: 'AI 网关监控查看' },
  { id: 29, permissionCode: 'system:quota:view', permissionName: '算力配额查看' },
  { id: 30, permissionCode: 'system:quota:edit', permissionName: '算力配额编辑' },
  { id: 28, permissionCode: 'system:audit:view', permissionName: 'AI 调用审计查看' },

  // 课程中心
  { id: 5, permissionCode: 'course:view', permissionName: '课程查看' },
  { id: 6, permissionCode: 'course:create', permissionName: '课程创建' },
  { id: 7, permissionCode: 'course:edit', permissionName: '课程编辑' },
  { id: 71, permissionCode: 'course:delete', permissionName: '课程删除' },
  { id: 72, permissionCode: 'course:ai:use', permissionName: '课程 AI 使用' },

  // AI 教学
  { id: 17, permissionCode: 'ai:chat', permissionName: '全局 AI 副驾驶' },
  { id: 19, permissionCode: 'ai:question', permissionName: 'AI 出题' },
  { id: 20, permissionCode: 'ai:exam', permissionName: 'AI 组卷' },
  { id: 18, permissionCode: 'ai:grading', permissionName: 'AI 批改' },
  { id: 73, permissionCode: 'ai:lesson:generate', permissionName: 'AI 教案生成' },
  { id: 74, permissionCode: 'ai:summary:view', permissionName: 'AI 课堂总结查看' },
  { id: 75, permissionCode: 'ai:recommendation:view', permissionName: 'AI 推荐查看' },
  { id: 31, permissionCode: 'ai:tool:use', permissionName: 'Agent 工具调用' },

  // 知识库
  { id: 15, permissionCode: 'knowledge:view', permissionName: '知识库查看' },
  { id: 16, permissionCode: 'knowledge:edit', permissionName: '知识库编辑' },
  { id: 76, permissionCode: 'knowledge:create', permissionName: '知识库创建' },
  { id: 77, permissionCode: 'knowledge:delete', permissionName: '知识库删除' },
  { id: 55, permissionCode: 'knowledge:ocr:use', permissionName: 'OCR 识别使用' },
  { id: 78, permissionCode: 'knowledge:chunk:view', permissionName: '切片管理查看' },
  { id: 79, permissionCode: 'knowledge:vector:view', permissionName: '向量状态查看' },
  { id: 25, permissionCode: 'knowledge:rag:debug', permissionName: 'RAG 检索调试' },
  { id: 80, permissionCode: 'knowledge:graph:view', permissionName: '知识图谱查看' },

  // 题库与作业
  { id: 8, permissionCode: 'question:view', permissionName: '题目查看' },
  { id: 9, permissionCode: 'question:edit', permissionName: '题目编辑' },
  { id: 81, permissionCode: 'question:create', permissionName: '题目创建' },
  { id: 82, permissionCode: 'question:delete', permissionName: '题目删除' },
  { id: 83, permissionCode: 'question:bank:view', permissionName: '题库查看' },
  { id: 10, permissionCode: 'exam:view', permissionName: '试卷查看' },
  { id: 11, permissionCode: 'exam:edit', permissionName: '试卷编辑' },
  { id: 56, permissionCode: 'exam:export', permissionName: '试卷排版导出' },
  { id: 12, permissionCode: 'assignment:view', permissionName: '作业查看' },
  { id: 13, permissionCode: 'assignment:create', permissionName: '作业创建' },
  { id: 14, permissionCode: 'assignment:grade', permissionName: '作业批改' },
  { id: 141, permissionCode: 'assignment:delete', permissionName: '作业删除' },

  // 学习中心
  { id: 84, permissionCode: 'learning:view', permissionName: '学习总览查看' },
  { id: 85, permissionCode: 'learning:task:view', permissionName: '学习任务查看' },
  { id: 86, permissionCode: 'learning:practice', permissionName: 'AI 练习使用' },
  { id: 87, permissionCode: 'learning:wrong:view', permissionName: '错题本查看' },
  { id: 88, permissionCode: 'learning:report:view', permissionName: '学习报告查看' },
  { id: 89, permissionCode: 'learning:path:view', permissionName: '学习路径查看' },

  // 教学分析
  { id: 21, permissionCode: 'analytics:view', permissionName: '课程概览分析' },
  { id: 90, permissionCode: 'analytics:learning', permissionName: '学情分析查看' },
  { id: 91, permissionCode: 'analytics:mastery', permissionName: '知识点掌握分析' },
  { id: 92, permissionCode: 'analytics:wrong', permissionName: '错题分析查看' },
  { id: 66, permissionCode: 'analytics:intervention:view', permissionName: '教学干预查看' },
  { id: 67, permissionCode: 'analytics:intervention:manage', permissionName: '教学干预管理' },
  { id: 93, permissionCode: 'analytics:ai-usage', permissionName: 'AI 使用分析查看' },
  { id: 94, permissionCode: 'analytics:report', permissionName: '教学报告查看' },

  // 教学资源
  { id: 22, permissionCode: 'resource:view', permissionName: '课件资源查看' },
  { id: 23, permissionCode: 'resource:upload', permissionName: '课件资源上传' },

  // 个人中心
  { id: 24, permissionCode: 'notice:view', permissionName: '消息通知查看' },
  { id: 99, permissionCode: 'profile:view', permissionName: '个人资料查看' },
  { id: 100, permissionCode: 'profile:security', permissionName: '账号安全设置' },
  { id: 53, permissionCode: 'ai:memory:view', permissionName: 'Agent 记忆查看' },
  { id: 54, permissionCode: 'ai:memory:manage', permissionName: 'Agent 记忆管理' },
  { id: 101, permissionCode: 'profile:preferences', permissionName: '偏好设置管理' }
];

/** 将后端权限列表与全量目录合并，按 permissionCode 去重（优先保留后端数据） */
export function mergeSystemPermissions(apiList: PermissionVO[]): PermissionVO[] {
  if (!apiList.length) {
    return [...DEFAULT_SYSTEM_PERMISSIONS];
  }

  const existingCodes = new Set(apiList.map((item) => item.permissionCode));
  const merged = [...apiList];

  for (const item of DEFAULT_SYSTEM_PERMISSIONS) {
    if (!existingCodes.has(item.permissionCode)) {
      merged.push({ ...item });
    }
  }

  return merged;
}
