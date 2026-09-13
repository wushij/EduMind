<template>
  <div class="page-container">
    <div class="page-header">
      <h2>文档管理</h2>
    </div>
    <div class="page-content">
      <el-alert
        v-if="!kbId"
        type="error"
        title="无效的知识库 ID"
        description="请从知识库列表进入，或选择有效的知识库后再访问文档管理。"
        show-icon
        :closable="false"
        class="kb-id-alert"
      />
      <el-card v-else shadow="never">
        <DocumentUploader :kb-id="kbId" @uploaded="fetchDocuments" />
        <DocumentTable
          v-loading="loading"
          :documents="documents"
          @parse="handleParse"
          @delete="handleDelete"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import DocumentUploader from '@/components/knowledge/DocumentUploader.vue';
import DocumentTable from '@/components/knowledge/DocumentTable.vue';
import { useDocumentUpload } from '@/composables/knowledge/useDocumentUpload';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';

const { kbId } = useKnowledgeRoute();
const { documents, loading, fetchDocuments, remove, triggerParse } = useDocumentUpload(kbId);

onMounted(() => {
  if (kbId.value) {
    fetchDocuments();
  }
});

watch(kbId, (id) => {
  if (id) {
    fetchDocuments();
  }
});

async function handleParse(id: number) {
  await triggerParse(id);
  ElMessage.success('已触发文档解析');
}

async function handleDelete(id: number) {
  await remove(id);
  ElMessage.success('文档已删除');
}
</script>

<style scoped lang="scss">
.page-container .page-header {
  margin-bottom: 16px;
  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }
}

.kb-id-alert {
  margin-bottom: 16px;
}
</style>
