<template>
  <el-card shadow="never" class="relation-editor">
    <template #header>维护知识点关系</template>
    <el-form :model="form" label-width="100px" size="small">
      <el-form-item label="源知识点">
        <el-input-number v-model="form.sourceId" :min="1" @change="loadRelations" />
      </el-form-item>
      <el-form-item label="目标知识点">
        <el-input-number v-model="form.targetId" :min="1" />
      </el-form-item>
      <el-form-item label="关系类型">
        <el-select v-model="form.relationType">
          <el-option label="前置 prerequisite" value="prerequisite" />
          <el-option label="相关 related" value="related" />
          <el-option label="后置 successor" value="successor" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="submit">保存关系</el-button>
    </el-form>
    <el-table :data="relations" size="small" style="margin-top: 12px">
      <el-table-column prop="relationType" label="类型" width="100" />
      <el-table-column prop="targetKnowledgePointId" label="目标" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="danger" @click="removeRelation(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchKnowledgePointRelations,
  saveKnowledgePointRelation,
  removeKnowledgePointRelation
} from '@/composables/knowledge/useKnowledgeGraph';

const emit = defineEmits<{ saved: [] }>();
const loading = ref(false);
const relations = ref<Array<{ id: number; relationType: string; targetKnowledgePointId: number }>>([]);
const form = reactive({
  sourceId: 15,
  targetId: 14,
  relationType: 'prerequisite'
});

async function loadRelations() {
  try {
    const res = await fetchKnowledgePointRelations(form.sourceId);
    relations.value = res.data ?? [];
  } catch {
    relations.value = [];
  }
}

async function submit() {
  loading.value = true;
  try {
    await saveKnowledgePointRelation(form.sourceId, {
      targetKnowledgePointId: form.targetId,
      relationType: form.relationType
    });
    ElMessage.success('关系已保存');
    await loadRelations();
    emit('saved');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    loading.value = false;
  }
}

async function removeRelation(id: number) {
  try {
    await removeKnowledgePointRelation(id);
    ElMessage.success('已删除');
    await loadRelations();
    emit('saved');
  } catch {
    ElMessage.error('删除失败');
  }
}

onMounted(loadRelations);
</script>
