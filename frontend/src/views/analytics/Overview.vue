<template>
  <div class="analytics-overview-container">
    <!-- 1. 页面标题与日期筛选 -->
    <div class="page-header-bar">
      <div class="header-text">
        <h2 class="page-title">教学分析</h2>
        <p class="page-subtitle">数据驱动教学，精准评估效果</p>
      </div>
      <div class="header-date-picker-wrap">
        <el-select v-model="courseId" placeholder="选择课程" style="width: 220px; margin-right: 12px" @change="loadOverviewData">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          value-format="YYYY-MM-DD"
          class="custom-range-picker"
        />
      </div>
    </div>

    <!-- 2. 二级导航 Tabs (对齐原型图 3 中图：教学概览、学生分析、知识点掌握、课程分析、AI使用分析) -->
    <div class="analytics-tabs-bar">
      <div
        v-for="tab in analyticsTabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="handleTabClick(tab)"
      >
        <span>{{ tab.label }}</span>
      </div>
    </div>

    <!-- 3. 4 大核心 KPI 指标卡片 (100% 对齐原型图 3 中图) -->
    <div class="kpi-cards-grid">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">班级平均分</span>
        </div>
        <div class="kpi-value">{{ avgScore }}</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">知识点掌握率</span>
        </div>
        <div class="kpi-value">{{ masteryRate }}</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">学生参与度</span>
        </div>
        <div class="kpi-value">{{ participationRate }}</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">AI调用次数</span>
        </div>
        <div class="kpi-value">{{ aiCallCount }}</div>
      </div>
    </div>

    <!-- 4. 2x2 专业图表矩阵 (成绩趋势 + 知识点掌握情况 + 学生成绩分布 + AI工具使用统计) -->
    <div class="charts-matrix-grid">
      <!-- 图表 1：成绩趋势 (双折线平滑曲线 + 近7天/近30天/近学期切换) -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">成绩趋势</h3>
          <div class="chart-actions">
            <!-- 图例说明 -->
            <div class="chart-legend-dots">
              <span class="legend-dot dot-class"></span>
              <span class="legend-text">班级平均分</span>
              <span class="legend-dot dot-school"></span>
              <span class="legend-text">全校平均分</span>
            </div>
            <!-- 时间快捷切换药丸 -->
            <div class="time-filter-pills">
              <button
                v-for="t in ['近7天', '近30天', '近学期']"
                :key="t"
                type="button"
                class="pill-btn"
                :class="{ active: selectedTrendRange === t }"
                @click="selectedTrendRange = t"
              >
                {{ t }}
              </button>
            </div>
          </div>
        </div>
        <div ref="scoreTrendChartRef" class="chart-body"></div>
      </div>

      <!-- 图表 2：知识点掌握情况 (横向柱状图) -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">知识点掌握情况</h3>
        </div>
        <div ref="knowledgeMasteryChartRef" class="chart-body"></div>
      </div>

      <!-- 图表 3：学习活跃度 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">学习活跃度</h3>
        </div>
        <div ref="gradeDistributionChartRef" class="chart-body"></div>
      </div>

      <!-- 图表 4：AI工具使用统计 (环形饼图) -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">AI工具使用统计</h3>
        </div>
        <div class="donut-chart-container">
          <div ref="aiToolUsageChartRef" class="donut-chart-canvas"></div>
          <div class="donut-legend-col">
            <div
              v-for="item in aiToolLegend"
              :key="item.name"
              class="donut-legend-row"
            >
              <span class="legend-circle" :style="{ backgroundColor: item.color }"></span>
              <span class="legend-name">{{ item.name }}</span>
              <span class="legend-ratio">{{ item.ratio }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, shallowRef, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import { getAiUsageAnalytics, getLearningAnalytics } from '@/api/analytics/learning';
import { getKnowledgeMastery } from '@/api/analytics/knowledge';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { AiUsageByProviderVO } from '@/types/analytics/learning';

const { courseOptions, courseId } = useTeacherCourses(102);

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

const CHART_COLORS = ['#1677FF', '#06B6D4', '#10B981', '#722ED1', '#94A3B8', '#F59E0B'];

const router = useRouter();
const route = useRoute();

// 日期选择范围 (默认近 30 天)
const dateRange = ref<[string, string]>(['2024-09-01', '2024-09-30']);

// 趋势图时间切换
const selectedTrendRange = ref('近30天');

// 二级导航 Tabs (对齐原型图 3 中图)
interface AnalyticsTabItem {
  label: string;
  value: string;
  route: string;
}

const analyticsTabs: AnalyticsTabItem[] = [
  { label: '教学概览', value: 'overview', route: '/analytics' },
  { label: '学生分析', value: 'student', route: '/analytics/learning' },
  { label: '知识点掌握', value: 'mastery', route: '/analytics/mastery' },
  { label: '课程分析', value: 'course', route: '/analytics' },
  { label: 'AI使用分析', value: 'ai_usage', route: '/analytics/ai-usage' }
];

const currentTab = ref('overview');

function handleTabClick(tab: AnalyticsTabItem) {
  currentTab.value = tab.value;
  if (tab.route && tab.route !== route.path) {
    router.push(tab.route);
  }
}

// ECharts Refs
const scoreTrendChartRef = ref<HTMLElement | null>(null);
const knowledgeMasteryChartRef = ref<HTMLElement | null>(null);
const gradeDistributionChartRef = ref<HTMLElement | null>(null);
const aiToolUsageChartRef = ref<HTMLElement | null>(null);

const scoreTrendChart = shallowRef<echarts.ECharts | null>(null);
const knowledgeMasteryChart = shallowRef<echarts.ECharts | null>(null);
const gradeDistributionChart = shallowRef<echarts.ECharts | null>(null);
const aiToolUsageChart = shallowRef<echarts.ECharts | null>(null);

// 1. 初始化成绩趋势图 (双折线平滑曲线)
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

// 2. 初始化知识点掌握情况 (横向柱状图)
function initKnowledgeMasteryChart() {
  if (!knowledgeMasteryChartRef.value) return;
  knowledgeMasteryChart.value?.dispose();

  const chart = echarts.init(knowledgeMasteryChartRef.value);
  knowledgeMasteryChart.value = chart;

  const categories = masteryCategories.value;
  const values = masteryValues.value;

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
      data: categories,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#334155', fontSize: 12, fontWeight: 500 }
    },
    series: [
      {
        type: 'bar',
        data: values,
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

// 3. 初始化学生成绩分布 (直方图)
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

// 4. 初始化 AI 工具使用统计 (环形饼图)
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
  const range = resolveRange();
  try {
    const [learningRes, aiRes, masteryRes] = await Promise.all([
      getLearningAnalytics({ courseId: courseId.value, range }),
      getAiUsageAnalytics({ courseId: courseId.value, range }),
      getKnowledgeMastery({ courseId: courseId.value })
    ]);
    const learning = learningRes.data;
    if (learning) {
      avgScore.value = learning.avgScore != null ? learning.avgScore.toFixed(1) : '--';
      masteryRate.value = `${Math.round((learning.knowledgeMasteryAvg ?? 0) * 100)}%`;
      participationRate.value = `${Math.round((learning.completionRate ?? 0) * 100)}%`;
      scoreTrendDates.value = (learning.trends?.score ?? []).map((p) => formatShortDate(p.date));
      scoreTrendValues.value = (learning.trends?.score ?? []).map((p) => p.avgScore ?? 0);
      activityDates.value = (learning.trends?.learning ?? []).map((p) => formatShortDate(p.date));
      activityValues.value = (learning.trends?.learning ?? []).map((p) => p.activeUsers ?? 0);
    }
    const mastery = masteryRes.data;
    if (mastery) {
      masteryCategories.value = mastery.dimensions ?? [];
      masteryValues.value = mastery.classAvg ?? [];
    }
    const aiUsage = aiRes.data;
    if (aiUsage) {
      aiCallCount.value = (aiUsage.totalCalls ?? 0).toLocaleString();
      buildAiLegend(aiUsage.byProvider ?? []);
    }
  } catch {
    // keep current values
  }
  refreshCharts();
}

onMounted(() => {
  nextTick(() => {
    window.addEventListener('resize', handleResize);
    loadOverviewData();
  });
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  scoreTrendChart.value?.dispose();
  knowledgeMasteryChart.value?.dispose();
  gradeDistributionChart.value?.dispose();
  aiToolUsageChart.value?.dispose();
});
</script>

<style scoped lang="scss">
.analytics-overview-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  .page-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
  }

  .page-subtitle {
    margin: 4px 0 0;
    font-size: 14px;
    color: #64748b;
  }
}

