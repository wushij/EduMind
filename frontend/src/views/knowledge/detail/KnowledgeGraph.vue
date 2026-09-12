<template>
  <div class="knowledge-graph-page">
    <div class="graph-header">
      <h2>课程知识图谱</h2>
      <p>深度关系图谱（V1.0）</p>
      <el-slider v-model="depth" :min="1" :max="3" style="width: 160px" />
      <el-select v-model="relationTypes" multiple collapse-tags placeholder="关系类型" style="width: 220px">
        <el-option label="prerequisite" value="prerequisite" />
        <el-option label="related" value="related" />
        <el-option label="successor" value="successor" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadGraph">刷新图谱</el-button>
    </div>

    <div v-loading="loading" class="graph-layout">
      <div ref="chartRef" class="graph-chart"></div>
      <div class="side-panels">
        <el-card v-if="selectedNode" class="node-detail-card" shadow="never">
          <h3>节点详情</h3>
          <p><strong>名称：</strong>{{ selectedNode.label }}</p>
          <p><strong>类型：</strong>{{ selectedNode.type }}</p>
          <p v-if="selectedNode.refId"><strong>引用 ID：</strong>{{ selectedNode.refId }}</p>
        </el-card>
        <GraphGapPanel :gaps="gaps" />
        <GraphRelationEditor @saved="loadGraph" />
      </div>
    </div>
    <el-empty v-if="!loading && graphData.nodes.length === 0" description="暂无图谱数据，请先完成文档切片与知识点配置" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import { getGraphGaps, getKnowledgeGraph } from '@/api/knowledge/graph';
import GraphGapPanel from '@/components/knowledge/GraphGapPanel.vue';
import GraphRelationEditor from '@/components/knowledge/GraphRelationEditor.vue';
import type { GraphGapVO, KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';

const { kbId } = useKnowledgeRoute();
const authStore = useAuthStore();
const studentId = computed(() => authStore.currentUser?.id ?? 3);
const loading = ref(false);
const chartRef = ref<HTMLDivElement | null>(null);
const selectedNode = ref<KnowledgeGraphNode | null>(null);
const graphData = ref<KnowledgeGraphVO>({ nodes: [], edges: [] });
const gaps = ref<GraphGapVO[]>([]);
const depth = ref(2);
const relationTypes = ref<string[]>(['prerequisite', 'related']);
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
    const types = relationTypes.value.join(',');
    const res = await getKnowledgeGraph(kbId.value, depth.value, types);
    graphData.value = res?.data || { nodes: [], edges: [] };
    const gapRes = await getGraphGaps(kbId.value, studentId.value, 0.6);
    gaps.value = gapRes?.data || [];
    renderChart();
  } catch {
    graphData.value = { nodes: [], edges: [] };
    ElMessage.error('加载知识图谱失败');
  } finally {
    loading.value = false;
  }
}

watch(kbId, () => loadGraph());
watch([depth, relationTypes], () => loadGraph(), { deep: true });

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
    grid-template-columns: 1fr 320px;
    gap: 16px;
    min-height: 520px;
  }

  .side-panels {
    display: flex;
    flex-direction: column;
    gap: 12px;
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
