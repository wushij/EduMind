<template>
  <div class="page-container">
    <div class="page-header">
      <h2>文档管理</h2>
    </div>
    <div class="page-content">
      <el-card shadow="never">
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
import { onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import DocumentUploader from '@/components/knowledge/DocumentUploader.vue';
import DocumentTable from '@/components/knowledge/DocumentTable.vue';
import { useDocumentUpload } from '@/composables/knowledge/useDocumentUpload';

const route = useRoute();
const kbId = Number(route.params.id);
const { documents, loading, fetchDocuments, remove, triggerParse } = useDocumentUpload(kbId);

onMounted(() => fetchDocuments());

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
</style>
