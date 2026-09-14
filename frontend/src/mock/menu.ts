/**
 * EduMind 智教云 - 全量核心系统菜单与权限树预设种子数据
 * 100% 覆盖 EduMind 8 大核心业务模块架构，支持本地持久化 CRUD 引擎
 */

import type { SysMenu } from '@/types/system/menu';

export const MENU_STORAGE_KEY = 'edumind_sys_menu_v2_6';

/**
 * 默认预设的 EduMind 8 大核心业务模块菜单树
 */
export const DEFAULT_MENU_TREE: SysMenu[] = [
  // 1. 课程中心
  {
    id: 100,
    parentId: 0,
    name: '课程中心',
    type: 1,
    path: '/course',
    icon: 'Reading',
    sort: 1,
    status: 1,
    visible: true,
    children: [
      {
        id: 101,
        parentId: 100,
        name: '我的课程',
        type: 2,
        path: '/course',
        component: 'views/course/CourseList.vue',
        icon: 'Collection',
        permission: 'course:view',
        sort: 1,
        status: 1,
        visible: true,
        children: [
          {
            id: 1011,
            parentId: 101,
            name: '新增课程',
            type: 3,
            permission: 'course:create',
            sort: 1,
            status: 1
          },
          {
            id: 1012,
            parentId: 101,
            name: '编辑课程',
            type: 3,
            permission: 'course:edit',
            sort: 2,
            status: 1
          },
          {
            id: 1013,
            parentId: 101,
            name: '删除课程',
            type: 3,
            permission: 'course:delete',
            sort: 3,
            status: 1
          }
        ]
      },
      {
        id: 102,
        parentId: 100,
        name: '创建课程',
        type: 2,
        path: '/course/create',
        component: 'views/course/CourseCreate.vue',
        icon: 'DocumentAdd',
        permission: 'course:create',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 103,
        parentId: 100,
        name: '课程 AI 助手',
        type: 2,
        path: '/course/ai-assistant',
        component: 'views/course/detail/CourseAI.vue',
        icon: 'ChatDotRound',
        permission: 'course:ai:use',
        sort: 3,
        status: 1,
        visible: true
      }
    ]
  },

  // 2. AI 教学
  {
    id: 200,
    parentId: 0,
    name: 'AI 教学',
    type: 1,
    path: '/ai',
    icon: 'MagicStick',
    sort: 2,
    status: 1,
    visible: true,
    children: [
      {
        id: 201,
        parentId: 200,
        name: 'AI 教学助手',
        type: 2,
        path: '/ai/assistant/chat',
        component: 'views/ai/AIChat.vue',
        icon: 'Service',
        permission: 'ai:chat',
        sort: 1,
        status: 1,
        visible: true,
        children: [
          {
            id: 2011,
            parentId: 201,
            name: '对话交互',
            type: 3,
            permission: 'ai:chat:interact',
            sort: 1,
            status: 1
          },
          {
            id: 2012,
            parentId: 201,
            name: '会话清空',
            type: 3,
            permission: 'ai:chat:clear',
            sort: 2,
            status: 1
          }
        ]
      },
      {
        id: 202,
        parentId: 200,
        name: 'AI 智能出题',
        type: 2,
        path: '/ai/question/generate',
        component: 'views/ai/AIQuestion.vue',
        icon: 'EditPen',
        permission: 'ai:question',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 203,
        parentId: 200,
        name: 'AI 组卷编排',
        type: 2,
        path: '/ai/exam/generate',
        component: 'views/ai/AIExam.vue',
        icon: 'Tickets',
        permission: 'ai:exam',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 204,
        parentId: 200,
        name: 'AI 智能批改',
        type: 2,
        path: '/ai/grading',
        component: 'views/ai/AIGrading.vue',
        icon: 'CircleCheck',
        permission: 'ai:grading',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 205,
        parentId: 200,
        name: 'AI 教案生成',
        type: 2,
        path: '/ai/lesson',
        component: 'views/ai/AILessonPlan.vue',
        icon: 'Notebook',
        permission: 'ai:lesson:generate',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 206,
        parentId: 200,
        name: 'AI 课堂总结',
        type: 2,
        path: '/ai/summary',
        component: 'views/ai/AISummary.vue',
        icon: 'DocumentCopy',
        permission: 'ai:summary:view',
        sort: 6,
        status: 1,
        visible: true
      },
      {
        id: 207,
        parentId: 200,
        name: 'AI 推荐中心',
        type: 2,
        path: '/ai/recommendation',
        component: 'views/ai/AIRecommendation.vue',
        icon: 'Promotion',
        permission: 'ai:recommendation:view',
        sort: 7,
        status: 1,
        visible: true
      },
      {
        id: 208,
        parentId: 200,
        name: 'Agent 智能体中心',
        type: 2,
        path: '/ai/agent',
        component: 'views/ai/AgentCenter.vue',
        icon: 'Cpu',
        permission: 'ai:tool:use',
        sort: 8,
        status: 1,
        visible: true
      },
      {
        id: 209,
        parentId: 200,
        name: '记忆与隐私',
        type: 2,
        path: '/ai/memory',
        component: 'views/ai/AIMemory.vue',
        icon: 'Key',
        permission: 'ai:memory:view',
        sort: 9,
        status: 1,
        visible: true
      }
    ]
  },

  // 3. 知识库
  {
    id: 300,
    parentId: 0,
    name: '知识库',
    type: 1,
    path: '/knowledge',
    icon: 'FolderOpened',
    sort: 3,
    status: 1,
    visible: true,
    children: [
      {
        id: 301,
        parentId: 300,
        name: '知识库大盘',
        type: 2,
        path: '/knowledge',
        component: 'views/knowledge/KnowledgeList.vue',
        icon: 'Folder',
        permission: 'knowledge:view',
        sort: 1,
        status: 1,
        visible: true,
        children: [
          {
            id: 3011,
            parentId: 301,
            name: '新建知识库',
            type: 3,
            permission: 'knowledge:create',
            sort: 1,
            status: 1
          },
          {
            id: 3012,
            parentId: 301,
            name: '知识库编辑',
            type: 3,
            permission: 'knowledge:edit',
            sort: 2,
            status: 1
          },
          {
            id: 3013,
            parentId: 301,
            name: '删除知识库',
            type: 3,
            permission: 'knowledge:delete',
            sort: 3,
            status: 1
          }
        ]
      },
      {
        id: 302,
        parentId: 300,
        name: '创建知识库',
        type: 2,
        path: '/knowledge/create',
        component: 'views/knowledge/KnowledgeCreate.vue',
        icon: 'FolderAdd',
        permission: 'knowledge:create',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 303,
        parentId: 300,
        name: 'OCR 试卷识别',
        type: 2,
        path: '/knowledge/ocr',
        component: 'views/knowledge/KnowledgeOCR.vue',
        icon: 'Search',
        permission: 'knowledge:ocr:use',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 304,
        parentId: 300,
        name: '文档切片管理',
        type: 2,
        path: '/knowledge/chunks',
        component: 'views/knowledge/detail/Chunks.vue',
        icon: 'Grid',
        permission: 'knowledge:chunk:view',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 305,
        parentId: 300,
        name: '向量状态监控',
        type: 2,
        path: '/knowledge/embeddings',
        component: 'views/knowledge/detail/Embeddings.vue',
        icon: 'PieChart',
        permission: 'knowledge:vector:view',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 306,
        parentId: 300,
        name: 'RAG 检索诊断',
        type: 2,
        path: '/knowledge/rag-debug',
        component: 'views/knowledge/detail/RAGDebug.vue',
        icon: 'Operation',
        permission: 'knowledge:rag:debug',
        sort: 6,
        status: 1,
        visible: true
      },
      {
        id: 307,
        parentId: 300,
        name: '学科知识图谱',
        type: 2,
        path: '/knowledge/graph',
        component: 'views/knowledge/detail/KnowledgeGraph.vue',
        icon: 'Connection',
        permission: 'knowledge:graph:view',
        sort: 7,
        status: 1,
        visible: true
      }
    ]
  },

  // 4. 题库与作业
  {
    id: 400,
    parentId: 0,
    name: '题库与作业',
    type: 1,
    path: '/question',
    icon: 'Document',
    sort: 4,
    status: 1,
    visible: true,
    children: [
      {
        id: 401,
        parentId: 400,
        name: '题目列表',
        type: 2,
        path: '/question/list',
        component: 'views/question/QuestionList.vue',
        icon: 'Memo',
        permission: 'question:view',
        sort: 1,
        status: 1,
        visible: true,
        children: [
          {
            id: 4011,
            parentId: 401,
            name: '创建题目',
            type: 3,
            permission: 'question:create',
            sort: 1,
            status: 1
          },
          {
            id: 4012,
            parentId: 401,
            name: '修改题目',
            type: 3,
            permission: 'question:edit',
            sort: 2,
            status: 1
          },
          {
            id: 4013,
            parentId: 401,
            name: '删除题目',
            type: 3,
            permission: 'question:delete',
            sort: 3,
            status: 1
          }
        ]
      },
      {
        id: 402,
        parentId: 400,
        name: '题库管理',
        type: 2,
        path: '/question/banks',
        component: 'views/question/QuestionBankList.vue',
        icon: 'Collection',
        permission: 'question:bank:view',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 403,
        parentId: 400,
        name: '试卷管理',
        type: 2,
        path: '/question/exams',
        component: 'views/question/ExamList.vue',
        icon: 'DocumentChecked',
        permission: 'exam:view',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 404,
        parentId: 400,
        name: '试卷导出中心',
        type: 2,
        path: '/question/exports',
        component: 'views/question/ExamExportCenter.vue',
        icon: 'DocumentCopy',
        permission: 'exam:export',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 405,
        parentId: 400,
        name: '作业任务',
        type: 2,
        path: '/question/assignments',
        component: 'views/question/AssignmentList.vue',
        icon: 'Notebook',
        permission: 'assignment:view',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 406,
        parentId: 400,
        name: '批改记录',
        type: 2,
        path: '/question/submissions',
        component: 'views/question/SubmissionList.vue',
        icon: 'Finished',
        permission: 'assignment:grade',
        sort: 6,
        status: 1,
        visible: true
      }
    ]
  },

  // 5. 学习中心
  {
    id: 500,
    parentId: 0,
    name: '学习中心',
    type: 1,
    path: '/learning',
    icon: 'TrendCharts',
    sort: 5,
    status: 1,
    visible: true,
    children: [
      {
        id: 501,
        parentId: 500,
        name: '学习总览',
        type: 2,
        path: '/learning',
        component: 'views/learning/LearningDashboard.vue',
        icon: 'DataBoard',
        permission: 'learning:view',
        sort: 1,
        status: 1,
        visible: true
      },
      {
        id: 502,
        parentId: 500,
        name: '学习任务',
        type: 2,
        path: '/learning/tasks',
        component: 'views/learning/LearningTasks.vue',
        icon: 'List',
        permission: 'learning:task:view',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 503,
        parentId: 500,
        name: 'AI 交互练习',
        type: 2,
        path: '/learning/practice',
        component: 'views/learning/PracticeAI.vue',
        icon: 'MagicStick',
        permission: 'learning:practice',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 504,
        parentId: 500,
        name: '个性化错题本',
        type: 2,
        path: '/learning/wrong-questions',
        component: 'views/learning/WrongQuestions.vue',
        icon: 'Warning',
        permission: 'learning:wrong:view',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 505,
        parentId: 500,
        name: '学习分析报告',
        type: 2,
        path: '/learning/report',
        component: 'views/learning/LearningReport.vue',
        icon: 'DataLine',
        permission: 'learning:report:view',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 506,
        parentId: 500,
        name: '自适应学习路径',
        type: 2,
        path: '/learning/path',
        component: 'views/learning/LearningPath.vue',
        icon: 'MapLocation',
        permission: 'learning:path:view',
        sort: 6,
        status: 1,
        visible: true
      }
    ]
  },

  // 6. 教学分析
  {
    id: 600,
    parentId: 0,
    name: '教学分析',
    type: 1,
    path: '/analytics',
    icon: 'DataAnalysis',
    sort: 6,
    status: 1,
    visible: true,
    children: [
      {
        id: 601,
        parentId: 600,
        name: '课程概览分析',
        type: 2,
        path: '/analytics',
        component: 'views/analytics/CourseOverview.vue',
        icon: 'Histogram',
        permission: 'analytics:view',
        sort: 1,
        status: 1,
        visible: true
      },
      {
        id: 602,
        parentId: 600,
        name: '学情整体大盘',
        type: 2,
        path: '/analytics/learning',
        component: 'views/analytics/LearningAnalytics.vue',
        icon: 'TrendCharts',
        permission: 'analytics:learning',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 603,
        parentId: 600,
        name: '知识点掌握分布',
        type: 2,
        path: '/analytics/mastery',
        component: 'views/analytics/KnowledgeMastery.vue',
        icon: 'PieChart',
        permission: 'analytics:mastery',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 604,
        parentId: 600,
        name: '错题成因诊断',
        type: 2,
        path: '/analytics/wrong-questions',
        component: 'views/analytics/WrongQuestionAnalysis.vue',
        icon: 'QuestionFilled',
        permission: 'analytics:wrong',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 605,
        parentId: 600,
        name: '教学干预决策',
        type: 2,
        path: '/analytics/interventions',
        component: 'views/analytics/Interventions.vue',
        icon: 'Warning',
        permission: 'analytics:intervention:view',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 606,
        parentId: 600,
        name: 'AI 消耗与用量分析',
        type: 2,
        path: '/analytics/ai-usage',
        component: 'views/analytics/AIUsage.vue',
        icon: 'Coin',
        permission: 'analytics:ai-usage',
        sort: 6,
        status: 1,
        visible: true
      },
      {
        id: 607,
        parentId: 600,
        name: '周期教学报告',
        type: 2,
        path: '/analytics/teaching-report',
        component: 'views/analytics/TeachingReport.vue',
        icon: 'DocumentCopy',
        permission: 'analytics:report',
        sort: 7,
        status: 1,
        visible: true
      }
    ]
  },

  // 7. AI 智算中心
  {
    id: 700,
    parentId: 0,
    name: 'AI 智算中心',
    type: 1,
    path: '/system/models',
    icon: 'Cpu',
    sort: 7,
    status: 1,
    visible: true,
    children: [
      {
        id: 701,
        parentId: 700,
        name: 'AI 模型接入',
        type: 2,
        path: '/system/models',
        component: 'views/system/ai-model/ModelList.vue',
        icon: 'Cpu',
        permission: 'system:model:view',
        sort: 1,
        status: 1,
        visible: true
      },
      {
        id: 702,
        parentId: 700,
        name: 'Prompt 模板库',
        type: 2,
        path: '/system/prompts',
        component: 'views/system/prompt/PromptList.vue',
        icon: 'ChatLineSquare',
        permission: 'system:prompt:view',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 703,
        parentId: 700,
        name: 'AI 教学工具',
        type: 2,
        path: '/system/tools',
        component: 'views/system/ai-tool/ToolList.vue',
        icon: 'Operation',
        permission: 'system:tool:view',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 704,
        parentId: 700,
        name: 'AI 网关监控',
        type: 2,
        path: '/system/gateway',
        component: 'views/system/gateway/GatewayDashboard.vue',
        icon: 'Connection',
        permission: 'system:gateway:view',
        sort: 4,
        status: 1,
        visible: true
      },
      {
        id: 705,
        parentId: 700,
        name: '算力与配额管控',
        type: 2,
        path: '/system/quotas',
        component: 'views/system/quota/TenantQuota.vue',
        icon: 'Money',
        permission: 'system:quota:view',
        sort: 5,
        status: 1,
        visible: true
      },
      {
        id: 706,
        parentId: 700,
        name: 'AI 审计日志',
        type: 2,
        path: '/system/audit',
        component: 'views/system/audit/AuditLog.vue',
        icon: 'Clock',
        permission: 'system:audit:view',
        sort: 6,
        status: 1,
        visible: true
      }
    ]
  },

  // 8. 系统管理 (精简瘦身后)
  {
    id: 800,
    parentId: 0,
    name: '系统管理',
    type: 1,
    path: '/system/users',
    icon: 'Setting',
    sort: 8,
    status: 1,
    visible: true,
    children: [
      // 1. 用户管理
      {
        id: 801,
        parentId: 800,
        name: '用户管理',
        type: 2,
        path: '/system/users',
        component: 'views/system/users/UserList.vue',
        icon: 'User',
        permission: 'system:user:view',
        sort: 1,
        status: 1,
        visible: true,
        children: [
          {
            id: 8011,
            parentId: 801,
            name: '新增用户',
            type: 3,
            permission: 'system:user:add',
            sort: 1,
            status: 1
          },
          {
            id: 8012,
            parentId: 801,
            name: '编辑用户',
            type: 3,
            permission: 'system:user:edit',
            sort: 2,
            status: 1
          },
          {
            id: 8013,
            parentId: 801,
            name: '删除用户',
            type: 3,
            permission: 'system:user:delete',
            sort: 3,
            status: 1
          }
        ]
      },

      // 2. 角色权限
      {
        id: 802,
        parentId: 800,
        name: '角色权限',
        type: 2,
        path: '/system/roles',
        component: 'views/system/roles/RoleList.vue',
        icon: 'Lock',
        permission: 'system:role:view',
        sort: 2,
        status: 1,
        visible: true,
        children: [
          {
            id: 8021,
            parentId: 802,
            name: '分配权限',
            type: 3,
            permission: 'system:role:edit',
            sort: 1,
            status: 1
          }
        ]
      },

      // 3. 菜单管理
      {
        id: 803,
        parentId: 800,
        name: '菜单管理',
        type: 2,
        path: '/system/menus',
        component: 'views/system/menu/index.vue',
        icon: 'Operation',
        permission: 'system:menu:view',
        sort: 3,
        status: 1,
        visible: true,
        children: [
          {
            id: 8031,
            parentId: 803,
            name: '新增菜单',
            type: 3,
            permission: 'system:menu:add',
            sort: 1,
            status: 1
          },
          {
            id: 8032,
            parentId: 803,
            name: '修改菜单',
            type: 3,
            permission: 'system:menu:edit',
            sort: 2,
            status: 1
          },
          {
            id: 8033,
            parentId: 803,
            name: '删除菜单',
            type: 3,
            permission: 'system:menu:delete',
            sort: 3,
            status: 1
          }
        ]
      },

      // 4. 权限分配矩阵
      {
        id: 804,
        parentId: 800,
        name: '权限分配矩阵',
        type: 2,
        path: '/system/permissions',
        component: 'views/system/roles/Permission.vue',
        icon: 'Key',
        permission: 'system:permission:view',
        sort: 4,
        status: 1,
        visible: true
      },

      // 5. 租户与校区
      {
        id: 805,
        parentId: 800,
        name: '租户与校区',
        type: 2,
        path: '/system/tenants',
        component: 'views/system/tenants/TenantList.vue',
        icon: 'School',
        permission: 'system:tenant:view',
        sort: 5,
        status: 1,
        visible: true
      },

      // 6. 组织架构
      {
        id: 806,
        parentId: 800,
        name: '组织架构',
        type: 2,
        path: '/system/organizations',
        component: 'views/system/organizations/OrgTree.vue',
        icon: 'Connection',
        permission: 'system:org:view',
        sort: 6,
        status: 1,
        visible: true
      },

      // 7. 业务操作日志
      {
        id: 8071,
        parentId: 800,
        name: '操作日志',
        type: 2,
        path: '/system/oper-log',
        component: 'views/system/oper-log/index.vue',
        icon: 'Memo',
        permission: 'system:operlog:query',
        sort: 8,
        status: 1,
        visible: true,
        children: [
          {
            id: 80711,
            parentId: 8071,
            name: '操作日志查询',
            type: 3,
            permission: 'system:operlog:query',
            sort: 1,
            status: 1
          },
          {
            id: 80712,
            parentId: 8071,
            name: '操作日志删除',
            type: 3,
            permission: 'system:operlog:delete',
            sort: 2,
            status: 1
          },
          {
            id: 80713,
            parentId: 8071,
            name: '操作日志清空',
            type: 3,
            permission: 'system:operlog:clear',
            sort: 3,
            status: 1
          },
          {
            id: 80714,
            parentId: 8071,
            name: '操作日志导出',
            type: 3,
            permission: 'system:operlog:export',
            sort: 4,
            status: 1
          }
        ]
      },

      // 8. 系统全局配置 (放最后)
      {
        id: 808,
        parentId: 800,
        name: '消息广播推送',
        type: 2,
        path: '/system/notification-broadcast',
        component: 'views/system/notification/BroadcastList.vue',
        icon: 'Promotion',
        permission: 'notice:broadcast:view',
        sort: 8,
        status: 1,
        visible: true
      },

      // 8.2 国密 KMS 密钥管理 (Gate I9)
      {
        id: 8085,
        parentId: 800,
        name: '国密密钥',
        type: 2,
        path: '/system/security/keys',
        component: 'views/system/security/KeyVersionList.vue',
        icon: 'Key',
        permission: 'security:key:view',
        sort: 8.5,
        status: 1,
        visible: true,
        children: [
          {
            id: 80851,
            parentId: 8085,
            name: '密钥版本查看',
            type: 3,
            permission: 'security:key:view',
            sort: 1,
            status: 1
          },
          {
            id: 80852,
            parentId: 8085,
            name: '密钥版本轮换',
            type: 3,
            permission: 'security:key:rotate',
            sort: 2,
            status: 1
          }
        ]
      },
      {
        id: 809,
        parentId: 800,
        name: '系统全局配置',
        type: 2,
        path: '/system/config',
        component: 'views/system/config/SystemConfig.vue',
        icon: 'Monitor',
        permission: 'system:config:view',
        sort: 9,
        status: 1,
        visible: true
      }
    ]
  },

  // 9. 个人中心（含消息通知，对齐 AppSidebar）
  {
    id: 900,
    parentId: 0,
    name: '个人中心',
    type: 1,
    path: '/profile',
    icon: 'User',
    sort: 9,
    status: 1,
    visible: true,
    children: [
      {
        id: 901,
        parentId: 900,
        name: '消息通知',
        type: 2,
        path: '/notice',
        component: 'views/notice/NotificationCenter.vue',
        icon: 'Bell',
        permission: 'notice:view',
        sort: 1,
        status: 1,
        visible: true
      },
      {
        id: 902,
        parentId: 900,
        name: '个人资料',
        type: 2,
        path: '/profile',
        component: 'views/profile/Profile.vue',
        icon: 'Avatar',
        permission: 'profile:view',
        sort: 2,
        status: 1,
        visible: true
      },
      {
        id: 903,
        parentId: 900,
        name: '账号安全',
        type: 2,
        path: '/profile/security',
        component: 'views/profile/Security.vue',
        icon: 'Lock',
        permission: 'profile:security',
        sort: 3,
        status: 1,
        visible: true
      },
      {
        id: 904,
        parentId: 900,
        name: '偏好设置',
        type: 2,
        path: '/profile/preferences',
        component: 'views/profile/Preferences.vue',
        icon: 'Tools',
        permission: 'profile:preferences',
        sort: 4,
        status: 1,
        visible: true
      }
    ]
  }
];

