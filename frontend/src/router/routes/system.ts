import { RouteRecordRaw } from 'vue-router';

export const systemRoutes: RouteRecordRaw[] = [
  // 多租户与校区架构 (V2.0)
  {
    path: '/system/tenants',
    name: 'SystemTenantList',
    component: () => import('@/views/system/tenants/TenantList.vue'),
    meta: { title: '租户与校区管理', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/organizations',
    name: 'SystemOrgTree',
    component: () => import('@/views/system/organizations/OrgTree.vue'),
    meta: { title: '组织架构与班级', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 用户管理
  {
    path: '/system/users',
    name: 'SystemUserList',
    component: () => import('@/views/system/users/UserList.vue'),
    meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/users/:id',
    name: 'SystemUserDetail',
    component: () => import('@/views/system/users/UserDetail.vue'),
    meta: { title: '用户详情', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 角色与权限
  {
    path: '/system/roles',
    name: 'SystemRoleList',
    component: () => import('@/views/system/roles/RoleList.vue'),
    meta: { title: '角色权限管理', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/menus',
    name: 'SystemMenuList',
    component: () => import('@/views/system/menu/index.vue'),
    meta: { title: '菜单管理', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/permissions',
    name: 'SystemPermission',
    component: () => import('@/views/system/roles/Permission.vue'),
    meta: { title: '权限分配矩阵', requiresAuth: true, roles: ['ADMIN'] }
  },

  // AI 模型接入
  {
    path: '/system/models',
    name: 'SystemModelList',
    component: () => import('@/views/system/ai-model/ModelList.vue'),
    meta: { title: 'AI 模型调度配置', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/models/edit/:id?',
    name: 'SystemModelEdit',
    component: () => import('@/views/system/ai-model/ModelEdit.vue'),
    meta: { title: '模型参数编辑', requiresAuth: true, roles: ['ADMIN'] }
  },

  // Prompt 模板库
  {
    path: '/system/prompts',
    name: 'SystemPromptList',
    component: () => import('@/views/system/prompt/PromptList.vue'),
    meta: { title: 'Prompt 提示词模板', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/system/prompts/editor/:id?',
    name: 'SystemPromptEditor',
    component: () => import('@/views/system/prompt/PromptEditor.vue'),
    meta: { title: 'Prompt 编辑器', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // AI 工具管理
  {
    path: '/system/tools',
    name: 'SystemToolList',
    component: () => import('@/views/system/ai-tool/ToolList.vue'),
    meta: { title: 'AI 教学工具维护', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/tools/edit/:id?',
    name: 'SystemToolEdit',
    component: () => import('@/views/system/ai-tool/ToolEdit.vue'),
    meta: { title: '编辑 AI 工具', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 配额与额度 (V2.0 租户用量与配额大盘)
  {
    path: '/system/quotas',
    name: 'SystemQuota',
    component: () => import('@/views/system/quota/TenantQuota.vue'),
    meta: { title: '租户用量与配额大盘', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 审计日志 (AI 算力与网关调用)
  {
    path: '/system/audit',
    name: 'SystemAuditLog',
    component: () => import('@/views/system/audit/AuditLog.vue'),
    meta: { title: '系统安全与调用审计', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 业务操作日志 (对齐 wu-admin)
  {
    path: '/system/oper-log',
    name: 'SystemOperLog',
    component: () => import('@/views/system/oper-log/index.vue'),
    meta: { title: '业务操作日志', requiresAuth: true, roles: ['ADMIN'] }
  },

  // AI 网关
  {
    path: '/system/gateway',
    name: 'SystemGatewayDashboard',
    component: () => import('@/views/system/gateway/GatewayDashboard.vue'),
    meta: { title: 'AI 网关监控', requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system/gateway/routes',
    name: 'SystemGatewayRoutes',
    component: () => import('@/views/system/gateway/RouteRules.vue'),
    meta: {
      title: '网关路由规则',
      requiresAuth: true,
      roles: ['ADMIN'],
      activeMenu: '/system/gateway'
    }
  },

  {
    path: '/system/notification-broadcast',
    name: 'NotificationBroadcast',
    component: () => import('@/views/system/notification/BroadcastList.vue'),
    meta: {
      title: '消息广播推送',
      requiresAuth: true,
      permissions: ['notice:broadcast:view']
    }
  },

  // 国密 KMS 密钥版本管理 (Gate I9)
  {
    path: '/system/security/keys',
    name: 'SystemSecurityKeys',
    component: () => import('@/views/system/security/KeyVersionList.vue'),
    meta: {
      title: '国密 KMS 密钥管理',
      requiresAuth: true,
      roles: ['ADMIN'],
      permissions: ['security:key:view']
    }
  },

  // 平台全局配置 (放最后)
  {
    path: '/system/config',
    name: 'SystemConfig',
    component: () => import('@/views/system/config/SystemConfig.vue'),
    meta: { title: '平台全局配置', requiresAuth: true, roles: ['ADMIN'] }
  }
];
