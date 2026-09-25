<template>
  <div class="radar-wrapper">
    <div v-if="!hasData" class="radar-empty">
      <el-empty description="暂无考点掌握度雷达数据" :image-size="70" />
    </div>
    <div v-else ref="chartRef" class="knowledge-radar" :style="{ height }" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick, computed } from 'vue';
import * as echarts from 'echarts';
import type { KnowledgeMasteryVO } from '@/types/analytics/mastery';

const props = withDefaults(
  defineProps<{
    data?: KnowledgeMasteryVO | null;
    height?: string;
  }>(),
  {
    data: null,
    height: '340px'
  }
);

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

const hasData = computed(() => {
  return (props.data?.dimensions?.length ?? 0) > 0;
});

function buildOption(): echarts.EChartsOption {
  const dimensions = props.data?.dimensions ?? [];
  const personal = props.data?.personal ?? [];
  const classAvg = props.data?.classAvg ?? [];
  const hasPersonal = personal.length > 0 && personal.some((v) => v > 0);

  const seriesData: any[] = [];

  if (hasPersonal) {
    seriesData.push({
      name: '当前学员表现',
      value: personal,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99, 102, 241, 0.45)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0.08)' }
        ])
      },
      lineStyle: { color: '#6366F1', width: 2.5 },
      itemStyle: { color: '#6366F1', borderColor: '#FFFFFF', borderWidth: 2 }
    });
  }

  seriesData.push({
    name: '全班掌握均分',
    value: classAvg.length ? classAvg : dimensions.map(() => 75),
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(16, 185, 129, 0.35)' },
        { offset: 1, color: 'rgba(16, 185, 129, 0.06)' }
      ])
    },
    lineStyle: { color: '#10B981', width: 2.2 },
    itemStyle: { color: '#10B981', borderColor: '#FFFFFF', borderWidth: 1.5 }
  });

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#1E293B', fontSize: 13 },
      extraCssText: 'box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08); border-radius: 8px;'
    },
    legend: {
      data: hasPersonal ? ['当前学员表现', '全班掌握均分'] : ['全班掌握均分'],
      bottom: 4,
      icon: 'roundRect',
      textStyle: { color: '#475569', fontSize: 12, fontWeight: 500 }
    },
    radar: {
      indicator: dimensions.map((name) => ({
        name: name.length > 8 ? name.slice(0, 7) + '...' : name,
        max: 100
      })),
      radius: '62%',
      center: ['50%', '48%'],
      splitNumber: 4,
      splitArea: {
        areaStyle: {
          color: ['rgba(248, 250, 252, 0.8)', 'rgba(255, 255, 255, 0.8)']
        }
      },
      splitLine: {
        lineStyle: { color: '#E2E8F0' }
      },
      axisLine: {
        lineStyle: { color: '#CBD5E1' }
      },
      axisName: {
        color: '#475569',
        fontSize: 11,
        fontWeight: 500
      }
    },
    series: [
      {
        type: 'radar',
        data: seriesData
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
.radar-wrapper {
  width: 100%;
  position: relative;
}

.radar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 320px;
}

.knowledge-radar {
  width: 100%;
}
</style>
