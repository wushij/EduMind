<template>
  <div ref="chartRef" class="learning-chart" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { LearningTrendData } from '@/types/analytics/learning';
import type { LearningReportTrendsVO } from '@/types/learning/report';

const props = withDefaults(
  defineProps<{
    trends?: LearningTrendData | null;
    personalTrends?: LearningReportTrendsVO | null;
    mode?: 'class' | 'personal';
    height?: string;
  }>(),
  {
    trends: null,
    personalTrends: null,
    mode: 'class',
    height: '320px'
  }
);

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

function buildOption(): echarts.EChartsOption {
  const isPersonal = props.mode === 'personal';
  const study = isPersonal
    ? (props.personalTrends?.studyMinutesByDate ?? [])
    : (props.trends?.learning ?? []);
  const score = isPersonal
    ? (props.personalTrends?.scoreByDate ?? [])
    : (props.trends?.score ?? []);
  const dates = study.map((item) => item.date);
  const barLabel = isPersonal ? '学习分钟' : '活跃学生';
  const barData = isPersonal
    ? study.map((item) => ('minutes' in item ? item.minutes : 0))
    : study.map((item) => ('activeUsers' in item ? item.activeUsers : 0));

  return {
    grid: { left: '3%', right: '4%', top: '14%', bottom: '10%', containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: { color: '#1E293B', fontSize: 12 }
    },
    legend: {
      data: [barLabel, '平均分'],
      top: 0,
      textStyle: { color: '#64748B', fontSize: 12 }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#CBD5E1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: isPersonal ? '分钟' : '人数',
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: { color: '#94A3B8', fontSize: 11 }
      },
      {
        type: 'value',
        name: '分数',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { color: '#94A3B8', fontSize: 11 }
      }
    ],
    series: [
      {
        name: barLabel,
        type: 'bar',
        data: barData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#38BDF8' },
            { offset: 1, color: '#1677FF' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: 20
      },
      {
        name: '平均分',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: score.map((item) => item.avgScore),
        itemStyle: { color: '#10B981' },
        lineStyle: { width: 3 }
      }
    ]
  };
}

function renderChart() {
  if (!chartRef.value) return;
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartRef.value);
  }
  chartInstance.value.setOption(buildOption(), true);
}

function handleResize() {
  chartInstance.value?.resize();
}

watch(
  () => [props.trends, props.personalTrends, props.mode],
  () => nextTick(renderChart),
  { deep: true }
);

onMounted(() => {
  nextTick(renderChart);
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance.value?.dispose();
  chartInstance.value = null;
});
</script>

<style scoped lang="scss">
.learning-chart {
  width: 100%;
  height: v-bind(height);
}
</style>
