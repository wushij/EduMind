import { ref, computed, watch, onMounted, onUnmounted, nextTick, shallowRef, type Component } from 'vue';
import { useRouter, type Router } from 'vue-router';
import {
  EditPen,
  Tickets,
  CircleCheck,
  TrendCharts,
  FolderOpened,
  ChatDotRound,
  Reading,
  DocumentCopy,
  Collection,
  Notebook
} from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getDashboardSummary } from '@/api/dashboard/dashboard';
import { getAiUsageAnalytics } from '@/api/analytics/learning';
import { listNotifications, getUnreadCount } from '@/api/notification/index';
import type { DashboardStatistics } from '@/types/analytics/statistics';
import type { AiUsageAnalyticsVO } from '@/types/analytics/learning';
import type { NotificationVO } from '@/types/notification/index';
import type { IconTheme } from '@/types/common/icon';
import { useAuthStore } from '@/stores/auth/auth';
import { canAccessRoute } from '@/utils/router/route-access';

export interface CommonFunctionItem {
  title: string;
  icon: Component;
  theme: IconTheme;
  route: string;
  desc?: string;
}

export interface TodoItem {
  id: string | number;
  title: string;
  count: string;
  dotColor: string;
  route: string;
  urgent?: boolean;
}

export interface AbilityLegendItem {
  label: string;
  percent: number;
  color: string;
  count?: number;
}

export interface LatestNewsItem {
  id: number | string;
  title: string;
  time: string;
  tagColor: string;
  category?: string;
  route?: string;
}

export interface KpiCardItem {
  id: string;
  label: string;
  value: string | number;
  unit?: string;
  sublabel: string;
  trend: string;
  trendUp: boolean;
  route: string;
  icon: Component;
  theme: 'blue' | 'purple' | 'emerald' | 'amber';
}

export const COMMON_FUNCTIONS: CommonFunctionItem[] = [
  { title: 'AI 工具广场', icon: EditPen, theme: 'blue', route: '/ai/marketplace', desc: '备课批改类 AI 工具统一入口' },
  { title: '智能出题组卷', icon: Tickets, theme: 'cyan', route: '/ai/question/generate', desc: '按大纲自适应多题型生成' },
  { title: 'AI 作业批改', icon: CircleCheck, theme: 'emerald', route: '/ai/grading', desc: '全自动语义批阅与错因诊断' },
  { title: '学情全景分析', icon: TrendCharts, theme: 'amber', route: '/analytics/learning', desc: '多维认知掌握与薄弱点透视' },
  { title: '课程资源中心', icon: FolderOpened, theme: 'rose', route: '/course', desc: '教案课件与视频多媒体管理' },
  { title: '课程 AI 助教', icon: ChatDotRound, theme: 'purple', route: '/course/ai-assistant', desc: '24小时智能在线释疑解惑' },
  { title: '知识库管理', icon: Collection, theme: 'indigo', route: '/knowledge', desc: '文档切片切分与向量索引库' },
  { title: 'AI 教学总结', icon: DocumentCopy, theme: 'teal', route: '/ai/summary', desc: '课堂互动回顾与教学洞察' }
];

export const DEFAULT_TODO_ITEMS: TodoItem[] = [
  { id: 1, title: '待批改作业', count: '12 份', dotColor: '#3B82F6', route: '/question/submissions' },
  { id: 2, title: '待审核课程资源', count: '3 个', dotColor: '#10B981', route: '/course' },
  { id: 3, title: '学生问题咨询', count: '8 条', dotColor: '#EF4444', route: '/course/ai-assistant' },
  { id: 4, title: '课堂反馈待处理', count: '5 条', dotColor: '#F59E0B', route: '/analytics' },
  { id: 5, title: '系统通知', count: '2 条', dotColor: '#3B82F6', route: '/dashboard/recent' }
];

