<template>
  <div ref="containerRef" class="knowledge-graph-g6" />
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { Graph } from '@antv/g6';
import type { KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';

const props = defineProps<{
  graphData: KnowledgeGraphVO;
}>();

const emit = defineEmits<{
  (e: 'node-click', node: KnowledgeGraphNode | null): void;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
let graph: Graph | null = null;

const NODE_COLORS: Record<string, string> = {
  CHAPTER: '#1677FF',
  KNOWLEDGE_POINT: '#52C41A',
  CHUNK: '#FAAD14'
};

function toG6Data(data: KnowledgeGraphVO) {
  const nodes = data.nodes.map((node) => ({
    id: node.id,
    data: {
      label: node.label,
      type: node.type,
      refId: node.refId,
      raw: node
    },
    style: {
      fill: NODE_COLORS[node.type] || '#722ED1',
      size: node.type === 'CHAPTER' ? 36 : 28
    }
  }));

  const edges = data.edges.map((edge, index) => ({
    id: `edge-${index}`,
    source: edge.source,
    target: edge.target,
    data: { relation: edge.relation }
  }));

  return { nodes, edges };
}

function renderGraph() {
  if (!containerRef.value) return;

  const g6Data = toG6Data(props.graphData);

  if (!graph) {
    graph = new Graph({
      container: containerRef.value,
      width: containerRef.value.clientWidth || 800,
      height: 520,
      data: g6Data,
      layout: {
        type: 'd3-force',
        link: { distance: 100 },
        collide: { radius: 40 }
      },
      node: {
        style: {
          labelText: (d: { data?: { label?: string } }) => d.data?.label || '',
          labelFontSize: 11,
          labelFill: '#334155'
        }
      },
      edge: {
        style: {
          stroke: '#94A3B8',
          endArrow: true
        }
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element']
    });

    graph.on('node:click', (event: unknown) => {
      const nodeId = (event as { target?: { id?: string } })?.target?.id;
      const raw = props.graphData.nodes.find((n) => n.id === nodeId) || null;
      emit('node-click', raw);
    });
  } else {
    graph.setData(g6Data);
  }

  graph.render();
}

function handleResize() {
  if (!graph || !containerRef.value) return;
  graph.setSize(containerRef.value.clientWidth, 520);
}

watch(
  () => props.graphData,
  () => renderGraph(),
  { deep: true }
);

onMounted(() => {
  renderGraph();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  graph?.destroy();
  graph = null;
});
</script>

<style scoped lang="scss">
.knowledge-graph-g6 {
  width: 100%;
  height: 520px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
</style>