// 顶部日期选择器
.header-date-picker-wrap {
  display: flex;
  align-items: center;

  :deep(.el-range-editor.el-input__wrapper) {
    background: #FFFFFF;
    border-radius: 9999px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
    border: none;
    padding: 3px 14px;
  }
}

// 二级导航 Tabs (对齐原型图 3 中图)
.analytics-tabs-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #E2E8F0;
  padding-bottom: 2px;

  .tab-item {
    padding: 8px 18px;
    font-size: 14px;
    font-weight: 500;
    color: #64748B;
    cursor: pointer;
    border-radius: 8px 8px 0 0;
    transition: all 0.2s ease;
    position: relative;

    &:hover {
      color: #1677FF;
    }

    &.active {
      color: #1677FF;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        right: 0;
        height: 2.5px;
        background: #1677FF;
        border-radius: 2px;
      }
    }
  }
}

// 3. 4 大核心 KPI 指标卡片
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .kpi-card {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 18px 22px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .kpi-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;

      .kpi-label {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
      }

      .trend-badge {
        font-size: 11px;
        padding: 1px 7px;
        border-radius: 9999px;
        font-weight: 600;

        &--up {
          background: #ECFDF5;
          color: #10B981;
        }
      }
    }

    .kpi-value {
      font-size: 28px;
      font-weight: 800;
      color: #0F172A;
      letter-spacing: -0.5px;
    }
  }
}

