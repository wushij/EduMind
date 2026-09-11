import type { DashboardKpiItem, DashboardShortcut, RecentActivityItem } from '@/types/dashboard/dashboard';
import type { DashboardStatistics } from '@/types/analytics/statistics';
import { MOCK_DASHBOARD_DATA } from '@/mock/dashboard';

type DashboardRole = 'ADMIN' | 'TEACHER' | 'STUDENT';

function formatCount(value?: number): string {
  return String(value ?? 0);
}

export function buildKpisFromSummary(
  summary: DashboardStatistics,
  role: DashboardRole
): DashboardKpiItem[] {
  const course = formatCount(summary.courseCount);
  const question = formatCount(summary.questionCount);
  const exam = formatCount(summary.examCount);
  const aiChat = formatCount(summary.aiConversationCount);
  const assignment = formatCount(summary.assignmentCount);
  const pendingGrading = formatCount(summary.pendingGradingCount);
  const pendingAssignment = formatCount(summary.pendingAssignmentCount);

  if (role === 'TEACHER') {
    return [
      {
        id: 'my_courses',
        label: '主讲课程',
        value: `${course} 门`,
        trend: '实时统计',
        trendUp: true,
        icon: 'Reading',
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
        sublabel: '来自 Dashboard API'
      },
      {
        id: 'questions_bank',
        label: '题库题目',
        value: `${question} 道`,
        trend: '可检索组卷',
        trendUp: true,
        icon: 'EditPen',
        gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
        sublabel: '题库模块统计'
      },
      {
        id: 'pending_grading',
        label: '待批改提交',
        value: `${pendingGrading} 份`,
        trend: pendingGrading ? '需优先处理' : '暂无积压',
        trendUp: !pendingGrading,
        icon: 'CircleCheck',
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
        sublabel: '作业批改队列'
      },
      {
        id: 'ai_answers',
        label: 'AI 会话',
        value: `${aiChat} 次`,
        trend: '助教互动',
        trendUp: true,
        icon: 'Service',
        gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
        sublabel: 'AI 模块统计'
      }
    ];
  }

  if (role === 'STUDENT') {
    return [
      {
        id: 'enrolled_courses',
        label: '修读课程',
        value: `${course} 门`,
        trend: '本学期',
        trendUp: true,
        icon: 'Collection',
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
        sublabel: '课程中心统计'
      },
      {
        id: 'pending_assignments',
        label: '待完成作业',
        value: `${pendingAssignment} 项`,
        trend: pendingAssignment ? '请及时提交' : '已全部完成',
        trendUp: !pendingAssignment,
        icon: 'Notebook',
        gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
        sublabel: '作业模块统计'
      },
      {
        id: 'exams',
        label: '可用试卷',
        value: `${exam} 套`,
        trend: '练习与测评',
        trendUp: true,
        icon: 'Tickets',
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
        sublabel: '试卷模块统计'
      },
      {
        id: 'ai_chat',
        label: 'AI 答疑',
        value: `${aiChat} 次`,
        trend: '课程助教',
        trendUp: true,
        icon: 'ChatDotRound',
        gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
        sublabel: 'AI 会话统计'
      }
    ];
  }

  return [
    {
      id: 'courses',
      label: '活跃课程',
      value: `${course} 门`,
      trend: '平台汇总',
      trendUp: true,
      icon: 'Collection',
      gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
      sublabel: 'Dashboard API'
    },
    {
      id: 'questions',
      label: '题库题目',
      value: `${question} 道`,
      trend: '可组卷检索',
      trendUp: true,
      icon: 'EditPen',
      gradient: 'linear-gradient(135deg, #722ED1 0%, #A855F7 100%)',
      sublabel: '题库模块统计'
    },
    {
      id: 'assignments',
      label: '作业任务',
      value: `${assignment} 项`,
      trend: `${pendingGrading} 待批改`,
      trendUp: !pendingGrading,
      icon: 'Notebook',
      gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
      sublabel: '教学模块统计'
    },
    {
      id: 'ai_tokens',
      label: 'AI 会话',
      value: `${aiChat} 次`,
      trend: `${exam} 套试卷`,
      trendUp: true,
      icon: 'Cpu',
      gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
      sublabel: 'AI 与试卷统计'
    }
  ];
}

export function getDashboardShortcuts(): DashboardShortcut[] {
  return MOCK_DASHBOARD_DATA.shortcuts;
}

export function getMockRecentActivities(): RecentActivityItem[] {
  return MOCK_DASHBOARD_DATA.recentActivities;
}

export function mapRecentCourses(
  summary: DashboardStatistics | null,
  useMockFallback: boolean
) {
  if (summary?.recentCourses?.length) {
    const gradients = [
      'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)',
      'linear-gradient(135deg, #722ED1 0%, #C084FC 100%)',
      'linear-gradient(135deg, #059669 0%, #10B981 100%)'
    ];
    return summary.recentCourses.map((course, index) => ({
      id: course.id,
      title: course.name,
      code: `C${course.id}`,
      teacher: '任课教师',
      students: 0,
      chapters: 6,
      progress: 50,
      gradient: gradients[index % gradients.length]
    }));
  }

  if (useMockFallback) {
    return [
      {
        id: 101,
        title: '离散数学与图论基础',
        code: 'MATH201',
        teacher: '张敏 教授',
        students: 124,
        chapters: 12,
        progress: 68,
        gradient: 'linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)'
      },
      {
        id: 102,
        title: '面向对象程序设计 (Java)',
        code: 'CS102',
        teacher: '李峰 教授',
        students: 148,
        chapters: 16,
        progress: 42,
        gradient: 'linear-gradient(135deg, #722ED1 0%, #C084FC 100%)'
      },
      {
        id: 103,
        title: '数据结构与算法深度解析',
        code: 'CS204',
        teacher: '王伟 副教授',
        students: 96,
        chapters: 14,
        progress: 85,
        gradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)'
      }
    ];
  }

  return [];
}
