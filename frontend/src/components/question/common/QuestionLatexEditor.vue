<template>
  <div class="question-latex-editor">
    <el-input
      ref="inputRef"
      :model-value="modelValue"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      :class="inputClass"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <div v-if="showPreview && previewText" class="latex-preview-panel">
      <div class="preview-head">
        <span class="preview-label">渲染预览</span>
        <span class="preview-hint">行内 $...$ · 独立 $$...$$</span>
      </div>
      <MathText :text="previewText" tag="div" custom-class="preview-body" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import MathText from '@/components/common/MathText.vue';
import type { ElInput } from 'element-plus';

const inputRef = ref<InstanceType<typeof ElInput>>();

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    rows?: number;
    placeholder?: string;
    inputClass?: string;
    showPreview?: boolean;
  }>(),
  {
    modelValue: '',
    rows: 3,
    placeholder: '',
    inputClass: '',
    showPreview: true
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void;
}>();

const previewText = computed(() => (props.modelValue || '').trim());

function focus() {
  inputRef.value?.focus?.();
  const el = inputRef.value?.textarea ?? inputRef.value?.$el?.querySelector?.('textarea');
  el?.focus?.();
}

defineExpose({ focus });
</script>

<style scoped lang="scss">
.question-latex-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .latex-preview-panel {
    padding: 10px 14px;
    border-radius: 12px;
    border: 1px dashed #cbd5e1;
    background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);

    .preview-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 6px;
    }

    .preview-label {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: #64748b;
    }

    .preview-hint {
      font-size: 11px;
      color: #94a3b8;
    }

    :deep(.preview-body) {
      font-size: 14px;
      line-height: 1.65;
      color: #0f172a;

      .katex {
        font-size: 1.08em;
      }

      .math-block {
        margin: 0.35em 0;
      }
    }
  }
}
</style>
