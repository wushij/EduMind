<template>
  <div ref="chartRef" class="knowledge-radar" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { KnowledgeMasteryVO } from '@/types/analytics/mastery';

const props = withDefaults(
  defineProps<{
    data?: KnowledgeMasteryVO | null;
    height?: string;
  }>(),
  {
    data: null,
    height: '360px'
  }
);

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

function buildOption(): echarts.EChartsOption {
  const dimensions = props.data?.dimensions ?? [];
  const personal = props.data?.personal ?? [];
  const classAvg = props.data?.classAvg ?? [];

  return {
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1
    },
    legend: {
      data: ['个人掌握', '班级平均'],
      bottom: 0,
      textStyle: { color: '#64748B', fontSize: 12 }
    },
    radar: {
      indicator: dimensions.map((name) => ({ name, max: 100 })),
      radius: '62%',
      splitArea: { areaStyle: { color: ['#F8FAFC', '#FFFFFF'] } },
      axisName: { color: '#64748B', fontSize: 12 }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            name: '个人掌握',
            value: personal,
            areaStyle: { color: 'rgba(22, 119, 255, 0.18)' },
            lineStyle: { color: '#1677FF', width: 2 },
            itemStyle: { color: '#1677FF' }
          },
          {
            name: '班级平均',
            value: classAvg,
            areaStyle: { color: 'rgba(16, 185, 129, 0.12)' },
            lineStyle: { color: '#10B981', width: 2 },
            itemStyle: { color: '#10B981' }
          }
        ]
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
.knowledge-radar {
  width: 100%;
  height: v-bind(height);
}
</style>
