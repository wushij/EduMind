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
          <svg
            v-if="!uploading"
            class="upload-svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path class="upload-arrow-stem" d="M12 19V5" />
            <path class="upload-arrow-head" d="m5 12 7-7 7 7" />
          </svg>
          <svg
            v-else
            class="upload-loading-svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          >
            <circle cx="12" cy="12" r="9" stroke="#e2e8f0" stroke-width="2" />
            <path d="M12 3a9 9 0 0 1 9 9" stroke="#2563eb" stroke-width="2" />
          </svg>
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
    border: 1.5px dashed #cbd5e1;
    border-radius: 12px;
    background: #f8fafc;
    padding: 30px 20px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #2563eb;
      background: #eff6ff;

      .upload-icon-wrap {
        background: #dbeafe;
        color: #1d4ed8;
        transform: translateY(-2px);

        .upload-arrow-head,
        .upload-arrow-stem {
          transform: translateY(-1.5px);
        }
      }
    }
  }

  .upload-icon-wrap {
    width: 48px;
    height: 48px;
    margin: 0 auto 12px;
    border-radius: 50%;
    background: #e2e8f0;
    color: #475569;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    .upload-svg {
      width: 24px;
      height: 24px;
      display: block;

      .upload-arrow-head,
      .upload-arrow-stem {
        transition: transform 0.2s ease;
      }
    }

    .upload-loading-svg {
      width: 24px;
      height: 24px;
      display: block;
      animation: spin 0.8s linear infinite;
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