export const ABILITY_LEGEND: AbilityLegendItem[] = [
  { label: '优秀', percent: 24, color: '#2563EB' },
  { label: '良好', percent: 36, color: '#06B6D4' },
  { label: '中等', percent: 28, color: '#F59E0B' },
  { label: '待提升', percent: 12, color: '#F97316' }
];

export const LATEST_NEWS: LatestNewsItem[] = [
  { id: 1, title: '三年级数学单元测试', time: '10:24', tagColor: '#3B82F6' },
  { id: 2, title: 'AI 生成的教案已完成', time: '09:18', tagColor: '#8B5CF6' },
  { id: 3, title: '学生提交了作业', time: '昨天', tagColor: '#10B981' },
  { id: 4, title: '新课程资源已上线', time: '昨天', tagColor: '#F59E0B' },
  { id: 5, title: '系统版本更新', time: '05-20', tagColor: '#64748B' }
];

export const TEACHING_ACTIVITY_DATA = [45, 52, 60, 56, 70, 75, 78];

const STUDENT_FUNCTIONS: CommonFunctionItem[] = [
  { title: 'AI 自适应练习', icon: EditPen, theme: 'blue', route: '/learning/practice', desc: '基于掌握度智能推荐题目' },
  { title: '我的修读课程', icon: Reading, theme: 'cyan', route: '/course', desc: '课件查阅与课堂课后复习' },
  { title: '课程 AI 助教', icon: ChatDotRound, theme: 'purple', route: '/course/ai-assistant', desc: '不懂就问，名师级AI点拨' },
  { title: '智能错题本', icon: Notebook, theme: 'rose', route: '/learning/wrong-book', desc: '智能归因与举一反三巩固' },
  { title: '个人学情报告', icon: TrendCharts, theme: 'amber', route: '/learning/report', desc: '知识图谱掌握度实时画像' },
  // 学生作业入口必须指向学习中心待办任务页：/question/assignments 是教师作业管理页（ADMIN/TEACHER）
  { title: '课程作业提交', icon: CircleCheck, theme: 'emerald', route: '/learning/tasks', desc: '在线作答与即时智能测评' }
];

export function buildLineChartOption(daily?: Array<{ date: string; calls?: number }>): echarts.EChartsOption {
  let dates: string[] = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  let values: number[] = TEACHING_ACTIVITY_DATA;

  if (daily && daily.length > 0) {
    dates = daily.map((d) => (d.date.length > 5 ? d.date.slice(5) : d.date));
    values = daily.map((d) => d.calls || 0);
  }

  return {
    grid: {
      left: '2%',
      right: '4%',
      top: '12%',
      bottom: '8%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: { color: '#1E293B', fontSize: 12 },
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        return `<div style="font-weight:600;margin-bottom:4px">${item.axisValue}</div>
                <div style="color:#2563EB">AI 教学交互: <b>${item.value}</b> 次</div>`;
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#CBD5E1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 }
    },
    series: [
      {
        name: '课堂活跃度',
        type: 'line',
        data: values,
        smooth: true,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#2563EB', borderColor: '#FFFFFF', borderWidth: 2 },
        lineStyle: { width: 3, color: '#2563EB' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.35)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      }
    ]
  };
}

export function buildPieChartOption(legend?: AbilityLegendItem[]): echarts.EChartsOption {
  const data = (legend || ABILITY_LEGEND).map((item) => ({
    value: item.percent,
    name: item.label.split(' ')[0],
    itemStyle: { color: item.color }
  }));

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: { color: '#1E293B', fontSize: 12 },
      formatter: '{b}: {c}%'
    },
    series: [
      {
        name: '学生能力分布',
        type: 'pie',
        radius: ['52%', '78%'],
        center: ['48%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 6
        },
        data
      }
    ]
  };
}

