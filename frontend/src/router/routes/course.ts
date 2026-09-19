import { RouteRecordRaw } from 'vue-router';

export const courseRoutes: RouteRecordRaw[] = [
  {
    path: '/course',
    name: 'CourseList',
    component: () => import('@/views/course/CourseList.vue'),
    meta: { title: '课程中心', requiresAuth: true }
  },
  {
    path: '/course/create',
    name: 'CourseCreate',
    component: () => import('@/views/course/CourseCreate.vue'),
    meta: { title: '新建课程', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/course/ai',
    name: 'CourseAIAssistant',
    component: () => import('@/views/course/CourseAIPage.vue'),
    meta: { title: '课程 AI 助手', requiresAuth: true, fullHeight: true }
  },
  {
    path: '/course/ai-assistant',
    name: 'CourseAIAssistantRedirect',
    redirect: '/course/ai',
    meta: { title: '课程 AI 助手', requiresAuth: true }
  },
  {
    path: '/course/:id',
    name: 'CourseDetail',
    component: () => import('@/views/course/CourseDetail.vue'),
    redirect: to => `/course/${to.params.id}/overview`,
    meta: { title: '课程空间', requiresAuth: true },
    children: [
      {
        path: 'overview',
        name: 'CourseOverview',
        component: () => import('@/views/course/detail/Overview.vue'),
        meta: { title: '课程概览', requiresAuth: true }
      },
      {
        path: 'chapters',
        name: 'CourseChapters',
        component: () => import('@/views/course/detail/Chapters.vue'),
        meta: { title: '教学大纲', requiresAuth: true }
      },
      {
        path: 'learn/:lessonId',
        name: 'CourseLessonLearn',
        component: () => import('@/views/course/learn/LessonLearn.vue'),
        meta: { title: '课节学习', requiresAuth: true }
      },
      {
        path: 'lessons/:lessonId/edit',
        name: 'CourseLessonEdit',
        component: () => import('@/views/course/lesson/LessonEdit.vue'),
        meta: {
          title: '编辑课节',
          requiresAuth: true,
          roles: ['ADMIN', 'TEACHER'],
          immersiveLessonStudio: true
        }
      },
      {
        path: 'knowledge-points',
        name: 'CourseKnowledgePoints',
        component: () => import('@/views/course/detail/KnowledgePoints.vue'),
        meta: { title: '知识点体系', requiresAuth: true }
      },
      {
        path: 'resources',
        name: 'CourseResources',
        component: () => import('@/views/course/detail/Resources.vue'),
        meta: { title: '教学资料', requiresAuth: true }
      },
      {
        path: 'ai',
        name: 'CourseAI',
        component: () => import('@/views/course/detail/CourseAI.vue'),
        meta: { title: '课程 AI 助手', requiresAuth: true, fullHeight: true }
      },
      {
        path: 'members',
        name: 'CourseMembers',
        component: () => import('@/views/course/detail/Members.vue'),
        meta: { title: '课程成员', requiresAuth: true }
      }
    ]
  }
];
