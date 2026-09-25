<template>
  <div class="overview-charts-grid">
    <!-- 图表 1：四大错因类型构成全景图 -->
    <div class="chart-card">
      <div class="card-header">
        <div class="header-left">
          <div class="card-icon-tag card-icon-tag--pie">
            <svg viewBox="0 0 24 24" class="tag-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21.21 15.89A10 10 0 1 1 8 2.83" />
              <path d="M22 12A10 10 0 0 0 12 2v10z" />
            </svg>
          </div>
          <div class="header-title-wrap">
            <h3 class="card-title">班级失分根因构成</h3>
            <span class="card-subtitle">按认知偏差、运算推理与审题漏洞多维分布</span>
          </div>
        </div>

        <div class="header-legend">
          <span class="legend-badge legend-badge--concept">概念偏差</span>
          <span class="legend-badge legend-badge--calc">计算失误</span>
          <span class="legend-badge legend-badge--logic">逻辑漏洞</span>
          <span class="legend-badge legend-badge--reading">审题偏差</span>
        </div>
      </div>

      <div class="chart-body">
        <div ref="errorTypeChartRef" class="echarts-container" />
      </div>
    </div>

    <!-- 图表 2：易错薄弱考点排行 TOP 5 -->
    <div class="chart-card">
      <div class="card-header">
        <div class="header-left">
          <div class="card-icon-tag card-icon-tag--bar">
            <svg viewBox="0 0 24 24" class="tag-svg" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
          </div>
          <div class="header-title-wrap">
            <h3 class="card-title">易错考点预警排行</h3>
            <span class="card-subtitle">失分频次最高的核心考点与知识断层分布</span>
          </div>
        </div>

        <span class="warning-alert-pill">
          <span class="alert-dot" />
          <span>重点突破</span>
        </span>
      </div>

      <div class="chart-body">
        <div v-if="topWeakKnowledgePoints.length > 0" ref="weakKpChartRef" class="echarts-container" />
        <div v-else class="chart-empty-state">
          <svg viewBox="0 0 24 24" class="empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M8 12h8" />
          </svg>
          <span>当前未检测到显著易错考点断层</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import type { WeakKpSummaryVO } from '@/types/analytics/mastery';

const props = defineProps<{
  errorTypeDistribution?: Record<string, number>;
  topWeakKnowledgePoints: WeakKpSummaryVO[];
}>();

const errorTypeChartRef = ref<HTMLDivElement | null>(null);
const weakKpChartRef = ref<HTMLDivElement | null>(null);

let errorTypeChart: echarts.ECharts | null = null;
let weakKpChart: echarts.ECharts | null = null;

function renderErrorTypeChart() {
  if (!errorTypeChartRef.value) return;
  if (!errorTypeChart) {
    errorTypeChart = echarts.init(errorTypeChartRef.value);
  }

  const dist = props.errorTypeDistribution || {
    CONCEPT: 0,
    CALC: 0,
    LOGIC: 0,
    READING: 0
  };

  const chartData = [
    { name: '概念偏差', value: dist.CONCEPT || 0, itemStyle: { color: '#3B82F6' } },
    { name: '计算失误', value: dist.CALC || 0, itemStyle: { color: '#F59E0B' } },
    { name: '逻辑漏洞', value: dist.LOGIC || 0, itemStyle: { color: '#8B5CF6' } },
    { name: '审题偏差', value: dist.READING || 0, itemStyle: { color: '#EC4899' } }
  ];

  const total = chartData.reduce((acc, cur) => acc + cur.value, 0);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      textStyle: { color: '#1E293B', fontSize: 13 },
      formatter: (params: any) => {
        const percent = total > 0 ? ((params.value / total) * 100).toFixed(1) : '0';
        return `
          <div style="font-weight:600;margin-bottom:4px">${params.name}</div>
          <div style="color:#64748B">失分人次：<strong style="color:#0F172A">${params.value}</strong></div>
          <div style="color:#64748B">全班占比：<strong style="color:#2563EB">${percent}%</strong></div>
        `;
      }
    },
    series: [
      {
        name: '失分根因',
        type: 'pie',
        radius: ['52%', '76%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          scale: true,
          scaleSize: 6,
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            formatter: (params: any) => `${params.name}\n${params.value} 人次`
          }
        },
        data: chartData
      }
    ]
  };

  errorTypeChart.setOption(option);
}

