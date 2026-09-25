<template>
  <div class="ai-usage-charts-grid">
    <!-- 左侧：调用量与 Token 消耗双轴趋势图 -->
    <div class="chart-panel trend-panel">
      <div class="panel-header">
        <div class="header-left">
          <h3 class="panel-title">调用频度与 Token 消耗趋势</h3>
          <span class="panel-subtitle">按时间维度统计调用总量与算力消耗动态</span>
        </div>
        <div class="header-legend-tags">
          <span class="legend-tag legend-tag--calls">
            <span class="legend-dot" /> 调用量 (次)
          </span>
          <span class="legend-tag legend-tag--tokens">
            <span class="legend-dot" /> Token 消耗
          </span>
        </div>
      </div>

      <div ref="trendChartRef" class="chart-canvas" />
      <div v-if="!hasTrendData" class="chart-empty-mask">
        <el-empty description="暂无当前时间窗口内的调用趋势数据" :image-size="70" />
      </div>
    </div>

    <!-- 右侧：教育场景与模型分布环形图 -->
    <div class="chart-panel donut-panel">
      <div class="panel-header">
        <div class="header-left">
          <h3 class="panel-title">智能应用与模型分布</h3>
          <span class="panel-subtitle">业务场景占比与大模型供应商分布</span>
        </div>
        <div class="distribution-toggle">
          <button
            type="button"
            class="toggle-btn"
            :class="{ active: distType === 'scene' }"
            @click="distType = 'scene'"
          >
            业务场景
          </button>
          <button
            type="button"
            class="toggle-btn"
            :class="{ active: distType === 'model' }"
            @click="distType = 'model'"
          >
            模型厂商
          </button>
        </div>
      </div>

      <div ref="donutChartRef" class="chart-canvas" />
      <div v-if="!hasDonutData" class="chart-empty-mask">
        <el-empty description="暂无场景或模型分布数据" :image-size="70" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { AiUsageAnalyticsVO } from '@/types/analytics/learning';

const props = withDefaults(
  defineProps<{
    data?: AiUsageAnalyticsVO | null;
  }>(),
  {
    data: null
  }
);

const distType = ref<'scene' | 'model'>('scene');

const trendChartRef = ref<HTMLElement | null>(null);
const donutChartRef = ref<HTMLElement | null>(null);

const trendChart = shallowRef<echarts.ECharts | null>(null);
const donutChart = shallowRef<echarts.ECharts | null>(null);

const hasTrendData = computed(() => {
  return (props.data?.daily ?? []).length > 0;
});

const hasDonutData = computed(() => {
  if (distType.value === 'scene') {
    return (props.data?.byScene ?? []).length > 0;
  }
  return (props.data?.byProvider ?? []).length > 0;
});

function formatTokens(val: number): string {
  if (val >= 10000) {
    return `${(val / 10000).toFixed(1)}万`;
  }
  return val.toLocaleString();
}

function renderTrendChart() {
  if (!trendChartRef.value) return;
  if (!trendChart.value) {
    trendChart.value = echarts.init(trendChartRef.value);
  }

  const daily = props.data?.daily ?? [];
  const dates = daily.map((d) => d.date);
  const calls = daily.map((d) => d.calls);
  const tokens = daily.map((d) => d.tokens);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b', fontSize: 13 },
      shadowColor: 'rgba(0, 0, 0, 0.08)',
      shadowBlur: 10,
      formatter: (params: any) => {
        if (!Array.isArray(params) || params.length === 0) return '';
        const date = params[0].axisValue;
        let html = `<div style="font-weight:600;margin-bottom:6px;color:#0f172a">${date}</div>`;
        for (const item of params) {
          const val = item.seriesName.includes('Token')
            ? formatTokens(item.value)
            : `${item.value.toLocaleString()} 次`;
          html += `<div style="display:flex;align-items:center;justify-content:space-between;gap:16px;margin:3px 0">
            <span style="display:inline-flex;align-items:center;gap:6px">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${item.color}"></span>
              <span style="color:#64748b">${item.seriesName}</span>
            </span>
            <strong style="color:#0f172a">${val}</strong>
          </div>`;
        }
        return html;
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      top: '12%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11.5 }
    },
    yAxis: [
      {
        type: 'value',
        name: '调用次数',
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 0, 0, 10] },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { color: '#64748b', fontSize: 11 }
      },
      {
        type: 'value',
        name: 'Token 消耗',
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 10, 0, 0] },
        splitLine: { show: false },
        axisLabel: {
          color: '#64748b',
          fontSize: 11,
          formatter: (v: number) => formatTokens(v)
        }
      }
    ],
    series: [
      {
        name: '调用量',
        type: 'bar',
        data: calls,
        yAxisIndex: 0,
        barMaxWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#38bdf8' },
            { offset: 1, color: '#2563eb' }
          ]),
          borderRadius: [6, 6, 0, 0]
        }
      },
      {
        name: 'Token 消耗',
        type: 'line',
        data: tokens,
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        itemStyle: { color: '#10b981' },
        lineStyle: { width: 2.5, color: '#10b981' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.28)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
          ])
        }
      }
    ]
  };

  trendChart.value.setOption(option, true);
}