function hasMenuPath(nodes: SysMenu[], path: string): boolean {
  return nodes.some((n) => n.path === path || (n.children && hasMenuPath(n.children, path)));
}

function removeMenuByPath(nodes: SysMenu[], path: string): boolean {
  const idx = nodes.findIndex((n) => n.path === path);
  if (idx !== -1) {
    nodes.splice(idx, 1);
    return true;
  }
  for (const node of nodes) {
    if (node.children && removeMenuByPath(node.children, path)) {
      return true;
    }
  }
  return false;
}

/**
 * 将「AI 审计日志」从系统管理迁移至 AI 智算中心，与侧边栏架构保持一致
 */
function migrateAuditMenuToAiCompute(tree: SysMenu[]): boolean {
  const aiComputeMod = tree.find((m) => m.id === 700);
  if (!aiComputeMod || !aiComputeMod.children) {
    return false;
  }

  const alreadyInAiCompute = aiComputeMod.children.some((c) => c.path === '/system/audit');
  const auditInSystem = hasMenuPath(tree.find((m) => m.id === 800)?.children || [], '/system/audit');

  if (alreadyInAiCompute && !auditInSystem) {
    return false;
  }

  let changed = false;

  if (auditInSystem) {
    changed = removeMenuByPath(tree.find((m) => m.id === 800)?.children || [], '/system/audit') || changed;
  }

  if (!alreadyInAiCompute) {
    const defaultAuditNode = DEFAULT_MENU_TREE
      .find((m) => m.id === 700)
      ?.children?.find((c) => c.path === '/system/audit');
    if (defaultAuditNode) {
      aiComputeMod.children.push(JSON.parse(JSON.stringify(defaultAuditNode)));
      aiComputeMod.children.sort((a, b) => (a.sort || 0) - (b.sort || 0));
      changed = true;
    }
  }

  return changed;
}

