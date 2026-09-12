<template>
  <div class="knowledge-graph-page">
    <div class="graph-header">
      <h2>课程知识图谱</h2>
      <p>基于知识点与文档切片大纲构建的关联网络（V0.5 MVP）</p>
      <el-button type="primary" :loading="loading" @click="loadGraph">刷新图谱</el-button>
    </div>

    <div v-loading="loading" class="graph-layout">
      <div ref="chartRef" class="graph-chart"></div>
      <el-card v-if="selectedNode" class="node-detail-card" shadow="never">
        <h3>节点详情</h3>
        <p><strong>名称：</strong>{{ selectedNode.label }}</p>
        <p><strong>类型：</strong>{{ selectedNode.type }}</p>
        <p v-if="selectedNode.refId"><strong>引用 ID：</strong>{{ selectedNode.refId }}</p>
      </el-card>
    </div>
    <el-empty v-if="!loading && graphData.nodes.length === 0" description="暂无图谱数据，请先完成文档切片与知识点配置" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import { getKnowledgeGraph } from '@/api/knowledge/graph';
import type { KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';

const { kbId } = useKnowledgeRoute();
const loading = ref(false);
const chartRef = ref<HTMLDivElement | null>(null);
const selectedNode = ref<KnowledgeGraphNode | null>(null);
const graphData = ref<KnowledgeGraphVO>({ nodes: [], edges: [] });
let chart: echarts.ECharts | null = null;

function renderChart() {
  if (!chartRef.value) return;
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const nodes = graphData.value.nodes.map((node) => ({
    id: node.id,
    name: node.label,
    symbolSize: node.type === 'CHAPTER' ? 42 : 28,
    category: node.type,
    raw: node
  }));
  const links = graphData.value.edges.map((edge) => ({
    source: edge.source,
    target: edge.target,
    value: edge.relation
  }));
  chart.setOption({
    tooltip: {},
    legend: [{ data: ['KNOWLEDGE_POINT', 'CHAPTER', 'CHUNK'] }],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      label: { show: true, position: 'right', fontSize: 11 },
      force: { repulsion: 120, edgeLength: 80 },
      categories: [
        { name: 'KNOWLEDGE_POINT' },
        { name: 'CHAPTER' },
        { name: 'CHUNK' }
      ],
      data: nodes,
      links
    }]
  });
  chart.off('click');
  chart.on('click', (params: any) => {
    selectedNode.value = params?.data?.raw || null;
  });
}

async function loadGraph() {
  if (!kbId.value) return;
  loading.value = true;
  try {
    const res = await getKnowledgeGraph(kbId.value);
    graphData.value = res?.data || { nodes: [], edges: [] };
    renderChart();
  } catch {
    graphData.value = { nodes: [], edges: [] };
    ElMessage.error('加载知识图谱失败');
  } finally {
    loading.value = false;
  }
}

watch(kbId, () => loadGraph());

onMounted(() => {
  loadGraph();
  window.addEventListener('resize', () => chart?.resize());
});

onBeforeUnmount(() => {
  chart?.dispose();
  chart = null;
});
</script>

<style scoped lang="scss">
.knowledge-graph-page {
  padding: 24px;

  .graph-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    h2 {
      margin: 0;
    }

    p {
      margin: 0;
      color: #64748b;
      flex: 1;
    }
  }

  .graph-layout {
    display: grid;
    grid-template-columns: 1fr 280px;
    gap: 16px;
    min-height: 520px;
  }

  .graph-chart {
    height: 520px;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
  }

  .node-detail-card {
    border-radius: 12px;
  }
}
</style>
