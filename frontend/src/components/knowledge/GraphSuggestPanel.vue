<template>
  <el-card shadow="never" class="graph-suggest-panel">
    <template #header>
      <div class="panel-header">
        <span>AI 关系补全建议</span>
        <el-button type="primary" link :loading="loading" @click="loadSuggestions">
          刷新建议
        </el-button>
      </div>
    </template>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && suggestions.length === 0"
        description="暂无关系补全建议"
        :image-size="60"
      />

      <div v-else class="suggestion-list">
        <div
          v-for="item in suggestions"
          :key="`${item.sourceKnowledgePointId}-${item.targetKnowledgePointId}`"
          class="suggestion-item"
        >
          <div class="relation-line">
            <span class="kp-title">{{ item.sourceTitle }}</span>
            <el-tag size="small" type="info">{{ item.relationType }}</el-tag>
            <span class="kp-title">{{ item.targetTitle }}</span>
          </div>
          <p class="reason-text">{{ item.reason }}</p>
          <div class="item-footer">
            <el-tag size="small" effect="plain">
              置信度 {{ Math.round((item.confidence || 0) * 100) }}%
            </el-tag>
            <el-button
              size="small"
              type="primary"
              plain
              :loading="acceptingId === item.targetKnowledgePointId"
              @click="handleAccept(item)"
            >
              采纳关系
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { suggestRelations, createKnowledgePointRelation } from '@/api/knowledge/graph';
import type { GraphRelationSuggestion } from '@/types/knowledge/graph';

const props = defineProps<{
  kbId: number;
  sourceKnowledgePointId?: number;
}>();

const emit = defineEmits<{
  (e: 'accepted'): void;
}>();

const loading = ref(false);
const acceptingId = ref<number | null>(null);
const suggestions = ref<GraphRelationSuggestion[]>([]);

async function loadSuggestions() {
  if (!props.kbId) return;
  loading.value = true;
  try {
    const res = await suggestRelations(props.kbId, {
      sourceKnowledgePointId: props.sourceKnowledgePointId,
      maxSuggestions: 5
    });
    suggestions.value = res?.data ?? [];
  } catch {
    suggestions.value = [];
    ElMessage.error('加载关系建议失败');
  } finally {
    loading.value = false;
  }
}

async function handleAccept(item: GraphRelationSuggestion) {
  acceptingId.value = item.targetKnowledgePointId;
  try {
    await createKnowledgePointRelation(item.sourceKnowledgePointId, {
      targetKnowledgePointId: item.targetKnowledgePointId,
      relationType: item.relationType
    });
    ElMessage.success('关系已建立');
    suggestions.value = suggestions.value.filter(
      (s) => s.targetKnowledgePointId !== item.targetKnowledgePointId
    );
    emit('accepted');
  } catch {
    ElMessage.error('建立关系失败');
  } finally {
    acceptingId.value = null;
  }
}

watch(
  () => [props.kbId, props.sourceKnowledgePointId],
  () => loadSuggestions()
);

onMounted(loadSuggestions);
</script>

<style scoped lang="scss">
.graph-suggest-panel {
  border-radius: 12px;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;
    color: #0f172a;
  }

  .suggestion-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .suggestion-item {
    padding: 10px 12px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #f8fafc;

    .relation-line {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 6px;

      .kp-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
      }
    }

    .reason-text {
      margin: 0 0 8px;
      font-size: 12px;
      line-height: 1.5;
      color: #64748b;
    }

    .item-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>
