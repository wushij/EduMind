<template>
  <div class="learning-path-graph-panel">
    <div class="panel-header">
      <div class="header-left">
        <span class="title-decor-pill"></span>
        <h3 class="panel-title">知识图谱路径视图</h3>
      </div>
      <div class="legend-row">
        <span class="legend-item legend-weak">薄弱</span>
        <span class="legend-item legend-learning">学习中</span>
        <span class="legend-item legend-mastered">已掌握</span>
      </div>
    </div>
    <div v-if="!hasGraph" class="graph-empty">
      <el-empty description="当前课程未绑定知识库，仍可按周计划学习；绑定后可展示图谱子图" :image-size="100" />
    </div>
    <div v-else class="graph-body">
      <KnowledgeGraphG6 :graph-data="graphVo" @node-click="onNodeClick" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import KnowledgeGraphG6 from '@/components/knowledge/KnowledgeGraphG6.vue';
import type { LearningPathGraphSlice } from '@/types/learning/learning-path';
import type { KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';
import { weekAnchorId } from '@/utils/learning/learning-path';

const props = defineProps<{
  graphSlice: LearningPathGraphSlice | null | undefined;
}>();

const emit = defineEmits<{
  (e: 'focus-week', weekNo: number): void;
}>();

const hasGraph = computed(() => (props.graphSlice?.nodes?.length ?? 0) > 0);

const graphVo = computed<KnowledgeGraphVO>(() => ({
  nodes: (props.graphSlice?.nodes ?? []).map((n) => ({
    id: n.id,
    label: n.label,
    type: n.type,
    refId: n.refId
  })),
  edges: (props.graphSlice?.edges ?? []).map((e) => ({
    source: e.source,
    target: e.target,
    relation: e.relation
  }))
}));

function onNodeClick(node: KnowledgeGraphNode | null) {
  if (!node?.refId) return;
  const el = document.getElementById(weekAnchorId(1));
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  emit('focus-week', 1);
}
</script>

<style scoped lang="scss">
.learning-path-graph-panel {
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  overflow: hidden;
  min-height: 420px;
  display: flex;
  flex-direction: column;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    border-bottom: 1px solid #f1f5f9;
    flex-wrap: wrap;
    gap: 10px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .title-decor-pill {
    width: 4px;
    height: 16px;
    border-radius: 9999px;
    background: #1677ff;
  }

  .panel-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }

  .legend-row {
    display: flex;
    gap: 10px;
  }

  .legend-item {
    font-size: 11px;
    padding: 2px 10px;
    border-radius: 9999px;
    font-weight: 600;

    &.legend-weak {
      background: #fef2f2;
      color: #dc2626;
    }
    &.legend-learning {
      background: #fffbeb;
      color: #d97706;
    }
    &.legend-mastered {
      background: #ecfdf5;
      color: #059669;
    }
  }

  .graph-body {
    padding: 8px 12px 16px;
    flex: 1;
  }

  .graph-empty {
    padding: 24px;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
