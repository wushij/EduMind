import { RouteRecordRaw } from 'vue-router';

export const systemRoutes: RouteRecordRaw[] = [
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

  // 配额与额度
  {
    path: '/system/quotas',
    name: 'SystemQuota',
    component: () => import('@/views/system/quota/Quota.vue'),
    meta: { title: 'Token 配额管控', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 审计日志
  {
    path: '/system/audit',
    name: 'SystemAuditLog',
    component: () => import('@/views/system/audit/AuditLog.vue'),
    meta: { title: '系统安全与调用审计', requiresAuth: true, roles: ['ADMIN'] }
  },

  // 平台通用配置
  {
    path: '/system/config',
    name: 'SystemConfig',
    component: () => import('@/views/system/config/SystemConfig.vue'),
    meta: { title: '平台全局配置', requiresAuth: true, roles: ['ADMIN'] }
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
  }
];