// 4. 2x2 专业图表矩阵
.charts-matrix-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }

  .chart-card {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 22px 24px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    flex-direction: column;
    min-height: 310px;

    .chart-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 14px;

      .chart-title {
        font-size: 15px;
        font-weight: 700;
        color: #1E293B;
        margin: 0;
      }

      .chart-actions {
        display: flex;
        align-items: center;
        gap: 16px;

        .chart-legend-dots {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 11.5px;
          color: #64748B;

          .legend-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;

            &.dot-class {
              background: #1677FF;
            }

            &.dot-school {
              background: #38BDF8;
              margin-left: 8px;
            }
          }
        }

        .time-filter-pills {
          display: flex;
          align-items: center;
          background: #F1F5F9;
          padding: 2px;
          border-radius: 8px;

          .pill-btn {
            background: transparent;
            border: none;
            outline: none;
            padding: 3px 9px;
            border-radius: 6px;
            font-size: 11px;
            color: #64748B;
            cursor: pointer;
            transition: all 0.15s ease;

            &.active {
              background: #FFFFFF;
              color: #1677FF;
              font-weight: 600;
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
            }
          }
        }
      }
    }

    .chart-body {
      width: 100%;
      height: 240px;
      flex: 1;
    }

    // 环形图容器
    .donut-chart-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex: 1;
      height: 240px;

      .donut-chart-canvas {
        width: 60%;
        height: 100%;
      }

      .donut-legend-col {
        width: 40%;
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-left: 10px;

        .donut-legend-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 12px;

          .legend-circle {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            margin-right: 6px;
            flex-shrink: 0;
          }

          .legend-name {
            color: #475569;
            flex: 1;
          }

          .legend-ratio {
            font-weight: 600;
            color: #1E293B;
          }
        }
      }
    }
  }
}
</style>
