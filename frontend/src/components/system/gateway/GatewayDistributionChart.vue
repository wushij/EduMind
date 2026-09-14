<template>
  <div class="distribution-wrapper">
    <div class="distribution-header">
      <div class="tab-pill-group">
        <button
          type="button"
          class="tab-pill"
          :class="{ active: activeMode === 'provider' }"
          @click="activeMode = 'provider'"
        >
          模型与供应商
        </button>
        <button
          type="button"
          class="tab-pill"
          :class="{ active: activeMode === 'scene' }"
          @click="activeMode = 'scene'"
        >
          业务教学场景
        </button>
      </div>
    </div>
    <div ref="chartRef" class="gateway-distribution-chart" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { GatewayProviderMetricVO, GatewaySceneMetricVO } from '@/types/system/gateway';

const props = withDefaults(
  defineProps<{
    providers?: GatewayProviderMetricVO[];
    scenes?: GatewaySceneMetricVO[];
    height?: string;
  }>(),
  {
    providers: () => [],
    scenes: () => [],
    height: '280px'
  }
);

const activeMode = ref<'provider' | 'scene'>('provider');
const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<echarts.ECharts | null>(null);

const colors = ['#2563EB', '#06B6D4', '#10B981', '#F59E0B', '#8B5CF6', '#EC4899', '#6366F1', '#14B8A6'];

function formatSceneName(scene: string, sceneName?: string) {
  if (sceneName && sceneName.trim() && sceneName !== scene) return sceneName;
  if (!scene) return '通用教学场景';
  const lower = scene.toLowerCase();
  if (lower === 'chat') return '课程智能助教答疑';
  if (lower.includes('global_assis')) return '全局教学助手';
  if (lower.includes('rag') || lower.includes('chat_rag') || lower === 'knowledge') return 'RAG 知识检索增强';
  if (lower === 'question_generate' || lower === 'question') return 'AI 题库出题与变式';
  if (lower === 'grading') return '作业/主观题智能批改';
  if (lower === 'agent') return 'Agent 多步任务规划';
  if (lower === 'exam') return '智能组卷与试题分析';
  if (lower === 'stream') return '流式启发式对话';
  return scene;
}

function buildOption(): echarts.EChartsOption {
  let chartData: { name: string; fullName: string; value: number; tokens: number }[] = [];

  if (activeMode.value === 'provider') {
    const list = props.providers || [];
    chartData = list.map((item) => ({
      name: item.provider,
      fullName: item.provider,
      value: item.calls,
      tokens: item.tokens
    }));
  } else {
    const list = props.scenes || [];
    chartData = list.map((item) => {
      const friendlyName = formatSceneName(item.scene, item.sceneName);
      return {
        name: friendlyName,
        fullName: friendlyName,
        value: item.calls,
        tokens: item.tokens
      };
    });
  }

  const totalCalls = chartData.reduce((sum, item) => sum + item.value, 0);

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#F8FAFC', fontSize: 12 },
      formatter: (params: any) => {
        const item = params.data;
        const percent = params.percent ?? 0;
        const tokenStr =
          item.tokens >= 10000
            ? `${(item.tokens / 10000).toFixed(1)} 万`
            : Number(item.tokens).toLocaleString();
        return `
          <div style="font-weight: 700; margin-bottom: 5px; color: #FFFFFF; font-size: 13px;">${item.fullName}</div>
          <div style="font-size: 11.5px; color: #94A3B8; margin-bottom: 3px;">网关调用量：<b style="color: #F8FAFC;">${Number(item.value).toLocaleString()}</b> 次 (${percent}%)</div>
          <div style="font-size: 11.5px; color: #94A3B8;">算力消耗：<b style="color: #38BDF8;">${tokenStr}</b> Tokens</div>
        `;
      }
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: '2%',
      top: 'middle',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 8,
      pageIconSize: 10,
      pageTextStyle: { color: '#94A3B8', fontSize: 10 },
      textStyle: { color: '#334155', fontSize: 11.5 },
      formatter: (name: string) => {
        const target = chartData.find((d) => d.name === name);
        if (!target) return name;
        const pct = totalCalls > 0 ? ((target.value / totalCalls) * 100).toFixed(1) : '0';
        // 完整展示名称，不暴力截断为 deepseek..
        return `${name}  ${pct}%`;
      }
    },
    series: [
      {
        name: activeMode.value === 'provider' ? '供应商占比' : '场景占比',
        type: 'pie',
        radius: ['38%', '60%'],
        center: ['26%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#FFFFFF',
          borderWidth: 2
        },
        // 彻底关闭切片上的小引线文字，防止在小环形图上重叠截断成 Flash·de...
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 5,
          label: { show: false },
          labelLine: { show: false }
        },
        data: chartData.map((item, idx) => ({
          name: item.name,
          fullName: item.fullName,
          value: item.value,
          tokens: item.tokens,
          itemStyle: { color: colors[idx % colors.length] }
        }))
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

watch([() => props.providers, () => props.scenes, activeMode], () => nextTick(renderChart), {
  deep: true
});

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
.distribution-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;

  .distribution-header {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 6px;

    .tab-pill-group {
      display: inline-flex;
      background: #F1F5F9;
      border-radius: 9999px;
      padding: 3px;
      gap: 2px;

      .tab-pill {
        border: none;
        background: transparent;
        padding: 4px 12px;
        border-radius: 9999px;
        font-size: 11.5px;
        font-weight: 500;
        color: #64748B;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          color: #1E293B;
        }

        &.active {
          background: #FFFFFF;
          color: #2563EB;
          font-weight: 600;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }
      }
    }
  }

  .gateway-distribution-chart {
    flex: 1;
    width: 100%;
    min-height: v-bind(height);
  }
}
</style>
