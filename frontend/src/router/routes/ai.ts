import { RouteRecordRaw } from 'vue-router';

export const aiRoutes: RouteRecordRaw[] = [
  // AI 工具广场分类
  {
    path: '/ai/marketplace',
    name: 'AIMarketplace',
    component: () => import('@/views/ai/marketplace/AllTools.vue'),
    meta: { title: 'AI 工具广场', requiresAuth: true }
  },
  {
    path: '/ai/marketplace/teacher',
    name: 'AIMarketplaceTeacher',
    component: () => import('@/views/ai/marketplace/TeacherTools.vue'),
    meta: { title: '教师专属 AI 工具', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/marketplace/student',
    name: 'AIMarketplaceStudent',
    component: () => import('@/views/ai/marketplace/StudentTools.vue'),
    meta: { title: '学生专属 AI 工具', requiresAuth: true, roles: ['ADMIN', 'STUDENT'] }
  },
  {
    path: '/ai/marketplace/recommended',
    name: 'AIMarketplaceRecommended',
    component: () => import('@/views/ai/marketplace/RecommendedTools.vue'),
    meta: { title: 'AI 推荐工具', requiresAuth: true }
  },
  {
    path: '/ai/marketplace/my-tools',
    name: 'AIMarketplaceMyTools',
    component: () => import('@/views/ai/marketplace/MyTools.vue'),
    meta: { title: '我的收藏工具', requiresAuth: true }
  },
  {
    path: '/ai/marketplace/v05/:toolId',
    name: 'AIMarketplaceV05Notice',
    component: () => import('@/views/ai/marketplace/V05ToolNotice.vue'),
    meta: { title: 'V0.5 工具预告', requiresAuth: true }
  },

  // AI 智能出题
  {
    path: '/ai/question/generate',
    name: 'AIQuestionGenerate',
    component: () => import('@/views/ai/question/Generate.vue'),
    meta: { title: 'AI 智能出题', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/question/preview',
    name: 'AIQuestionPreview',
    component: () => import('@/views/ai/question/Preview.vue'),
    meta: { title: 'AI 出题预览与入库', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // AI 智能组卷
  {
    path: '/ai/exam/generate',
    name: 'AIExamGenerate',
    component: () => import('@/views/ai/exam/Generate.vue'),
    meta: { title: 'AI 智能组卷', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/exam/preview',
    name: 'AIExamPreview',
    component: () => import('@/views/ai/exam/Preview.vue'),
    meta: { title: 'AI 试卷预览与调整', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // AI 智能批改
  {
    path: '/ai/grading',
    name: 'AIGrading',
    component: () => import('@/views/ai/grading/Grading.vue'),
    meta: { title: 'AI 智能批改', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/grading/result',
    name: 'AIGradingResult',
    component: () => import('@/views/ai/grading/Result.vue'),
    meta: { title: '批改结果与复核', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // AI 课堂总结与推荐
  {
    path: '/ai/summary',
    name: 'AISummary',
    component: () => import('@/views/ai/summary/Summary.vue'),
    meta: { title: 'AI 课堂总结', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/recommendation',
    name: 'AIRecommendation',
    component: () => import('@/views/ai/recommendation/Recommendation.vue'),
    meta: { title: 'AI 智能推荐', requiresAuth: true }
  },

  // 旧版独立 AI 教案页已下线，AI 备课统一收敛到课程内课节的教案工作台，兼容历史链接
  {
    path: '/ai/lesson',
    redirect: '/course/ai'
  },
  {
    path: '/ai/lesson-plan',
    redirect: '/course/ai'
  },

  // 旧版独立 AI 助手页已下线，兼容历史链接重定向至课程 AI 工作台
  {
    path: '/ai/assistant/chat',
    redirect: '/course/ai'
  },
  {
    path: '/ai/assistant/history',
    redirect: '/course/ai'
  },

  // AI Agent 中心
  {
    path: '/ai/agent',
    name: 'AIAgentCenter',
    component: () => import('@/views/ai/agent/AgentCenter.vue'),
    meta: { title: 'AI Agent 中心', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/ai/agent/workflow',
    name: 'AIAgentWorkflow',
    component: () => import('@/views/ai/agent/AgentWorkflow.vue'),
    meta: {
      title: 'Agent 工作流',
      requiresAuth: true,
      roles: ['ADMIN', 'TEACHER'],
      activeMenu: '/ai/agent'
    }
  },

  // Agent 长期记忆与隐私治理 (V2.0)
  {
    path: '/ai/memory',
    name: 'AIAgentMemory',
    component: () => import('@/views/ai/memory/MemoryList.vue'),
    meta: { title: 'Agent 长期记忆与隐私', requiresAuth: true }
  }
];
