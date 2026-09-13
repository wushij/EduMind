import { RouteRecordRaw } from 'vue-router';

export const analyticsRoutes: RouteRecordRaw[] = [
  {
    path: '/analytics',
    name: 'AnalyticsOverview',
    component: () => import('@/views/analytics/Overview.vue'),
    meta: { title: '学情数据总览', requiresAuth: true }
  },
  {
    path: '/analytics/learning',
    name: 'AnalyticsLearning',
    component: () => import('@/views/analytics/LearningAnalysis.vue'),
    meta: { title: '学生学情诊断', requiresAuth: true }
  },
  {
    path: '/analytics/mastery',
    name: 'AnalyticsMastery',
    component: () => import('@/views/analytics/KnowledgeMastery.vue'),
    meta: { title: '知识点掌握度画像', requiresAuth: true }
  },
  {
    path: '/analytics/wrong-questions',
    name: 'AnalyticsWrongQuestions',
    component: () => import('@/views/analytics/WrongQuestionAnalysis.vue'),
    meta: { title: '错题归因分析', requiresAuth: true }
  },
  {
    path: '/analytics/ai-usage',
    name: 'AnalyticsAIUsage',
    component: () => import('@/views/analytics/AIUsageAnalysis.vue'),
    meta: { title: 'AI 消耗与调用分析', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/analytics/teaching-report',
    name: 'AnalyticsTeachingReport',
    component: () => import('@/views/analytics/TeachingReport.vue'),
    meta: { title: '教学质量分析报告', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/analytics/interventions',
    name: 'AnalyticsTeachingIntervention',
    component: () => import('@/views/analytics/interventions/InterventionCenter.vue'),
    meta: { title: '教学干预决策工作台', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  }
];
