<template>
  <div class="knowledge-graph-shell" :class="{ 'is-borderless': !bordered }">
    <div v-if="showToolbar" class="graph-toolbar">
      <button type="button" class="graph-tool-btn" title="放大" @click="zoomIn">＋</button>
      <button type="button" class="graph-tool-btn" title="缩小" @click="zoomOut">－</button>
      <button type="button" class="graph-tool-btn graph-tool-btn--wide" title="适应画布" @click="fitView">
        适应画布
      </button>
    </div>
    <div ref="containerRef" class="knowledge-graph-g6" :style="{ height: `${height}px` }" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { Graph } from '@antv/g6';
import type { KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';

const props = withDefaults(
  defineProps<{
    graphData: KnowledgeGraphVO;
    /** 画布高度，默认 520（学习路径页会传入更高的值） */
    height?: number;
    /** 是否展示缩放工具条 */
    showToolbar?: boolean;
    /** 是否自带卡片边框：嵌在已有卡片内（如学习路径面板）时传 false，避免卡中卡 */
    bordered?: boolean;
  }>(),
  {
    height: 520,
    showToolbar: false,
    bordered: true
  }
);

const emit = defineEmits<{
  (e: 'node-click', node: KnowledgeGraphNode | null): void;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
let graph: Graph | null = null;
let fitTimer: ReturnType<typeof setTimeout> | null = null;

/** 节点类型兜底色：纯知识图谱场景（无掌握度数据）使用 */
const TYPE_COLORS: Record<string, string> = {
  CHAPTER: '#2563eb',
  KNOWLEDGE_POINT: '#10b981',
  CHUNK: '#f59e0b'
};

/**
 * 掌握状态色：与学习路径面板图例「薄弱 / 学习中 / 已掌握」严格一一对应。
 * 采用更温润典雅的珊瑚绯红、琥珀暖橙与清新翡翠绿，杜绝刺眼纯红。
 */
const STATUS_COLORS: Record<string, string> = {
  WEAK: '#f43f5e',
  LEARNING: '#f59e0b',
  MASTERED: '#10b981'
};

function resolveFill(node: KnowledgeGraphNode): string {
  if (node.status && STATUS_COLORS[node.status]) {
    return STATUS_COLORS[node.status];
  }
  return TYPE_COLORS[node.type] || '#8b5cf6';
}

function resolveSize(node: KnowledgeGraphNode): number {
  const base = node.type === 'CHAPTER' ? 36 : 28;
  return node.highlight ? base + 12 : base;
}

/** 关系类型连线色：前置/后置与普通关联区分开，避免所有边糊成一样的灰线 */
const RELATION_COLORS: Record<string, string> = {
  prerequisite: '#93b4f5',
  related: '#d8e0ea',
  successor: '#c4b5fd'
};

function toG6Data(data: KnowledgeGraphVO) {
  const nodes = (data.nodes || []).map((node) => ({
    id: node.id,
    data: {
      label: node.label,
      type: node.type,
      refId: node.refId,
      masteryPercent: node.masteryPercent,
      status: node.status,
      highlight: node.highlight,
      raw: node
    },
    style: {
      size: resolveSize(node),
      fill: resolveFill(node),
      stroke: node.highlight ? '#2563eb' : '#ffffff',
      lineWidth: node.highlight ? 3.5 : 2,
      shadowColor: node.highlight ? 'rgba(37, 99, 235, 0.35)' : 'rgba(15, 23, 42, 0.08)',
      shadowBlur: node.highlight ? 14 : 8,
      shadowOffsetY: 2,
      // 掌握状态光晕：让「薄弱 / 学习中 / 已掌握」一眼可辨，而不是只有细微的填充色差
      halo: Boolean(node.status && STATUS_COLORS[node.status]),
      haloStroke: node.status ? STATUS_COLORS[node.status] : undefined,
      haloStrokeOpacity: 0.26,
      haloLineWidth: 9,
      // 标签加浅色底并限宽换行，避免长知识点名互相压盖、糊成一团
      labelText: node.label || '',
      labelFontSize: 11.5,
      labelFontWeight: 600,
      labelFill: '#1e293b',
      labelBackground: true,
      labelBackgroundFill: 'rgba(255, 255, 255, 0.94)',
      labelBackgroundStroke: '#eef2f7',
      labelBackgroundLineWidth: 1,
      labelBackgroundRadius: 6,
      labelPadding: [3, 8],
      labelMaxWidth: 132,
      labelWordWrap: true,
      labelMaxLines: 2
    }
  }));

  const edges = (data.edges || []).map((edge, index) => ({
    id: `edge-${index}`,
    source: edge.source,
    target: edge.target,
    data: { relation: edge.relation },
    style: {
      stroke: RELATION_COLORS[edge.relation || ''] || '#d8e0ea',
      lineWidth: 1.3,
      endArrow: true,
      endArrowSize: 6
    }
  }));

  return { nodes, edges };
}

async function renderGraph() {
  if (!containerRef.value) return;

  try {
    const g6Data = toG6Data(props.graphData);

    if (!graph) {
      graph = new Graph({
        container: containerRef.value,
        width: containerRef.value.clientWidth || 800,
        height: props.height,
        data: g6Data,
        // 布局完成后自动适应画布，避免节点缩成一团、四周大片留白
        autoFit: 'view',
        padding: 24,
        layout: {
          type: 'd3-force',
          link: { distance: 130 },
          collide: { radius: 66 },
          manyBody: { strength: -340 }
        },
        node: {
          style: {
            labelText: (d: { data?: { label?: string } }) => d?.data?.label || ''
          }
        },
        edge: {
          style: {
            stroke: '#d8e0ea',
            lineWidth: 1.3,
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

    await graph.render();
    graph.fitView();
    scheduleFit();
  } catch {
    // 图谱渲染失败不应拖垮整页：静默降级，其余区域仍可正常使用
  }
}

/**
 * 力导向布局会持续迭代，渲染瞬间 fitView 时节点尚未铺开，
 * 这里再补一次延迟自适应，确保最终布局完整落在画布内、四周不留大片空白。
 */
function scheduleFit() {
  if (fitTimer) {
    clearTimeout(fitTimer);
  }
  fitTimer = setTimeout(() => {
    fitTimer = null;
    try {
      graph?.fitView();
    } catch {
      /* 忽略：图谱已销毁 */
    }
  }, 600);
}

function zoomIn() {
  graph?.zoomBy(1.2);
}

function zoomOut() {
  graph?.zoomBy(0.8);
}

function fitView() {
  graph?.fitView();
}

function handleResize() {
  if (!graph || !containerRef.value) return;
  graph.setSize(containerRef.value.clientWidth, props.height);
  graph.fitView();
}

watch(
  () => props.graphData,
  () => renderGraph(),
  { deep: true }
);

watch(
  () => props.height,
  () => handleResize()
);

onMounted(() => {
  renderGraph();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  if (fitTimer) {
    clearTimeout(fitTimer);
    fitTimer = null;
  }
  graph?.destroy();
  graph = null;
});
</script>

<style scoped lang="scss">
.knowledge-graph-shell {
  position: relative;
  width: 100%;
}

.graph-toolbar {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 5;
  display: flex;
  gap: 6px;
}

.graph-tool-btn {
  width: 28px;
  height: 28px;
  border-radius: 9999px;
  border: 1px solid #e8eef7;
  background: rgba(255, 255, 255, 0.94);
  color: #475569;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  transition: border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;

  &:hover {
    border-color: #bfdbfe;
    color: #1677ff;
    background: #f5faff;
  }

  &--wide {
    width: auto;
    padding: 0 12px;
    font-size: 12px;
    font-weight: 600;
  }
}

.knowledge-graph-g6 {
  width: 100%;
  background:
    radial-gradient(1200px 400px at 50% -10%, rgba(22, 119, 255, 0.05), transparent 70%),
    #fff;
  border: 1px solid #e8eef7;
  border-radius: 16px;
  overflow: hidden;
}

/**
 * 嵌入模式：外层已有卡片（如学习路径面板）时不再自带边框与圆角，
 * 否则会出现「卡中卡」的双层边框，视觉上非常笨重。
 */
.knowledge-graph-shell.is-borderless .knowledge-graph-g6 {
  border: none;
  border-radius: 0;
  box-shadow: none;
  background:
    radial-gradient(900px 320px at 50% -10%, rgba(22, 119, 255, 0.055), transparent 70%),
    #fbfdff;
}
</style>