function renderDonutChart() {
  if (!donutChartRef.value) return;
  if (!donutChart.value) {
    donutChart.value = echarts.init(donutChartRef.value);
  }

  const palette = [
    '#3b82f6',
    '#10b981',
    '#8b5cf6',
    '#f59e0b',
    '#06b6d4',
    '#ec4899',
    '#6366f1'
  ];

  let chartData: { name: string; value: number }[] = [];

  if (distType.value === 'scene') {
    const scenes = props.data?.byScene ?? [];
    chartData = scenes.map((s) => ({
      name: s.scene,
      value: s.calls
    }));
  } else {
    const providers = props.data?.byProvider ?? [];
    chartData = providers.map((p) => ({
      name: p.provider,
      value: p.calls
    }));
  }

  const total = chartData.reduce((acc, curr) => acc + curr.value, 0);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b', fontSize: 13 },
      shadowColor: 'rgba(0, 0, 0, 0.08)',
      shadowBlur: 10,
      formatter: (params: any) => {
        const percent = total > 0 ? ((params.value / total) * 100).toFixed(1) : 0;
        return `<div style="font-weight:600;margin-bottom:4px;color:#0f172a">${params.name}</div>
          <div style="color:#64748b;font-size:12px">调用次数: <strong style="color:#0f172a">${params.value.toLocaleString()}</strong> 次 (${percent}%)</div>`;
      }
    },
    legend: {
      bottom: '2%',
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      icon: 'circle',
      textStyle: { color: '#64748b', fontSize: 12 }
    },
    series: [
      {
        name: distType.value === 'scene' ? '场景分布' : '模型分布',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#ffffff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          scale: true,
          scaleSize: 8,
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 700,
            formatter: '{b}\n{d}%'
          }
        },
        data: chartData.map((item, idx) => ({
          ...item,
          itemStyle: { color: palette[idx % palette.length] }
        }))
      }
    ]
  };

  donutChart.value.setOption(option, true);
}

function handleResize() {
  trendChart.value?.resize();
  donutChart.value?.resize();
}

watch(
  () => props.data,
  () => {
    nextTick(() => {
      renderTrendChart();
      renderDonutChart();
    });
  },
  { deep: true }
);

watch(distType, () => {
  nextTick(renderDonutChart);
});

onMounted(() => {
  nextTick(() => {
    renderTrendChart();
    renderDonutChart();
  });
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  trendChart.value?.dispose();
  donutChart.value?.dispose();
  trendChart.value = null;
  donutChart.value = null;
});
</script>

<style scoped lang="scss">
.ai-usage-charts-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  width: 100%;
}

.chart-panel {
  position: relative;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.03);
  padding: 20px 22px;
  display: flex;
  flex-direction: column;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 12px;
  }

  .panel-title {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: -0.01em;
  }

  .panel-subtitle {
    font-size: 12.5px;
    color: #94a3b8;
  }

  .chart-canvas {
    width: 100%;
    height: 320px;
  }

  .chart-empty-mask {
    position: absolute;
    inset: 60px 0 0 0;
    background: rgba(255, 255, 255, 0.85);
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(2px);
  }
}

.trend-panel {
  .header-legend-tags {
    display: flex;
    align-items: center;
    gap: 12px;

    .legend-tag {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      color: #64748b;
      font-weight: 500;

      .legend-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
      }

      &--calls .legend-dot {
        background: #2563eb;
      }

      &--tokens .legend-dot {
        background: #10b981;
      }
    }
  }
}

.donut-panel {
  .distribution-toggle {
    display: flex;
    background: #f1f5f9;
    padding: 3px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;

    .toggle-btn {
      border: none;
      background: transparent;
      padding: 4px 14px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 500;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: #0f172a;
      }

      &.active {
        background: #ffffff;
        color: #2563eb;
        font-weight: 600;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
      }
    }
  }
}

@media (max-width: 1080px) {
  .ai-usage-charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