/**
 * 获取本地存储的菜单列表
 */
export function getStoredMenuTree(): SysMenu[] {
  try {
    const raw = localStorage.getItem(MENU_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) {
        if (migrateAuditMenuToAiCompute(parsed)) {
          saveStoredMenuTree(parsed);
        }
        // 自动校验并注入「操作日志」菜单节点，防止历史 localStorage 遗漏
        const hasOperLog = (nodes: SysMenu[]): boolean => {
          return nodes.some(n => n.path === '/system/oper-log' || (n.children && hasOperLog(n.children)));
        };
        if (!hasOperLog(parsed)) {
          const sysMod = parsed.find(m => m.id === 800);
          if (sysMod && sysMod.children) {
            const defaultSysMod = DEFAULT_MENU_TREE.find(m => m.id === 800);
            const operLogNode = defaultSysMod?.children?.find(c => c.id === 8071);
            if (operLogNode) {
              sysMod.children.splice(sysMod.children.length - 1, 0, JSON.parse(JSON.stringify(operLogNode)));
              saveStoredMenuTree(parsed);
            }
          }
        }
        // 自动校验并注入「国密密钥」菜单节点，防止历史 localStorage 遗漏
        const hasSecurityKeys = (nodes: SysMenu[]): boolean => {
          return nodes.some(n => n.path === '/system/security/keys' || (n.children && hasSecurityKeys(n.children)));
        };
        if (!hasSecurityKeys(parsed)) {
          const sysMod = parsed.find(m => m.id === 800);
          if (sysMod && sysMod.children) {
            const defaultSysMod = DEFAULT_MENU_TREE.find(m => m.id === 800);
            const secKeysNode = defaultSysMod?.children?.find(c => c.id === 8085);
            if (secKeysNode) {
              sysMod.children.splice(sysMod.children.length - 1, 0, JSON.parse(JSON.stringify(secKeysNode)));
              saveStoredMenuTree(parsed);
            }
          }
        }
        return parsed;
      }
    }
  } catch {
    // ignore parsing errors
  }
  // 首次默认写入
  saveStoredMenuTree(DEFAULT_MENU_TREE);
  return JSON.parse(JSON.stringify(DEFAULT_MENU_TREE));
}

