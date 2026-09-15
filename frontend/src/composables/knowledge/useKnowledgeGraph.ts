import { ref, computed, watch, onMounted, type Ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createKnowledgePointRelation,
  deleteKnowledgePointRelation,
  getGraphGaps,
  getKnowledgeGraph,
  listKnowledgePointRelations,
  suggestRelations
} from '@/api/knowledge/graph';
import type {
  GraphGapVO,
  GraphRelationSuggestion,
  KnowledgeGraphNode,
  KnowledgeGraphVO
} from '@/types/knowledge/graph';

export function buildRelationTypesParam(types: string[]): string {
  return types.join(',');
}

export function resolveKnowledgePointId(node: KnowledgeGraphNode | null): number | undefined {
  if (node?.type === 'KNOWLEDGE_POINT' && node.refId) {
    return node.refId;
  }
  return undefined;
}

export interface UseKnowledgeGraphOptions {
  kbId: Ref<number | undefined>;
  studentId: Ref<number>;
}

export async function fetchKnowledgePointRelations(knowledgePointId: number) {
  return listKnowledgePointRelations(knowledgePointId);
}

export async function saveKnowledgePointRelation(
  sourceKnowledgePointId: number,
  data: { targetKnowledgePointId: number; relationType: string }
) {
  return createKnowledgePointRelation(sourceKnowledgePointId, data);
}

export async function removeKnowledgePointRelation(relationId: number) {
  return deleteKnowledgePointRelation(relationId);
}

export async function fetchRelationSuggestions(
  kbId: number,
  params?: { sourceKnowledgePointId?: number; maxSuggestions?: number }
) {
  return suggestRelations(kbId, params);
}

export function useKnowledgeGraph(options: UseKnowledgeGraphOptions) {
  const { kbId, studentId } = options;
  const loading = ref(false);
  const selectedNode = ref<KnowledgeGraphNode | null>(null);
  const graphData = ref<KnowledgeGraphVO>({ nodes: [], edges: [] });
  const gaps = ref<GraphGapVO[]>([]);
  const depth = ref(2);
  const relationTypes = ref<string[]>(['prerequisite', 'related']);

  const selectedKpId = computed(() => resolveKnowledgePointId(selectedNode.value));

  function handleNodeClick(node: KnowledgeGraphNode | null) {
    selectedNode.value = node;
  }

  async function loadGraph() {
    if (!kbId.value) return;
    loading.value = true;
    try {
      const types = buildRelationTypesParam(relationTypes.value);
      const res = await getKnowledgeGraph(kbId.value, depth.value, types);
      graphData.value = res?.data || { nodes: [], edges: [] };
      const gapRes = await getGraphGaps(kbId.value, studentId.value, 0.6);
      gaps.value = gapRes?.data || [];
    } catch {
      graphData.value = { nodes: [], edges: [] };
      ElMessage.error('加载知识图谱失败');
    } finally {
      loading.value = false;
    }
  }

  watch(kbId, () => loadGraph());
  watch([depth, relationTypes], () => loadGraph(), { deep: true });

  onMounted(loadGraph);

  return {
    loading,
    selectedNode,
    graphData,
    gaps,
    depth,
    relationTypes,
    selectedKpId,
    handleNodeClick,
    loadGraph
  };
}
