<template>
  <el-dialog
    :model-value="visible"
    :title="title || 'AI 智能阅卷推演引擎'"
    width="640px"
    class="assignment-grading-engine-dialog"
    destroy-on-close
    :close-on-click-modal="false"
    :show-close="true"
    append-to-body
    @close="handleDialogClose"
  >
    <AiCognitiveThinkingPanel
      :active="visible"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.assignmentAiGrading"
      show-footer-actions
      abort-label="中止批改"
      @abort="$emit('abort')"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';

const props = defineProps<{
  visible: boolean;
  title?: string;
}>();

const emit = defineEmits<{
  (e: 'abort'): void;
}>();

function handleDialogClose() {
  if (props.visible) {
    emit('abort');
  }
}
</script>

<style scoped lang="scss">
.assignment-grading-engine-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18);
  }

  :deep(.el-dialog__header) {
    padding: 18px 24px 12px;
    margin-right: 0;
    border-bottom: 1px solid #f1f5f9;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  :deep(.el-dialog__body) {
    padding: 10px 24px 20px;
  }
}
</style>
