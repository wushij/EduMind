<template>
  <div class="kb-documents-page">
    <el-alert
      v-if="!kbId"
      type="error"
      title="无效的知识库 ID"
      description="请从知识库列表进入，或选择有效的知识库后再访问文档管理。"
      show-icon
      :closable="false"
      class="kb-id-alert"
    />

    <template v-else>
      <div class="doc-panel-card">
        <div class="panel-toolbar">
          <div class="toolbar-left">
            <span class="panel-title">文档管理</span>
            <span class="panel-badge">共 {{ documents.length }} 篇课件</span>
          </div>
          <div class="toolbar-right">
            <button type="button" class="table-action-pill table-action-pill--primary" @click="fetchDocuments">
              <el-icon class="mr-1"><Refresh /></el-icon>
              <span>刷新列表</span>
            </button>
          </div>
        </div>

        <div id="kb-doc-upload">
          <DocumentUploader
            :uploading="uploading"
            @select-file="handleUpload"
          />
        </div>

        <DocumentTable
          v-loading="loading"
          :documents="documents"
          @parse="handleParse"
          @delete="handleDelete"
          @view-chunks="handleViewChunks"
          @rechunk="handleRechunk"
        />

        <div v-if="!loading && documents.length === 0" class="empty-doc-panel">
          <el-icon class="empty-icon"><Document /></el-icon>
          <h3>暂无入库文档</h3>
          <p>拖拽或点击上方上传区，添加 PDF / Word / Markdown 课件后即可进行解析与切片。</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Document, Refresh } from '@element-plus/icons-vue';
import DocumentUploader from '@/components/knowledge/DocumentUploader.vue';
import DocumentTable from '@/components/knowledge/DocumentTable.vue';
import { useDocumentUpload } from '@/composables/knowledge/useDocumentUpload';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';

const router = useRouter();
const { kbId } = useKnowledgeRoute();
const { documents, loading, uploading, fetchDocuments, upload, remove, triggerParse, triggerRechunk } =
  useDocumentUpload(kbId);

async function handleUpload(file: File) {
  try {
    await upload(file);
    ElMessage.success('文档上传成功');
  } catch {
    ElMessage.error('文档上传失败');
  }
}

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
  try {
    await triggerParse(id);
    ElMessage.success('已触发文档解析');
  } catch {
    ElMessage.error('触发解析失败');
  }
}

async function handleDelete(id: number) {
  const target = documents.value.find(item => item.id === id);
  const fileName = target?.fileName || '该文档';

  try {
    await ElMessageBox.confirm(
      `确定删除「${fileName}」吗？删除后将同时移除关联切片，且不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    );
    await remove(id);
    ElMessage.success('文档已删除');
  } catch {
    // 用户取消
  }
}

function handleViewChunks(documentId: number) {
  if (!kbId.value) return;
  router.push({
    path: `/knowledge/${kbId.value}/chunks`,
    query: { docId: String(documentId) }
  });
}

async function handleRechunk(documentId: number) {
  try {
    await triggerRechunk(documentId);
    ElMessage.success('已触发重新切片');
    await fetchDocuments();
  } catch {
    ElMessage.error('重新切片失败');
  }
}
</script>

<style scoped lang="scss">
.kb-documents-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kb-id-alert {
  margin-bottom: 0;
}

.doc-panel-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px 16px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .panel-title {
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;
  }

  .panel-badge {
    height: 24px;
    padding: 0 10px;
    border-radius: 9999px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    color: #2563eb;
    font-size: 12px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
  }
}

.table-action-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 9999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;

  &--primary {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #2563eb;

    &:hover {
      background: #2563eb;
      border-color: #2563eb;
      color: #ffffff;
      transform: translateY(-1px);
    }
  }
}

.empty-doc-panel {
  margin-top: 12px;
  padding: 28px 16px;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  text-align: center;

  .empty-icon {
    font-size: 36px;
    color: #94a3b8;
    margin-bottom: 8px;
  }

  h3 {
    margin: 0 0 6px;
    font-size: 16px;
    color: #334155;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #64748b;
  }
}

.mr-1 {
  margin-right: 4px;
}
</style>
