import { RouteRecordRaw } from 'vue-router';

export const dashboardRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/Dashboard.vue'),
    meta: { title: '工作台首页', requiresAuth: true }
  },
  {
    path: '/dashboard/overview',
    name: 'DashboardOverview',
    component: () => import('@/views/dashboard/Overview.vue'),
    meta: { title: '概览看板', requiresAuth: true }
  },
  {
    path: '/dashboard/todo',
    name: 'DashboardTodo',
    component: () => import('@/views/dashboard/Todo.vue'),
    meta: { title: '我的待办', requiresAuth: true }
  },
  {
    path: '/dashboard/recent',
    name: 'DashboardRecent',
    component: () => import('@/views/dashboard/Recent.vue'),
    meta: { title: '近期动态', requiresAuth: true }
  }
];
