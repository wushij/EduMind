import { RouteRecordRaw } from 'vue-router';

export const noticeRoutes: RouteRecordRaw[] = [
  {
    path: '/notice',
    name: 'NotificationCenter',
    component: () => import('@/views/notice/NotificationCenter.vue'),
    meta: { title: '消息通知', requiresAuth: true, permissions: ['notice:view'] }
  }
];
