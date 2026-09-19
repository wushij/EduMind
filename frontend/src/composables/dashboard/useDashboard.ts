import { ref, onMounted, onUnmounted, nextTick, shallowRef, type Component } from 'vue';
import { useRouter } from 'vue-router';
import {
  EditPen,
  Tickets,
  CircleCheck,
  TrendCharts,
  FolderOpened,
  ChatDotRound,
  Histogram,
  Connection
} from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getDashboardSummary } from '@/api/dashboard/dashboard';
import { DashboardStatistics } from '@/types/analytics/statistics';
import type { IconTheme } from '@/types/common/icon';
import { USE_MOCK } from '@/config/mock';
import { MOCK_DASHBOARD_DATA } from '@/mock/dashboard';

export interface CommonFunctionItem {
  title: string;
  icon: Component;
  theme: IconTheme;
  route: string;
}

export interface TodoItem {
  id: number;
  title: string;
  count: string;
  dotColor: string;
  route: string;
}

export interface AbilityLegendItem {
  label: string;
  percent: number;
  color: string;
}

export interface LatestNewsItem {
  id: number;
  title: string;
  time: string;
  tagColor: string;
}

export const COMMON_FUNCTIONS: CommonFunctionItem[] = [
  { title: 'AI 备课', icon: EditPen, theme: 'blue', route: '/ai/lesson' },
  { title: '智能出题', icon: Tickets, theme: 'cyan', route: '/ai/question/generate' },
  { title: '作业批改', icon: CircleCheck, theme: 'emerald', route: '/ai/grading' },
  { title: '学情分析', icon: TrendCharts, theme: 'amber', route: '/analytics/learning' },
  { title: '课程资源', icon: FolderOpened, theme: 'rose', route: '/course' },
  { title: '课堂互动', icon: ChatDotRound, theme: 'purple', route: '/course/ai-assistant' },
  { title: '教学评价', icon: Histogram, theme: 'indigo', route: '/analytics' },
  { title: '家校沟通', icon: Connection, theme: 'teal', route: '/dashboard' }
];

export const DEFAULT_TODO_ITEMS: TodoItem[] = [
  { id: 1, title: '待批改作业', count: '12 份', dotColor: '#3B82F6', route: '/question/submissions' },
  { id: 2, title: '待审核课程资源', count: '3 个', dotColor: '#10B981', route: '/course' },
  { id: 3, title: '学生问题咨询', count: '8 条', dotColor: '#EF4444', route: '/course/ai-assistant' },
  { id: 4, title: '课堂反馈待处理', count: '5 条', dotColor: '#F59E0B', route: '/analytics' },
  { id: 5, title: '系统通知', count: '2 条', dotColor: '#3B82F6', route: '/dashboard' }
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

export function buildLineChartOption(): echarts.EChartsOption {
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
      textStyle: {
        color: '#1E293B',
        fontSize: 12
      },
      formatter: '{b}: 活跃度 {c}%'
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: {
        lineStyle: { color: '#CBD5E1' }
      },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748B',
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      interval: 25,
      splitLine: {
        lineStyle: {
          color: '#F1F5F9',
          type: 'dashed'
        }
      },
      axisLabel: {
        show: false
      }
    },
    series: [
      {
        name: '课堂活跃度',
        type: 'line',
        data: TEACHING_ACTIVITY_DATA,
        smooth: true,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: '#2563EB',
          borderColor: '#FFFFFF',
          borderWidth: 2
        },
        lineStyle: {
          width: 3,
          color: '#2563EB'
        },
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

export function buildPieChartOption(): echarts.EChartsOption {
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B',
        fontSize: 12
      },
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
        data: [
          { value: 24, name: '优秀', itemStyle: { color: '#2563EB' } },
          { value: 36, name: '良好', itemStyle: { color: '#06B6D4' } },
          { value: 28, name: '中等', itemStyle: { color: '#F59E0B' } },
          { value: 12, name: '待提升', itemStyle: { color: '#F97316' } }
        ]
      }
    ]
  };
}

export function useDashboard(options?: { enableCharts?: boolean }) {
  const router = useRouter();
  const enableCharts = options?.enableCharts ?? false;

  const summary = ref<DashboardStatistics | null>(null);
  const loading = ref(false);
  const usedMockFallback = ref(false);

  const commonFunctions = COMMON_FUNCTIONS;
  const todoItems = DEFAULT_TODO_ITEMS;
  const selectedDataPeriod = ref('WEEK');
  const abilityLegend = ABILITY_LEGEND;
  const latestNews = LATEST_NEWS;

  const teachingLineChartRef = ref<HTMLElement | null>(null);
  const abilityPieChartRef = ref<HTMLElement | null>(null);
  const lineChartInstance = shallowRef<echarts.ECharts | null>(null);
  const pieChartInstance = shallowRef<echarts.ECharts | null>(null);

  async function fetchSummary() {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getDashboardSummary();
      summary.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        summary.value = {
          courseCount: 3,
          questionCount: 128,
          examCount: 5,
          aiConversationCount: 12,
          assignmentCount: 0,
          pendingGradingCount: 0,
          pendingAssignmentCount: 0,
          recentCourses: MOCK_DASHBOARD_DATA.kpiStats.ADMIN.length
            ? [
                { id: 101, name: '高等数学（上）' },
                { id: 102, name: '数据结构与算法深度解析' }
              ]
            : []
        };
      } else {
        summary.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  function handleTodoClick(item: TodoItem) {
    router.push(item.route);
  }

  function handleWatchVideo() {
    // 预留：产品介绍视频
  }

  function initLineChart() {
    if (!teachingLineChartRef.value) return;
    if (lineChartInstance.value) {
      lineChartInstance.value.dispose();
    }

    const chart = echarts.init(teachingLineChartRef.value);
    lineChartInstance.value = chart;
    chart.setOption(buildLineChartOption());
  }

  function initPieChart() {
    if (!abilityPieChartRef.value) return;
    if (pieChartInstance.value) {
      pieChartInstance.value.dispose();
    }

    const chart = echarts.init(abilityPieChartRef.value);
    pieChartInstance.value = chart;
    chart.setOption(buildPieChartOption());
  }

  function handleResize() {
    lineChartInstance.value?.resize();
    pieChartInstance.value?.resize();
  }

  function disposeCharts() {
    window.removeEventListener('resize', handleResize);
    lineChartInstance.value?.dispose();
    pieChartInstance.value?.dispose();
  }

  onMounted(() => {
    fetchSummary();

    if (enableCharts) {
      nextTick(() => {
        initLineChart();
        initPieChart();
        window.addEventListener('resize', handleResize);
      });
    }
  });

  onUnmounted(() => {
    if (enableCharts) {
      disposeCharts();
    }
  });

  return {
    router,
    summary,
    loading,
    usedMockFallback,
    commonFunctions,
    todoItems,
    selectedDataPeriod,
    abilityLegend,
    latestNews,
    teachingLineChartRef,
    abilityPieChartRef,
    fetchSummary,
    handleTodoClick,
    handleWatchVideo
  };
}