export function useDashboard(options?: { enableCharts?: boolean }) {
  const router: Router = useRouter();
  const authStore = useAuthStore();
  const enableCharts = options?.enableCharts ?? false;

  const loading = ref(false);
  const trendLoading = ref(false);
  const usedMockFallback = ref(false);
  const summary = ref<DashboardStatistics | null>(null);
  const unreadNoticeCount = ref(0);
  const recentNotifications = ref<NotificationVO[]>([]);
  const aiUsageData = ref<AiUsageAnalyticsVO | null>(null);
  const selectedDataPeriod = ref<'WEEK' | 'MONTH'>('WEEK');

  const teachingLineChartRef = ref<HTMLElement | null>(null);
  const abilityPieChartRef = ref<HTMLElement | null>(null);
  const lineChartInstance = shallowRef<echarts.ECharts | null>(null);
  const pieChartInstance = shallowRef<echarts.ECharts | null>(null);

  const isStudent = computed(() => authStore.hasRole('STUDENT'));

  const commonFunctions = computed<CommonFunctionItem[]>(() => {
    const rawList = isStudent.value ? STUDENT_FUNCTIONS : COMMON_FUNCTIONS;
    return rawList.filter((fn) => canAccessRoute(router, fn.route));
  });

  const kpiCards = computed<KpiCardItem[]>(() => {
    const s = summary.value;
    const courses = s?.courseCount ?? 0;
    const questions = s?.questionCount ?? 0;
    const aiChats = s?.aiConversationCount ?? 0;
    const assignments = s?.assignmentCount ?? 0;
    const pendingGrading = s?.pendingGradingCount ?? 0;
    const pendingAssignments = s?.pendingAssignmentCount ?? 0;

    if (isStudent.value) {
      return [
        {
          id: 'courses',
          label: '在修核心课程',
          value: courses,
          unit: '门',
          sublabel: '学期培养方案必修与选修',
          trend: '在学进度平稳',
          trendUp: true,
          route: '/course',
          icon: Reading,
          theme: 'blue'
        },
        {
          id: 'questions',
          label: '智能题库题量',
          value: questions,
          unit: '道',
          sublabel: '已开放自适应练习题库',
          trend: '覆盖核心考点',
          trendUp: true,
          route: '/learning/practice',
          icon: Tickets,
          theme: 'emerald'
        },
        {
          id: 'aiChats',
          label: 'AI 助教互动释疑',
          value: aiChats,
          unit: '次',
          sublabel: '随时启发式提问与代码推导',
          trend: '学业疑问实时解答',
          trendUp: true,
          route: '/course/ai-assistant',
          icon: ChatDotRound,
          theme: 'purple'
        },
        {
          id: 'assignments',
          label: '待完成学习任务',
          value: pendingAssignments,
          unit: '项',
          sublabel: `已提交作业 ${(assignments - pendingAssignments) > 0 ? (assignments - pendingAssignments) : 0} 份`,
          trend: pendingAssignments > 0 ? '建议今日优先完成' : '全部已按期提交',
          trendUp: pendingAssignments === 0,
          route: '/learning/tasks',
          icon: CircleCheck,
          theme: 'amber'
        }
      ];
    }

    return [
      {
        id: 'courses',
        label: '主讲教学课程',
        value: courses,
        unit: '门',
        sublabel: s?.recentCourses?.length ? `最近活跃: ${s.recentCourses[0]?.name || '教学推进中'}` : '学科课程体系正常推进',
        trend: '+12% 活跃度',
        trendUp: true,
        route: '/course',
        icon: Collection,
        theme: 'blue'
      },
      {
        id: 'questions',
        label: '智能题库与资产',
        value: questions,
        unit: '道',
        sublabel: '涵盖各章节客观与综合题',
        trend: 'AI 组卷随时调用',
        trendUp: true,
        route: '/question/bank',
        icon: Tickets,
        theme: 'emerald'
      },
      {
        id: 'aiChats',
        label: '课程 AI 助教服务',
        value: aiChats,
        unit: '次',
        sublabel: '分担多门课程重复概念咨询',
        trend: '+24.5% 本周环比',
        trendUp: true,
        route: '/course/ai-assistant',
        icon: ChatDotRound,
        theme: 'purple'
      },
      {
        id: 'assignments',
        label: '待批阅学生答卷',
        value: pendingGrading,
        unit: '份',
        sublabel: `作业发布总量 ${assignments} 份`,
        trend: pendingGrading > 0 ? '需及时批改反馈' : '暂无积压批改',
        trendUp: pendingGrading === 0,
        route: '/question/submissions',
        icon: CircleCheck,
        theme: 'amber'
      }
    ];
  });

  const todoItems = computed<TodoItem[]>(() => {
    const s = summary.value;
    const items: TodoItem[] = [];

    if (isStudent.value) {
      const pendingAss = s?.pendingAssignmentCount ?? 0;
      if (pendingAss > 0) {
        items.push({
          id: 'todo-pending-ass',
          title: '待完成随堂与课后作业',
          count: `${pendingAss} 份待提交`,
          dotColor: '#EF4444',
          route: '/learning/tasks',
          urgent: true
        });
      }
      items.push({
        id: 'todo-wrong-book',
        title: '智能错题本薄弱项复习',
        count: '待强化',
        dotColor: '#F59E0B',
        route: '/learning/wrong-book'
      });
      items.push({
        id: 'todo-practice',
        title: '今日 AI 自适应针对性练习',
        count: '建议进行',
        dotColor: '#10B981',
        route: '/learning/practice'
      });
    } else {
      const pendingGrading = s?.pendingGradingCount ?? 0;
      if (pendingGrading > 0) {
        items.push({
          id: 'todo-grading',
          title: '待批阅学生作业与测试',
          count: `${pendingGrading} 份待处理`,
          dotColor: '#EF4444',
          route: '/question/submissions',
          urgent: true
        });
      }
      items.push({
        id: 'todo-lesson',
        title: '本周备课与教案编排更新',
        count: '推进中',
        dotColor: '#3B82F6',
        route: '/course'
      });
      items.push({
        id: 'todo-analytics',
        title: '学生掌握度薄弱点诊断',
        count: '实时追踪',
        dotColor: '#F59E0B',
        route: '/analytics/learning'
      });
      items.push({
        id: 'todo-qa',
        title: '学生疑难问题与助教答疑反馈',
        count: `${s?.aiConversationCount ? Math.min(s.aiConversationCount, 5) : 0} 条`,
        dotColor: '#8B5CF6',
        route: '/course/ai-assistant'
      });
    }

    if (unreadNoticeCount.value > 0) {
      items.push({
        id: 'todo-notice',
        title: '未读教务通知与系统公告',
        count: `${unreadNoticeCount.value} 条未读`,
        dotColor: '#3B82F6',
        route: '/dashboard/recent',
        urgent: false
      });
    }

    return items.length > 0 ? items : DEFAULT_TODO_ITEMS;
  });

  const abilityLegend = computed<AbilityLegendItem[]>(() => ABILITY_LEGEND);

  const latestNews = computed<LatestNewsItem[]>(() => {
    if (recentNotifications.value.length > 0) {
      return recentNotifications.value.map((n, idx) => {
        let tagColor = '#3B82F6';
        let category = '通知';
        if (n.type === 'SYSTEM' || n.type === 'BROADCAST') {
          tagColor = '#8B5CF6';
          category = '系统';
        } else if (n.type === 'COURSE') {
          tagColor = '#10B981';
          category = '课程';
        } else if (n.type === 'EXAM' || n.type === 'ASSIGNMENT') {
          tagColor = '#F59E0B';
          category = '作业';
        }
        return {
          id: n.id ?? idx,
          title: n.title || '系统业务动态更新',
          time: formatRelativeTime(n.createTime),
          tagColor,
          category,
          route: '/dashboard/recent'
        };
      });
    }
    return LATEST_NEWS.map((item) => ({
      ...item,
      category: '动态',
      route: '/dashboard/recent'
    }));
  });

  function formatRelativeTime(dateStr?: string) {
    if (!dateStr) return '刚刚';
    const now = new Date().getTime();
    const target = new Date(dateStr).getTime();
    if (Number.isNaN(target)) return dateStr.slice(5, 10);
    const diff = Math.max(0, now - target);
    const mins = Math.floor(diff / (1000 * 60));
    if (mins < 1) return '刚刚';
    if (mins < 60) return `${mins} 分钟前`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours} 小时前`;
    const days = Math.floor(hours / 24);
    if (days <= 7) return `${days} 天前`;
    return dateStr.slice(5, 10);
  }

  async function fetchSummary() {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const [sumRes, unreadRes, notifRes] = await Promise.allSettled([
        getDashboardSummary(),
        getUnreadCount(),
        listNotifications({ page: 1, pageSize: 5 })
      ]);

      if (sumRes.status === 'fulfilled' && sumRes.value?.data) {
        summary.value = sumRes.value.data;
      }
      if (unreadRes.status === 'fulfilled' && unreadRes.value?.data) {
        unreadNoticeCount.value = unreadRes.value.data.unreadCount ?? 0;
      }
      if (notifRes.status === 'fulfilled' && notifRes.value?.data) {
        const listData = notifRes.value.data.list;
        if (Array.isArray(listData)) {
          recentNotifications.value = listData;
        } else if (listData && Array.isArray((listData as any).list)) {
          recentNotifications.value = (listData as any).list;
        }
      }
    } catch {
      // 优雅容错
    } finally {
      loading.value = false;
    }
  }

  async function fetchTrendData() {
    trendLoading.value = true;
    const range = selectedDataPeriod.value === 'MONTH' ? '30d' : '7d';
    try {
      const res = await getAiUsageAnalytics({ range });
      if (res.data) {
        aiUsageData.value = res.data;
      }
    } catch {
      aiUsageData.value = null;
    } finally {
      trendLoading.value = false;
      renderLineChart();
    }
  }

  function renderLineChart() {
    if (!teachingLineChartRef.value) return;
    if (!lineChartInstance.value) {
      lineChartInstance.value = echarts.init(teachingLineChartRef.value);
    }
    const option = buildLineChartOption(aiUsageData.value?.daily);
    lineChartInstance.value.setOption(option, true);
  }

  function renderPieChart() {
    if (!abilityPieChartRef.value) return;
    if (!pieChartInstance.value) {
      pieChartInstance.value = echarts.init(abilityPieChartRef.value);
    }
    const option = buildPieChartOption(abilityLegend.value);
    pieChartInstance.value.setOption(option, true);
  }

  function handleResize() {
    lineChartInstance.value?.resize();
    pieChartInstance.value?.resize();
  }

  function handleTodoClick(item: TodoItem) {
    router.push(item.route);
  }

  function handleWatchVideo() {
    router.push('/course/ai-assistant');
  }

  watch(selectedDataPeriod, () => {
    fetchTrendData();
  });

  onMounted(async () => {
    await fetchSummary();
    if (enableCharts) {
      await fetchTrendData();
      nextTick(() => {
        renderLineChart();
        renderPieChart();
        window.addEventListener('resize', handleResize);
      });
    }
  });

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    lineChartInstance.value?.dispose();
    pieChartInstance.value?.dispose();
  });

  return {
    router,
    summary,
    loading,
    trendLoading,
    usedMockFallback,
    kpiCards,
    commonFunctions,
    todoItems,
    selectedDataPeriod,
    abilityLegend,
    latestNews,
    unreadNoticeCount,
    teachingLineChartRef,
    abilityPieChartRef,
    fetchSummary,
    fetchTrendData,
    handleTodoClick,
    handleWatchVideo
  };
}
