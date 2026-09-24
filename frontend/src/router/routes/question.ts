import { RouteRecordRaw } from 'vue-router';

export const questionRoutes: RouteRecordRaw[] = [
  // 试题管理
  {
    path: '/question',
    redirect: '/question/list',
    meta: { title: '题库与作业', requiresAuth: true }
  },
  {
    path: '/question/list',
    name: 'QuestionList',
    component: () => import('@/views/question/questions/QuestionList.vue'),
    meta: { title: '试题库管理', requiresAuth: true }
  },
  {
    path: '/question/create',
    name: 'QuestionCreate',
    component: () => import('@/views/question/questions/QuestionCreate.vue'),
    meta: { title: '录入新题目', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/edit/:id',
    name: 'QuestionEdit',
    component: () => import('@/views/question/questions/QuestionEdit.vue'),
    meta: { title: '编辑试题', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // 题库分类
  {
    path: '/question/banks',
    name: 'QuestionBankList',
    component: () => import('@/views/question/banks/BankList.vue'),
    meta: { title: '公共与课程题库', requiresAuth: true }
  },
  {
    path: '/question/banks/:id',
    name: 'QuestionBankDetail',
    component: () => import('@/views/question/banks/BankDetail.vue'),
    meta: { title: '题库详情', requiresAuth: true }
  },

  // 试卷管理
  {
    path: '/question/exams',
    name: 'ExamList',
    component: () => import('@/views/question/exams/ExamList.vue'),
    meta: { title: '试卷管理', requiresAuth: true }
  },
  {
    path: '/question/exams/create',
    name: 'ExamCreate',
    component: () => import('@/views/question/exams/ExamCreate.vue'),
    meta: { title: '新建试卷', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/exams/:id',
    name: 'ExamDetail',
    component: () => import('@/views/question/exams/ExamDetail.vue'),
    meta: { title: '试卷详情与打印', requiresAuth: true }
  },
  {
    path: '/question/exports',
    name: 'QuestionExportCenter',
    component: () => import('@/views/question/export/ExportCenter.vue'),
    meta: { title: '试卷导出中心', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // 作业管理（教师/管理员专属：页面承载全班提交进度、批改与删除等教学管理能力；
  // 学生侧作业入口是 /learning/tasks 与 /learning/assignments/:id/take）
  {
    path: '/question/assignments',
    name: 'AssignmentList',
    component: () => import('@/views/question/assignments/AssignmentList.vue'),
    meta: { title: '平时作业', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/assignments/create',
    name: 'AssignmentCreate',
    component: () => import('@/views/question/assignments/AssignmentCreate.vue'),
    meta: { title: '布置作业', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/assignments/:id',
    name: 'AssignmentDetail',
    component: () => import('@/views/question/assignments/AssignmentDetail.vue'),
    meta: { title: '作业详情', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },

  // 作业批阅与提交（全班答卷含成绩，属教师/管理员批改场景，后端接口同样要求 assignment:grade）
  {
    path: '/question/submissions',
    name: 'SubmissionList',
    component: () => import('@/views/question/submissions/SubmissionList.vue'),
    meta: { title: '提交记录与批阅', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/submissions/:id',
    name: 'SubmissionDetail',
    component: () => import('@/views/question/submissions/SubmissionDetail.vue'),
    meta: { title: '作答批阅详情', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  }
];
