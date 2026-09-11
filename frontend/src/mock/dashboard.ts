import type {
  DashboardKpiItem,
  DashboardShortcut,
  RecentActivityItem,
  DashboardData
} from '@/types/dashboard/dashboard';

export type { DashboardKpiItem, DashboardShortcut, RecentActivityItem, DashboardData };

export const MOCK_DASHBOARD_DATA: DashboardData = {
  kpiStats: {
    ADMIN: [
      {
        id: 'courses',
        label: '活跃开课总数',
        value: '36 门',
        trend: '+4 门 本周新增',
        trendUp: true,
        icon: 'Collection',
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
        sublabel: '全校理工/计算机/社科类'
      },
      {
        id: 'users',
        label: '在籍师生规模',
        value: '3,420 人',
        trend: '+12.5% 月环比',
        trendUp: true,
        icon: 'User',
        gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
        sublabel: '教师 86 人 / 学生 3,334 人'
      },
      {
        id: 'ai_tokens',
        label: 'AI 交互推导总量',
        value: '184.2 万',
        trend: '+38% 本月增长',
        trendUp: true,
        icon: 'Cpu',
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
        sublabel: 'RAG 检索调用 2.8 万次'
      },
      {
        id: 'knowledge',
        label: '挂载知识库文档',
        value: '1,280 份',
        trend: '99.4% 向量解析率',
        trendUp: true,
        icon: 'FolderOpened',
        gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
        sublabel: '已抽取核心知识点 4,520 个'
      }
    ],
    TEACHER: [
      {
        id: 'my_courses',
        label: '主讲教学课程',
        value: '3 门',
        trend: '在读 286 人',
        trendUp: true,
        icon: 'Reading',
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
        sublabel: '本周课时进度正常'
      },
      {
        id: 'ai_answers',
        label: '课程 AI 助教答疑',
        value: '1,248 次',
        trend: '98.6% 解惑满意度',
        trendUp: true,
        icon: 'Service',
        gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
        sublabel: '分担 76% 重复概念咨询'
      },
      {
        id: 'questions_bank',
        label: 'AI 生成智能题量',
        value: '360 道',
        trend: '+45 本周已归库',
        trendUp: true,
        icon: 'EditPen',
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
        sublabel: '累计生成仿真试卷 12 套'
      },
      {
        id: 'grading_rate',
        label: '作业综合批阅率',
        value: '94.2%',
        trend: 'AI 辅助批改提效 65%',
        trendUp: true,
        icon: 'CircleCheck',
        gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
        sublabel: '待人工终审作业 14 份'
      }
    ],
    STUDENT: [
      {
        id: 'student_courses',
        label: '当前修读课程',
        value: '4 门',
        trend: '进行中',
        trendUp: true,
        icon: 'Collection',
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
        sublabel: '本周有 2 门需提交作业'
      },
      {
        id: 'pending_tasks',
        label: '待完成学习任务',
        value: '6 项',
        trend: '包含 2 份随堂测验',
        trendUp: false,
        icon: 'Notebook',
        gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
        sublabel: '建议今日完成 1 份'
      },
      {
        id: 'practice_score',
        label: '知识点掌握综合分',
        value: '88.5',
        trend: '+3.2 较上周提升',
        trendUp: true,
        icon: 'TrendCharts',
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
        sublabel: '算法与数据结构表现优异'
      },
      {
        id: 'ai_tutor_chats',
        label: '向 AI 助教提问',
        value: '42 次',
        trend: '累计解决 38 处疑问',
        trendUp: true,
        icon: 'ChatDotRound',
        gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
        sublabel: '获得深度推导与示例代码'
      }
    ]
  },
  shortcuts: [
    {
      id: 'ai_question',
      title: 'AI 出题',
      icon: 'Tickets',
      path: '/ai/question/generate',
      gradient: 'linear-gradient(135deg, #1677FF 0%, #0958D9 100%)'
    },
    {
      id: 'ai_grading',
      title: 'AI 批改',
      icon: 'CircleCheck',
      path: '/ai/grading',
      gradient: 'linear-gradient(135deg, #059669 0%, #047857 100%)'
    },
    {
      id: 'ai_lesson',
      title: 'AI 教案',
      icon: 'Notebook',
      path: '/ai/lesson',
      gradient: 'linear-gradient(135deg, #722ED1 0%, #531DAB 100%)'
    },
    {
      id: 'ai_summary',
      title: 'AI 总结',
      icon: 'DocumentCopy',
      path: '/ai/summary',
      gradient: 'linear-gradient(135deg, #0284C7 0%, #0369A1 100%)'
    },
    {
      id: 'course_ai',
      title: '课程 AI 助手',
      icon: 'Service',
      path: '/course/ai-assistant',
      gradient: 'linear-gradient(135deg, #4F8CFF 0%, #1677FF 100%)'
    },
    {
      id: 'knowledge',
      title: '知识库',
      icon: 'FolderOpened',
      path: '/knowledge',
      gradient: 'linear-gradient(135deg, #7C3AED 0%, #5B21B6 100%)'
    }
  ],
  recentActivities: [
    {
      id: 'act_1',
      title: '使用 AI 智能组卷生成了《数据结构期中综合测试卷（A卷）》',
      user: '张敏 教授',
      avatar: 'ZM',
      time: '10 分钟前',
      tag: '组卷',
      tagType: 'primary'
    },
    {
      id: 'act_2',
      title: '针对《面向对象程序设计》大纲抽取了 16 个核心知识点切片并完成向量化',
      user: '知识库服务',
      avatar: 'KB',
      time: '35 分钟前',
      tag: '知识库',
      tagType: 'info'
    },
    {
      id: 'act_3',
      title: '学生 李浩 刚刚向《离散数学》专属 AI 助教发起了关于连通图的推导提问',
      user: '李浩 (学生)',
      avatar: 'LH',
      time: '1 小时前',
      tag: '助教互动',
      tagType: 'success'
    },
    {
      id: 'act_4',
      title: '完成《计算机网络》第 3 章作业的 AI 自动预判分（共 45 份答卷）',
      user: 'AI 批阅引擎',
      avatar: 'AI',
      time: '2 小时前',
      tag: '智能批改',
      tagType: 'warning'
    }
  ]
};
