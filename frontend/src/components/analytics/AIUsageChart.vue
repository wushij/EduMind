<template>
  <div ref="chartRef" class="ai-usage-chart" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { AiUsageAnalyticsVO } from '@/types/analytics/learning';

const props = withDefaults(
  defineProps<{
    data?: AiUsageAnalyticsVO | null;
    height?: string;
  }>(),
  {
    data: null,
    height: '320px'
  }
);

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

function buildOption(): echarts.EChartsOption {
  const daily = props.data?.daily ?? [];
  const byProvider = props.data?.byProvider ?? [];
  const colors = ['#1677FF', '#06B6D4', '#10B981', '#722ED1', '#F59E0B'];

  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#64748B', fontSize: 12 } },
    grid: { left: '3%', right: '4%', top: '10%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: daily.map((item) => item.date),
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 }
    },
    series: [
      {
        name: '日调用量',
        type: 'bar',
        data: daily.map((item) => item.calls),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#38BDF8' },
            { offset: 1, color: '#1677FF' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: 22
      },
      {
        name: '供应商占比',
        type: 'pie',
        radius: ['42%', '58%'],
        center: ['78%', '38%'],
        data: byProvider.map((item, index) => ({
          name: item.provider,
          value: item.calls,
          itemStyle: { color: colors[index % colors.length] }
        })),
        label: { fontSize: 11 }
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
  () => props.data,
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
.ai-usage-chart {
  width: 100%;
  height: v-bind(height);
}
</style>
