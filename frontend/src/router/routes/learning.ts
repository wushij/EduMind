import { RouteRecordRaw } from 'vue-router';

export const learningRoutes: RouteRecordRaw[] = [
  {
    path: '/learning',
    name: 'LearningHome',
    component: () => import('@/views/learning/LearningHome.vue'),
    meta: { title: '学习中心', requiresAuth: true }
  },
  {
    path: '/learning/tasks',
    name: 'LearningTasks',
    component: () => import('@/views/learning/Tasks.vue'),
    meta: { title: '待办学习任务', requiresAuth: true }
  },
  {
    path: '/learning/assignments/:id/take',
    name: 'AssignmentTake',
    component: () => import('@/views/learning/AssignmentTake.vue'),
    meta: { title: '作业作答', requiresAuth: true }
  },
  {
    path: '/learning/assignments/:id/result',
    name: 'AssignmentResult',
    component: () => import('@/views/learning/AssignmentResult.vue'),
    meta: { title: '作业结果', requiresAuth: true }
  },
  {
    path: '/learning/practice',
    name: 'LearningPractice',
    component: () => import('@/views/learning/AIPractice.vue'),
    meta: { title: 'AI 自适应练习', requiresAuth: true }
  },
  {
    path: '/learning/wrong-questions',
    name: 'LearningWrongQuestions',
    component: () => import('@/views/learning/WrongQuestions.vue'),
    meta: { title: '个人错题本', requiresAuth: true }
  },
  {
    path: '/learning/recommendations',
    name: 'LearningRecommendations',
    component: () => import('@/views/learning/Recommendations.vue'),
    meta: { title: '个性化资源推荐', requiresAuth: true }
  },
  {
    path: '/learning/report',
    name: 'LearningReport',
    component: () => import('@/views/learning/LearningReport.vue'),
    meta: { title: '学情诊断分析报告', requiresAuth: true }
  },
  {
    path: '/learning/path',
    name: 'LearningPath',
    component: () => import('@/views/learning/LearningPath.vue'),
    meta: { title: '知识图谱学习路径', requiresAuth: true }
  }
];
