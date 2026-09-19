<template>
  <div class="document-uploader" :class="{ 'document-uploader--uploading': uploading }">
    <el-upload
      drag
      multiple
      :auto-upload="false"
      :show-file-list="false"
      :disabled="uploading"
      :on-change="handleFileChange"
      accept=".pdf,.doc,.docx,.md,.markdown,.txt"
      class="upload-dropzone"
    >
      <div class="upload-inner">
        <div class="upload-icon-wrap">
          <el-icon v-if="!uploading" class="upload-icon"><UploadFilled /></el-icon>
          <el-icon v-else class="upload-icon is-loading"><Loading /></el-icon>
        </div>
        <p class="upload-title">
          {{ uploading ? '正在上传文档…' : '拖拽文件到此处，或点击上传' }}
        </p>
        <p class="upload-subtitle">
          支持 PDF / Word / Markdown，可多选或拖入多个文件，单文件建议不超过 50MB
        </p>
      </div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { Loading, UploadFilled } from '@element-plus/icons-vue';
import type { UploadFile } from 'element-plus';
import { createUploadFileBatcher } from '@/utils/upload/coalesce-upload-files';

defineProps<{
  uploading?: boolean;
}>();

const emit = defineEmits<{ 'select-files': [files: File[]] }>();

const enqueueFiles = createUploadFileBatcher((files) => {
  emit('select-files', files);
});

function handleFileChange(file: UploadFile) {
  if (!file.raw) return;
  enqueueFiles(file.raw);
}
</script>

<style scoped lang="scss">
.document-uploader {
  margin-bottom: 18px;

  &--uploading {
    opacity: 0.85;
  }

  :deep(.upload-dropzone) {
    width: 100%;
  }

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    height: auto;
    padding: 0;
    border: none;
    background: transparent;
  }

  .upload-inner {
    border: 1.5px dashed #bfdbfe;
    border-radius: 14px;
    background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
    padding: 28px 20px;
    text-align: center;
    transition: all 0.2s ease;

    &:hover {
      border-color: #60a5fa;
      background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
    }
  }

  .upload-icon-wrap {
    width: 52px;
    height: 52px;
    margin: 0 auto 10px;
    border-radius: 9999px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .upload-icon {
    font-size: 24px;
    color: #2563eb;

    &.is-loading {
      animation: spin 1s linear infinite;
    }
  }

  .upload-title {
    margin: 0 0 6px;
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
  }

  .upload-subtitle {
    margin: 0;
    font-size: 12px;
    color: #64748b;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
