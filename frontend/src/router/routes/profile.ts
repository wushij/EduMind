import { RouteRecordRaw } from 'vue-router';

export const profileRoutes: RouteRecordRaw[] = [
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { title: '个人信息', requiresAuth: true }
  },
  {
    path: '/profile/security',
    name: 'ProfileSecurity',
    component: () => import('@/views/profile/Security.vue'),
    meta: { title: '账号与安全设置', requiresAuth: true }
  },
  {
    path: '/profile/ai-usage',
    name: 'ProfileAIUsage',
    component: () => import('@/views/profile/AIUsage.vue'),
    meta: { title: '个人 AI 消耗明细', requiresAuth: true }
  },
  {
    path: '/profile/preferences',
    name: 'ProfilePreferences',
    component: () => import('@/views/profile/Preferences.vue'),
    meta: { title: '偏好与界面设置', requiresAuth: true }
  }
];
