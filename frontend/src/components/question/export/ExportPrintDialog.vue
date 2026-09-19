<template>
  <el-dialog
    v-model="visible"
    class="export-print-dialog"
    width="min(920px, 96vw)"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <template #header>
      <div class="dialog-head">
        <span class="dialog-title">纯净打印预览</span>
      </div>
    </template>

    <div ref="previewScrollRef" class="print-dialog-preview">
      <div ref="previewMountRef" class="print-dialog-preview-inner" />
      <div v-if="!mountedPreview" class="preview-loading">正在准备卷面…</div>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <button type="button" class="module-capsule-btn module-capsule-btn--secondary" @click="visible = false">
          取消
        </button>
        <button type="button" class="module-capsule-btn module-capsule-btn--primary" @click="emit('confirm')">
          <el-icon><Printer /></el-icon>
          <span>开始打印</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { Printer } from '@element-plus/icons-vue';
import { cloneExamPaperElement } from '@/utils/print/exam-paper-print';

const visible = defineModel<boolean>({ required: true });

const props = defineProps<{
  currentViewOnly?: boolean;
}>();

const emit = defineEmits<{
  confirm: [];
}>();

const previewScrollRef = ref<HTMLElement | null>(null);
const previewMountRef = ref<HTMLElement | null>(null);
const mountedPreview = ref(false);

async function mountPreviewClone() {
  mountedPreview.value = false;
  await nextTick();
  await nextTick();

  const host = previewMountRef.value;
  if (!host) return;

  host.innerHTML = '';
  const clone = cloneExamPaperElement({
    currentViewOnly: props.currentViewOnly,
    hideWatermark: true
  });

  if (!clone) {
    mountedPreview.value = false;
    return;
  }

  host.appendChild(clone);
  mountedPreview.value = true;
  previewScrollRef.value?.scrollTo(0, 0);
}

watch(visible, async (open) => {
  if (!open) return;
  await mountPreviewClone();
});

watch(
  () => props.currentViewOnly,
  () => {
    if (visible.value) {
      void mountPreviewClone();
    }
  }
);

function onClosed() {
  if (previewMountRef.value) {
    previewMountRef.value.innerHTML = '';
  }
  mountedPreview.value = false;
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.dialog-head {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .dialog-title {
    font-size: 16px;
    font-weight: 600;
    color: #0f172a;
  }
}

.print-dialog-preview {
  position: relative;
  max-height: min(72vh, 760px);
  min-height: 200px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 0;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

.print-dialog-preview-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding: 0;
  margin: 0;

  :deep(.export-paper-root) {
    width: 100%;
    max-width: 740px;
    margin: 0 auto;
    gap: 0;
  }

  :deep(.simulated-sheet) {
    width: 100% !important;
    max-width: 740px !important;
    min-height: 0 !important;
    margin: 0 !important;
    padding: 32px 36px 32px 32px !important;
    box-shadow: none !important;
    border: none !important;
    border-radius: 0 !important;
    background: #ffffff !important;
  }

  :deep(.simulated-sheet + .simulated-sheet) {
    border-top: 1px dashed #e5e7eb;
  }
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #64748b;
  background: rgba(241, 245, 249, 0.92);
  pointer-events: none;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

<style lang="scss">
.export-print-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.export-print-dialog .el-dialog__body {
  padding-top: 8px;
  overflow: visible;
}
</style>
