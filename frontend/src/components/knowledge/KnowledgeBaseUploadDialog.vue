<template>
  <el-dialog
    :model-value="visible"
    title="上传课件与教辅文档"
    width="540px"
    class="capsule-custom-dialog"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="upload-dialog-body">
      <!-- 目标知识库选择 -->
      <div class="field-block">
        <label class="field-label">目标知识库</label>
        <el-select
          v-model="targetKbId"
          placeholder="请选择要存入的知识库"
          style="width: 100%"
          class="capsule-select"
        >
          <el-option
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :label="kb.name"
            :value="kb.id"
          />
        </el-select>
      </div>

      <!-- 文件上传区域 -->
      <div class="field-block">
        <label class="field-label">课件/资料文件</label>
        <div
          class="drop-zone"
          :class="{ 'is-dragover': isDragOver, 'has-file': !!selectedFile }"
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            class="hidden-file-input"
            accept=".pdf,.doc,.docx,.md,.markdown,.txt"
            @change="handleFileChange"
          />

          <template v-if="!selectedFile">
            <div class="upload-badge-wrap">
              <svg
                class="dialog-upload-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 19V5" />
                <path d="m5 12 7-7 7 7" />
              </svg>
            </div>
            <p class="drop-text">
              点击或将文件拖拽至此处上传
            </p>
            <p class="drop-hint">
              支持 PDF、Word (.doc/.docx)、Markdown (.md)、TXT，单个文件不超过 50MB
            </p>
          </template>

          <template v-else>
            <div class="selected-file-preview">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-info">
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
              </div>
              <button
                type="button"
                class="remove-file-btn"
                @click.stop="selectedFile = null"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </template>
        </div>
      </div>

      <!-- 解析设置提示 -->
      <div class="pipeline-tip-box">
        <el-icon class="tip-icon"><InfoFilled /></el-icon>
        <span class="tip-text">
          文件上传成功后，系统将自动启动异步解析流水线（文本提取、语义分块、向量 Embedding），完成后即可用于 AI 助教答疑检索。
        </span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-actions-dock">
        <button
          type="button"
          class="capsule-modal-btn is-cancel"
          @click="emit('update:visible', false)"
        >
          取消
        </button>
        <button
          type="button"
          class="capsule-modal-btn is-confirm"
          :disabled="uploading || !targetKbId || !selectedFile"
          @click="handleUpload"
        >
          <el-icon v-if="uploading" class="is-spinning"><Loading /></el-icon>
          <span>{{ uploading ? '上传并解析中...' : '确认上传文档' }}</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Document, Close, InfoFilled, Loading } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { uploadDocument } from '@/api/knowledge/document';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';

const props = defineProps<{
  visible: boolean;
  knowledgeBases: KnowledgeBase[];
  initialKbId?: number;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  success: [];
}>();

const targetKbId = ref<number | undefined>(undefined);
const selectedFile = ref<File | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const isDragOver = ref(false);
const uploading = ref(false);

watch(
  () => props.visible,
  (open) => {
    if (open) {
      selectedFile.value = null;
      if (props.initialKbId) {
        targetKbId.value = props.initialKbId;
      } else if (!targetKbId.value && props.knowledgeBases.length > 0) {
        targetKbId.value = props.knowledgeBases[0].id;
      }
    }
  }
);

function triggerFileInput() {
  if (!selectedFile.value) {
    fileInputRef.value?.click();
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    validateAndSetFile(target.files[0]);
  }
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false;
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    validateAndSetFile(event.dataTransfer.files[0]);
  }
}

function validateAndSetFile(file: File) {
  const maxBytes = 50 * 1024 * 1024; // 50MB
  if (file.size > maxBytes) {
    ElMessage.error('文件过大，单个文件不能超过 50MB');
    return;
  }
  selectedFile.value = file;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

async function handleUpload() {
  if (!targetKbId.value) {
    ElMessage.warning('请选择目标知识库');
    return;
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择需要上传的文档');
    return;
  }

  uploading.value = true;
  try {
    await uploadDocument(targetKbId.value, selectedFile.value);
    ElMessage.success(`文档《${selectedFile.value.name}》上传成功，正在后台执行切片与向量化！`);
    emit('success');
    emit('update:visible', false);
  } catch (err: any) {
    console.error('上传失败', err);
    ElMessage.error(err?.message || '文档上传失败，请检查网络或格式');
  } finally {
    uploading.value = false;
  }
}
</script>

<style scoped lang="scss">
.upload-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;

  .field-block {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .field-label {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }
  }

  .drop-zone {
    border: 2px dashed #cbd5e1;
    border-radius: 16px;
    background: #f8fafc;
    padding: 32px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover,
    &.is-dragover {
      border-color: #1677ff;
      background: #eff6ff;
    }

    &.has-file {
      padding: 16px 20px;
      cursor: default;
      border-style: solid;
      border-color: #bfdbfe;
      background: #f0f7ff;
    }

    .hidden-file-input {
      display: none;
    }

    .upload-badge-wrap {
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

      .dialog-upload-svg {
        width: 24px;
        height: 24px;
        display: block;
      }
    }

    &:hover,
    &.is-dragover {
      border-color: #2563eb;
      background: #eff6ff;

      .upload-badge-wrap {
        background: #dbeafe;
        color: #1d4ed8;
        transform: translateY(-2px);
      }
    }

    .drop-text {
      margin: 0 0 6px 0;
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;
    }

    .drop-hint {
      margin: 0;
      font-size: 12px;
      color: #94a3b8;
      max-width: 400px;
      line-height: 1.5;
    }

    .selected-file-preview {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;

      .file-icon {
        font-size: 28px;
        color: #2563eb;
        flex-shrink: 0;
      }

      .file-info {
        flex: 1;
        min-width: 0;
        text-align: left;
        display: flex;
        flex-direction: column;

        .file-name {
          font-size: 13.5px;
          font-weight: 600;
          color: #0f172a;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .file-size {
          font-size: 12px;
          color: #64748b;
        }
      }

      .remove-file-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: 1px solid #e2e8f0;
        background: #ffffff;
        color: #64748b;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #fee2e2;
          color: #ef4444;
          border-color: #fca5a5;
        }
      }
    }
  }

  .pipeline-tip-box {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    background: #f1f5f9;
    padding: 10px 14px;
    border-radius: 12px;

    .tip-icon {
      font-size: 16px;
      color: #64748b;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .tip-text {
      font-size: 12px;
      color: #64748b;
      line-height: 1.5;
    }
  }
}

.dialog-actions-dock {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  .capsule-modal-btn {
    height: 38px;
    padding: 0 20px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: all 0.2s ease;
    display: inline-flex;
    align-items: center;
    gap: 6px;

    &.is-cancel {
      background: #f1f5f9;
      color: #64748b;
      border: 1px solid #e2e8f0;

      &:hover {
        background: #e2e8f0;
        color: #334155;
      }
    }

    &.is-confirm {
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      color: #ffffff;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.28);

      &:hover:not(:disabled) {
        background: linear-gradient(135deg, #4096ff 0%, #1d4ed8 100%);
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}
</style>