/**
 * 持久化菜单列表到本地存储
 */
export function saveStoredMenuTree(tree: SysMenu[]): void {
  try {
    localStorage.setItem(MENU_STORAGE_KEY, JSON.stringify(tree));
  } catch (err) {
    console.error('Failed to persist menu tree in localStorage:', err);
  }
}

/**
 * 递归查找最大 ID
 */
function findMaxId(nodes: SysMenu[]): number {
  let max = 0;
  for (const item of nodes) {
    if (item.id > max) max = item.id;
    if (item.children && item.children.length > 0) {
      const childMax = findMaxId(item.children);
      if (childMax > max) max = childMax;
    }
  }
  return max;
}

/**
 * 本地内存模拟：创建菜单
 */
export function mockCreateMenu(payload: Partial<SysMenu>): SysMenu {
  const tree = getStoredMenuTree();
  const nextId = Math.max(findMaxId(tree), 1000) + 1;
  const newMenu: SysMenu = {
    id: nextId,
    parentId: Number(payload.parentId) || 0,
    name: payload.name || '',
    type: payload.type || 2,
    path: payload.path || '',
    component: payload.component || '',
    icon: payload.icon || '',
    permission: payload.permission || '',
    sort: Number(payload.sort) || 10,
    status: (payload.status ?? 1) as 1 | 0,
    visible: payload.visible !== false,
    keepAlive: Boolean(payload.keepAlive),
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    children: []
  };

  if (newMenu.parentId === 0) {
    tree.push(newMenu);
  } else {
    const insertIntoParent = (nodes: SysMenu[]): boolean => {
      for (const node of nodes) {
        if (node.id === newMenu.parentId) {
          if (!node.children) node.children = [];
          node.children.push(newMenu);
          return true;
        }
        if (node.children && insertIntoParent(node.children)) {
          return true;
        }
      }
      return false;
    };
    if (!insertIntoParent(tree)) {
      // 找不到父级则退化为顶级节点
      tree.push(newMenu);
    }
  }

  saveStoredMenuTree(tree);
  return newMenu;
}

