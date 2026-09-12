<template>
  <div class="paper-distribution-charts">
    <div class="chart-row">
      <!-- 知识点覆盖率环形进度/仪表 -->
      <div class="chart-item">
        <div class="chart-title">知识点覆盖率</div>
        <div ref="gaugeRef" class="chart-box" />
        <div class="chart-meta">
          <span>覆盖知识点：<strong>{{ preview?.distinctKnowledgePointCount || 0 }}</strong> 个</span>
        </div>
      </div>

      <!-- 难度分布柱状图 -->
      <div class="chart-item">
        <div class="chart-title">难度正态分布</div>
        <div ref="diffRef" class="chart-box" />
        <div class="chart-meta">
          <span>总题量：<strong>{{ preview?.selectedCount || 0 }}</strong> 题</span>
        </div>
      </div>

      <!-- 题型分布饼图 -->
      <div class="chart-item">
        <div class="chart-title">题型比例</div>
        <div ref="typeRef" class="chart-box" />
        <div class="chart-meta">
          <span>卷面总分：<strong>{{ preview?.totalScore || 100 }}</strong> 分</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { SmartPaperComposeVO } from '@/types/ai/paper-compose';

const props = defineProps<{
  preview?: SmartPaperComposeVO | null;
}>();

const gaugeRef = ref<HTMLElement | null>(null);
const diffRef = ref<HTMLElement | null>(null);
const typeRef = ref<HTMLElement | null>(null);

const gaugeChart = shallowRef<echarts.ECharts | null>(null);
const diffChart = shallowRef<echarts.ECharts | null>(null);
const typeChart = shallowRef<echarts.ECharts | null>(null);

function renderCharts() {
  if (!props.preview) return;

  // 1. Coverage Gauge
  if (gaugeRef.value) {
    if (!gaugeChart.value) gaugeChart.value = echarts.init(gaugeRef.value);
    const rate = Math.round((props.preview.coverageRate || 0) * 100);
    gaugeChart.value.setOption({
      series: [
        {
          type: 'gauge',
          startAngle: 200,
          endAngle: -20,
          min: 0,
          max: 100,
          splitNumber: 5,
          itemStyle: {
            color: rate >= 80 ? '#10B981' : rate >= 60 ? '#1677FF' : '#F59E0B'
          },
          progress: { show: true, width: 14, roundCap: true },
          pointer: { show: false },
          axisLine: { lineStyle: { width: 14, color: [[1, '#E2E8F0']] } },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { distance: 18, color: '#94A3B8', fontSize: 10 },
          title: { show: false },
          detail: {
            valueAnimation: true,
            fontSize: 22,
            fontWeight: 700,
            offsetCenter: [0, '10%'],
            formatter: '{value}%',
            color: '#1E293B'
          },
          data: [{ value: rate }]
        }
      ]
    });
  }

  // 2. Difficulty Bar Chart
  if (diffRef.value) {
    if (!diffChart.value) diffChart.value = echarts.init(diffRef.value);
    const hist = props.preview.difficultyHistogram || {};
    const easy = hist['EASY'] || 0;
    const medium = hist['MEDIUM'] || 0;
    const hard = hist['HARD'] || 0;

    diffChart.value.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 15, right: 15, bottom: 25, left: 35 },
      xAxis: {
        type: 'category',
        data: ['简单 (Easy)', '中等 (Medium)', '较难 (Hard)'],
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisLabel: { color: '#64748B', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#F1F5F9' } },
        axisLabel: { color: '#94A3B8', fontSize: 10 }
      },
      series: [
        {
          name: '题量',
          type: 'bar',
          barWidth: 26,
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: (params: any) => {
              const colors = ['#10B981', '#3B82F6', '#EF4444'];
              return colors[params.dataIndex] || '#3B82F6';
            }
          },
          data: [easy, medium, hard]
        }
      ]
    });
  }

  // 3. Question Type Doughnut Chart
  if (typeRef.value) {
    if (!typeChart.value) typeChart.value = echarts.init(typeRef.value);
    const typeDist = props.preview.typeDistribution || {};
    const data: { name: string; value: number }[] = [];
    const typeLabelMap: Record<string, string> = {
      SINGLE_CHOICE: '单选题',
      MULTI_CHOICE: '多选题',
      JUDGE: '判断题',
      BLANK: '填空题',
      SHORT_ANSWER: '简答题',
      COMPREHENSIVE: '综合分析'
    };
    for (const [k, v] of Object.entries(typeDist)) {
      if (v > 0) {
        data.push({ name: typeLabelMap[k] || k, value: v });
      }
    }
    if (data.length === 0) {
      data.push({ name: '暂无题型分布', value: 0 });
    }

    typeChart.value.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 题 ({d}%)' },
      legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#64748B' } },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data
        }
      ]
    });
  }
}

function resizeAll() {
  gaugeChart.value?.resize();
  diffChart.value?.resize();
  typeChart.value?.resize();
}

watch(
  () => props.preview,
  () => {
    nextTick(() => renderCharts());
  },
  { deep: true }
);

onMounted(() => {
  nextTick(() => renderCharts());
  window.addEventListener('resize', resizeAll);
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll);
  gaugeChart.value?.dispose();
  diffChart.value?.dispose();
  typeChart.value?.dispose();
});
</script>

<style scoped lang="scss">
.paper-distribution-charts {
  margin-top: 16px;
  padding: 16px;
  background: #F8FAFC;
  border-radius: 12px;
  border: 1px solid #E2E8F0;

  .chart-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }

  .chart-item {
    background: #FFFFFF;
    border-radius: 8px;
    padding: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    align-items: center;

    .chart-title {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      margin-bottom: 6px;
      align-self: flex-start;
    }

    .chart-box {
      width: 100%;
      height: 180px;
    }

    .chart-meta {
      margin-top: 6px;
      font-size: 12px;
      color: #64748B;
      strong {
        color: #0F172A;
      }
    }
  }
}
</style>
