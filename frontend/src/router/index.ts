import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AppLayout from '@/layouts/AppLayout.vue';
import { staticRoutes } from './routes/static';
import { dashboardRoutes } from './routes/dashboard';
import { aiRoutes } from './routes/ai';
import { courseRoutes } from './routes/course';
import { knowledgeRoutes } from './routes/knowledge';
import { questionRoutes } from './routes/question';
import { learningRoutes } from './routes/learning';
import { analyticsRoutes } from './routes/analytics';
import { systemRoutes } from './routes/system';
import { profileRoutes } from './routes/profile';
import { noticeRoutes } from './routes/notice';
import { setupRouterGuards } from './guards';

const mainRoutes: RouteRecordRaw = {
  path: '/',
  component: AppLayout,
  redirect: '/dashboard',
  children: [
    ...dashboardRoutes,
    ...aiRoutes,
    ...courseRoutes,
    ...knowledgeRoutes,
    ...questionRoutes,
    ...learningRoutes,
    ...analyticsRoutes,
    ...systemRoutes,
    ...profileRoutes,
    ...noticeRoutes,
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/auth/NotFound.vue'),
      meta: { title: '404 - 页面未找到', requiresAuth: true }
    }
  ]
};

export const router = createRouter({
  history: createWebHistory(),
  routes: [mainRoutes, ...staticRoutes]
});

setupRouterGuards(router);