/**
 * 本地内存模拟：更新菜单
 */
export function mockUpdateMenu(id: number, payload: Partial<SysMenu>): boolean {
  const tree = getStoredMenuTree();

  const updateNode = (nodes: SysMenu[]): boolean => {
    for (const node of nodes) {
      if (node.id === id) {
        Object.assign(node, payload, {
          id, // 确保 ID 不被覆盖
          updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
        });
        return true;
      }
      if (node.children && updateNode(node.children)) {
        return true;
      }
    }
    return false;
  };

  const ok = updateNode(tree);
  if (ok) {
    saveStoredMenuTree(tree);
  }
  return ok;
}

/**
 * 本地内存模拟：删除菜单
 */
export function mockDeleteMenu(id: number): boolean {
  const tree = getStoredMenuTree();

  const removeNode = (nodes: SysMenu[]): boolean => {
    const idx = nodes.findIndex((n) => n.id === id);
    if (idx !== -1) {
      nodes.splice(idx, 1);
      return true;
    }
    for (const node of nodes) {
      if (node.children && removeNode(node.children)) {
        return true;
      }
    }
    return false;
  };

  const ok = removeNode(tree);
  if (ok) {
    saveStoredMenuTree(tree);
  }
  return ok;
}

/**
 * 重置为初始默认值
 */
export function mockResetDefaultMenus(): SysMenu[] {
  const cloned = JSON.parse(JSON.stringify(DEFAULT_MENU_TREE));
  saveStoredMenuTree(cloned);
  return cloned;
}