function cleanKpName(name?: string): string {
  if (!name) return '核心考点';
  return name.replace(/#/g, '').replace(/\s+/g, ' ').trim();
}

function renderWeakKpChart() {
  if (!weakKpChartRef.value) return;
  if (!weakKpChart) {
    weakKpChart = echarts.init(weakKpChartRef.value);
  }

  const list = [...props.topWeakKnowledgePoints].reverse();
  const yData = list.map((item) => cleanKpName(item.knowledgePointName));
  const xData = list.map((item) => item.wrongCount);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      textStyle: { color: '#1E293B', fontSize: 13 },
      formatter: (params: any) => {
        const item = list[params[0].dataIndex];
        const displayName = cleanKpName(item.knowledgePointName);
        return `
          <div style="font-weight:600;margin-bottom:4px">${displayName}</div>
          <div style="color:#64748B">做错学生：<strong style="color:#EF4444">${item.wrongCount} 人</strong></div>
          <div style="color:#64748B">考点错误率：<strong style="color:#F59E0B">${item.errorRate ?? 0}%</strong></div>
        `;
      }
    },
    grid: {
      left: '3%',
      right: '6%',
      bottom: '3%',
      top: '6%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: yData,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#475569',
        fontSize: 12,
        formatter: (val: string) => {
          return val.length > 8 ? `${val.substring(0, 8)}...` : val;
        }
      }
    },
    series: [
      {
        name: '失分人次',
        type: 'bar',
        barWidth: 16,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#F87171' },
            { offset: 1, color: '#EF4444' }
          ])
        },
        data: xData
      }
    ]
  };

  weakKpChart.setOption(option);
}

function handleResize() {
  errorTypeChart?.resize();
  weakKpChart?.resize();
}

watch(
  () => [props.errorTypeDistribution, props.topWeakKnowledgePoints],
  () => {
    nextTick(() => {
      renderErrorTypeChart();
      renderWeakKpChart();
    });
  },
  { deep: true }
);

onMounted(() => {
  nextTick(() => {
    renderErrorTypeChart();
    renderWeakKpChart();
    window.addEventListener('resize', handleResize);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  errorTypeChart?.dispose();
  weakKpChart?.dispose();
});
</script>

<style scoped lang="scss">
.overview-charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 18px;
  width: 100%;

  .chart-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
    padding: 20px 22px 16px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    transition: all 0.25s ease;

    &:hover {
      box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
      border-color: #cbd5e1;
    }

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 10px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .card-icon-tag {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;

        .tag-svg {
          width: 18px;
          height: 18px;
        }

        &--pie {
          background: #eff6ff;
          color: #2563eb;
        }

        &--bar {
          background: #fef2f2;
          color: #dc2626;
        }
      }

      .header-title-wrap {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .card-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
        }

        .card-subtitle {
          font-size: 12px;
          color: #64748b;
        }
      }

      .header-legend {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;

        .legend-badge {
          font-size: 11px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 6px;

          &--concept { background: #eff6ff; color: #2563eb; }
          &--calc { background: #fffbeb; color: #d97706; }
          &--logic { background: #f5f3ff; color: #7c3aed; }
          &--reading { background: #fdf2f8; color: #db2777; }
        }
      }

      .warning-alert-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        font-size: 12px;
        font-weight: 600;
        padding: 3px 10px;
        border-radius: 12px;
        background: #fef2f2;
        color: #ef4444;
        border: 1px solid #fee2e2;

        .alert-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #ef4444;
        }
      }
    }

    .chart-body {
      position: relative;
      width: 100%;
      height: 220px;

      .echarts-container {
        width: 100%;
        height: 100%;
      }

      .chart-empty-state {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 10px;
        color: #94a3b8;
        font-size: 13px;

        .empty-svg {
          width: 36px;
          height: 36px;
          color: #cbd5e1;
        }
      }
    }
  }
}
</style>
