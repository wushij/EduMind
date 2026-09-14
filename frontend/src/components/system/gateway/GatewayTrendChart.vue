<template>
  <div ref="chartRef" class="gateway-trend-chart" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { TimeSeriesPointVO } from '@/types/system/gateway';

const props = withDefaults(
  defineProps<{
    data?: TimeSeriesPointVO[];
    height?: string;
  }>(),
  {
    data: () => [],
    height: '300px'
  }
);

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

function buildOption(): echarts.EChartsOption {
  const points = props.data || [];
  const times = points.map((p) => p.time);
  const calls = points.map((p) => p.calls);
  const tokens = points.map((p) => p.tokens);

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#F8FAFC', fontSize: 12 },
      formatter: (params: any) => {
        if (!Array.isArray(params) || params.length === 0) return '';
        const time = params[0].axisValueLabel;
        let html = `<div style="font-weight: 600; margin-bottom: 6px; color: #94A3B8;">${time}</div>`;
        for (const item of params) {
          const isCalls = item.seriesIndex === 0;
          const label = isCalls ? '网关请求量' : 'Token 算力消耗';
          const val = isCalls
            ? `${Number(item.value).toLocaleString()} 次`
            : item.value >= 10000
            ? `${(item.value / 10000).toFixed(1)} 万 Tokens`
            : `${Number(item.value).toLocaleString()} Tokens`;
          html += `
            <div style="display: flex; justify-content: space-between; gap: 16px; align-items: center; margin: 3px 0;">
              <span style="display: inline-flex; align-items: center; gap: 6px;">
                <span style="width: 8px; height: 8px; border-radius: 50%; background-color: ${item.color};"></span>
                <span>${label}</span>
              </span>
              <span style="font-weight: 700; font-family: monospace;">${val}</span>
            </div>
          `;
        }
        return html;
      }
    },
    legend: {
      top: 2,
      left: 'center', // 居中放置，与左右两边的 Y 轴名称彻底拉开间距，杜绝重叠
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 7,
      itemGap: 28,
      textStyle: { color: '#64748B', fontSize: 12 }
    },
    grid: {
      left: '3%',
      right: '4%',
      top: '42px', // 充足上间距，避免坐标轴标题与顶部重叠
      bottom: '6%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: times,
      boundaryGap: true,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '调用量 (次)',
        nameTextStyle: { color: '#94A3B8', fontSize: 11, align: 'left', padding: [0, 0, 6, 0] },
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: { color: '#94A3B8', fontSize: 11 }
      },
      {
        type: 'value',
        name: 'Token 消耗',
        nameTextStyle: { color: '#94A3B8', fontSize: 11, align: 'right', padding: [0, 0, 6, 0] },
        splitLine: { show: false },
        axisLabel: {
          color: '#94A3B8',
          fontSize: 11,
          formatter: (val: number) => (val >= 10000 ? `${(val / 10000).toFixed(0)}w` : String(val))
        }
      }
    ],
    series: [
      {
        name: '网关请求量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 6,
        yAxisIndex: 0,
        data: calls,
        itemStyle: { color: '#2563EB' },
        lineStyle: { width: 2.5, color: '#2563EB' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.28)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      },
      {
        name: 'Token 消耗',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 16,
        data: tokens,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#10B981' },
            { offset: 1, color: '#059669' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
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
.gateway-trend-chart {
  width: 100%;
  height: v-bind(height);
}
</style>
