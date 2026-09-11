<template>
  <div class="document-uploader">
    <el-upload
      drag
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
    >
      <div class="upload-tip">拖拽文件到此处，或点击上传（PDF / Word / Markdown）</div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import type { UploadFile } from 'element-plus';
import { useDocumentUpload } from '@/composables/knowledge/useDocumentUpload';

const props = defineProps<{ kbId: number }>();
const emit = defineEmits<{ uploaded: [] }>();

const { upload } = useDocumentUpload(props.kbId);

async function handleFileChange(file: UploadFile) {
  if (!file.raw) return;
  try {
    await upload(file.raw);
    ElMessage.success('文档上传成功');
    emit('uploaded');
  } catch {
    ElMessage.error('文档上传失败');
  }
}
</script>

<style scoped lang="scss">
.document-uploader {
  margin-bottom: 16px;
  .upload-tip {
    padding: 24px;
    color: #6b7280;
  }
}
</style>
