import { ref, onMounted, onUnmounted, nextTick, shallowRef, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { AiUsageByProviderVO } from '@/types/analytics/learning';

export interface AnalyticsTabItem {
  label: string;
  value: string;
  route: string;
}

const CHART_COLORS = ['#1677FF', '#06B6D4', '#10B981', '#722ED1', '#94A3B8', '#F59E0B'];

export function useAnalyticsOverview(defaultCourseId?: number) {
  const router = useRouter();
  const route = useRoute();
  const { courseOptions, courseId, loadCourses } = useTeacherCourses(defaultCourseId);
  const { fetchOverview } = useLearningAnalytics();

  const avgScore = ref('--');
  const masteryRate = ref('--');
  const participationRate = ref('--');
  const aiCallCount = ref('--');

  const scoreTrendDates = ref<string[]>([]);
  const scoreTrendValues = ref<number[]>([]);
  const masteryCategories = ref<string[]>([]);
  const masteryValues = ref<number[]>([]);
  const activityDates = ref<string[]>([]);
  const activityValues = ref<number[]>([]);
  const aiToolLegend = ref<Array<{ name: string; ratio: number; color: string }>>([]);
  const aiPieData = ref<Array<{ value: number; name: string; itemStyle: { color: string } }>>([]);
  const aiTotalCalls = ref(0);

  const dateRange = ref<[string, string]>(['2024-09-01', '2024-09-30']);
  const selectedTrendRange = ref('近30天');

  const analyticsTabs: AnalyticsTabItem[] = [
    { label: '教学概览', value: 'overview', route: '/analytics' },
    { label: '学生分析', value: 'student', route: '/analytics/learning' },
    { label: '知识点掌握', value: 'mastery', route: '/analytics/mastery' },
    { label: '课程分析', value: 'course', route: '/analytics' },
    { label: 'AI使用分析', value: 'ai_usage', route: '/analytics/ai-usage' }
  ];

  const currentTab = ref('overview');

  const scoreTrendChartRef = ref<HTMLElement | null>(null);
  const knowledgeMasteryChartRef = ref<HTMLElement | null>(null);
  const gradeDistributionChartRef = ref<HTMLElement | null>(null);
  const aiToolUsageChartRef = ref<HTMLElement | null>(null);

  const scoreTrendChart = shallowRef<echarts.ECharts | null>(null);
  const knowledgeMasteryChart = shallowRef<echarts.ECharts | null>(null);
  const gradeDistributionChart = shallowRef<echarts.ECharts | null>(null);
  const aiToolUsageChart = shallowRef<echarts.ECharts | null>(null);

  function handleTabClick(tab: AnalyticsTabItem) {
    currentTab.value = tab.value;
    if (tab.route && tab.route !== route.path) {
      router.push(tab.route);
    }
  }

  function initScoreTrendChart() {
    if (!scoreTrendChartRef.value) return;
    scoreTrendChart.value?.dispose();

    const chart = echarts.init(scoreTrendChartRef.value);
    scoreTrendChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '3%',
        right: '4%',
        top: '14%',
        bottom: '10%',
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.96)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        textStyle: { color: '#1E293B', fontSize: 12 }
      },
      xAxis: {
        type: 'category',
        data: scoreTrendDates.value,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisTick: { show: false },
        axisLabel: { color: '#64748B', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        interval: 20,
        splitLine: {
          lineStyle: { color: '#F1F5F9', type: 'dashed' }
        },
        axisLabel: { color: '#94A3B8', fontSize: 11 }
      },
      series: [
        {
          name: '班级平均分',
          type: 'line',
          smooth: true,
          data: scoreTrendValues.value,
          itemStyle: { color: '#1677FF' },
          lineStyle: { width: 3, color: '#1677FF' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(22, 119, 255, 0.28)' },
              { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
            ])
          }
        }
      ]
    };

    chart.setOption(option);
  }

  function initKnowledgeMasteryChart() {
    if (!knowledgeMasteryChartRef.value) return;
    knowledgeMasteryChart.value?.dispose();

    const chart = echarts.init(knowledgeMasteryChartRef.value);
    knowledgeMasteryChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '4%',
        right: '12%',
        top: '6%',
        bottom: '6%',
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}: {c}%'
      },
      xAxis: {
        type: 'value',
        max: 100,
        show: false
      },
      yAxis: {
        type: 'category',
        data: masteryCategories.value,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#334155', fontSize: 12, fontWeight: 500 }
      },
      series: [
        {
          type: 'bar',
          data: masteryValues.value,
          barWidth: 10,
          showBackground: true,
          backgroundStyle: {
            color: '#F1F5F9',
            borderRadius: 5
          },
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#38BDF8' },
              { offset: 1, color: '#1677FF' }
            ]),
            borderRadius: 5
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{c}%',
            color: '#64748B',
            fontSize: 11,
            fontWeight: 600
          }
        }
      ]
    };

    chart.setOption(option);
  }

  function initGradeDistributionChart() {
    if (!gradeDistributionChartRef.value) return;
    gradeDistributionChart.value?.dispose();

    const chart = echarts.init(gradeDistributionChartRef.value);
    gradeDistributionChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '3%',
        right: '4%',
        top: '12%',
        bottom: '10%',
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}: {c} 人'
      },
      xAxis: {
        type: 'category',
        data: activityDates.value,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisTick: { show: false },
        axisLabel: { color: '#64748B', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        min: 0,
        splitLine: {
          lineStyle: { color: '#F1F5F9', type: 'dashed' }
        },
        axisLabel: { color: '#94A3B8', fontSize: 11 }
      },
      series: [
        {
          type: 'bar',
          data: activityValues.value,
          barWidth: 28,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#3B82F6' },
              { offset: 1, color: '#60A5FA' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }
      ]
    };

    chart.setOption(option);
  }

  function initAiToolUsageChart() {
    if (!aiToolUsageChartRef.value) return;
    aiToolUsageChart.value?.dispose();

    const chart = echarts.init(aiToolUsageChartRef.value);
    aiToolUsageChart.value = chart;

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}% ({d}%)'
      },
      title: {
        text: `${aiTotalCalls.value.toLocaleString()}\n总次数`,
        left: 'center',
        top: '38%',
        textStyle: {
          fontSize: 16,
          fontWeight: 'bold',
          color: '#1E293B',
          lineHeight: 22
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['58%', '78%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          label: { show: false },
          emphasis: {
            scale: true,
            scaleSize: 6
          },
          data: aiPieData.value
        }
      ]
    };

    chart.setOption(option);
  }

  function handleResize() {
    scoreTrendChart.value?.resize();
    knowledgeMasteryChart.value?.resize();
    gradeDistributionChart.value?.resize();
    aiToolUsageChart.value?.resize();
  }

  function resolveRange(): string {
    if (selectedTrendRange.value === '近7天') return '7d';
    if (selectedTrendRange.value === '近学期') return '90d';
    return '30d';
  }

  function formatShortDate(date: string): string {
    const parts = date.split('-');
    return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : date;
  }

  function buildAiLegend(byProvider: AiUsageByProviderVO[]) {
    const total = byProvider.reduce((sum, item) => sum + (item.calls ?? 0), 0);
    aiTotalCalls.value = total;
    aiPieData.value = byProvider.map((item, idx) => ({
      value: item.calls ?? 0,
      name: item.provider || '未知',
      itemStyle: { color: CHART_COLORS[idx % CHART_COLORS.length] }
    }));
    aiToolLegend.value = byProvider.map((item, idx) => ({
      name: item.provider || '未知',
      ratio: total > 0 ? Math.round(((item.calls ?? 0) / total) * 100) : 0,
      color: CHART_COLORS[idx % CHART_COLORS.length]
    }));
  }

  function refreshCharts() {
    nextTick(() => {
      initScoreTrendChart();
      initKnowledgeMasteryChart();
      initGradeDistributionChart();
      initAiToolUsageChart();
    });
  }

  watch(selectedTrendRange, () => {
    loadOverviewData();
  });

  watch(courseId, () => {
    loadOverviewData();
  });

  async function loadOverviewData() {
    if (!courseId.value || courseId.value <= 0) return;
    const range = resolveRange();
    const { learning, aiUsage, mastery } = await fetchOverview(courseId.value, range);
    if (learning) {
      avgScore.value = learning.avgScore != null ? learning.avgScore.toFixed(1) : '--';
      masteryRate.value = `${Math.round((learning.knowledgeMasteryAvg ?? 0) * 100)}%`;
      participationRate.value = `${Math.round((learning.completionRate ?? 0) * 100)}%`;
      scoreTrendDates.value = (learning.trends?.score ?? []).map((p) => formatShortDate(p.date));
      scoreTrendValues.value = (learning.trends?.score ?? []).map((p) => p.avgScore ?? 0);
      activityDates.value = (learning.trends?.learning ?? []).map((p) => formatShortDate(p.date));
      activityValues.value = (learning.trends?.learning ?? []).map((p) => p.activeUsers ?? 0);
    }
    if (mastery) {
      masteryCategories.value = mastery.dimensions ?? [];
      masteryValues.value = mastery.classAvg ?? [];
    }
    if (aiUsage) {
      aiCallCount.value = (aiUsage.totalCalls ?? 0).toLocaleString();
      buildAiLegend(aiUsage.byProvider ?? []);
    }
    refreshCharts();
  }

  onMounted(async () => {
    window.addEventListener('resize', handleResize);
    await loadCourses();
    await loadOverviewData();
  });

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    scoreTrendChart.value?.dispose();
    knowledgeMasteryChart.value?.dispose();
    gradeDistributionChart.value?.dispose();
    aiToolUsageChart.value?.dispose();
  });

  return {
    courseOptions,
    courseId,
    avgScore,
    masteryRate,
    participationRate,
    aiCallCount,
    dateRange,
    selectedTrendRange,
    analyticsTabs,
    currentTab,
    aiToolLegend,
    scoreTrendChartRef,
    knowledgeMasteryChartRef,
    gradeDistributionChartRef,
    aiToolUsageChartRef,
    handleTabClick,
    loadOverviewData
  };
}
