<template>
  <div class="knowledge-graph-page">
    <div class="graph-header">
      <h2>课程知识图谱</h2>
      <p>深度关系图谱（V1.1 · G6 力导向）</p>
      <el-slider v-model="depth" :min="1" :max="3" style="width: 160px" />
      <el-select v-model="relationTypes" multiple collapse-tags placeholder="关系类型" style="width: 220px">
        <el-option label="prerequisite" value="prerequisite" />
        <el-option label="related" value="related" />
        <el-option label="successor" value="successor" />
      </el-select>
      <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="loadGraph">
        刷新图谱
      </el-button>
    </div>

    <div v-loading="loading" class="graph-layout">
      <KnowledgeGraphG6
        :graph-data="graphData"
        @node-click="handleNodeClick"
      />
      <div class="side-panels">
        <el-card v-if="selectedNode" class="node-detail-card" shadow="never">
          <h3>节点详情</h3>
          <p><strong>名称：</strong>{{ selectedNode.label }}</p>
          <p><strong>类型：</strong>{{ selectedNode.type }}</p>
          <p v-if="selectedNode.refId"><strong>引用 ID：</strong>{{ selectedNode.refId }}</p>
        </el-card>
        <GraphGapPanel :gaps="gaps" />
        <GraphSuggestPanel
          :kb-id="kbId || 0"
          :source-knowledge-point-id="selectedKpId"
          @accepted="loadGraph"
        />
        <GraphRelationEditor @saved="loadGraph" />
      </div>
    </div>
    <el-empty v-if="!loading && graphData.nodes.length === 0" description="暂无图谱数据，请先完成文档切片与知识点配置" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';
import { Refresh } from '@element-plus/icons-vue';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import { useKnowledgeGraph } from '@/composables/knowledge/useKnowledgeGraph';
import KnowledgeGraphG6 from '@/components/knowledge/KnowledgeGraphG6.vue';
import GraphGapPanel from '@/components/knowledge/GraphGapPanel.vue';
import GraphSuggestPanel from '@/components/knowledge/GraphSuggestPanel.vue';
import GraphRelationEditor from '@/components/knowledge/GraphRelationEditor.vue';

const { kbId } = useKnowledgeRoute();
const authStore = useAuthStore();
const studentId = computed(() => authStore.currentUser?.id ?? 3);

const {
  loading,
  selectedNode,
  graphData,
  gaps,
  depth,
  relationTypes,
  selectedKpId,
  handleNodeClick,
  loadGraph
} = useKnowledgeGraph({ kbId, studentId });
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

  .node-detail-card {
    border-radius: 12px;
  }
}
</style>
