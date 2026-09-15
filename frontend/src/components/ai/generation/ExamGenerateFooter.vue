<template>
  <div class="launch-exam-footer">
    <button
      type="button"
      class="capsule-generate-btn"
      :disabled="generating || !isScoreMatched"
      @click="$emit('generate')"
    >
      <span v-if="!generating" class="btn-inner-content">
        <el-icon><Lightning /></el-icon>
        <span>启动 AI 智能组卷并排版预览</span>
      </span>
      <span v-else>正在智能抽取题目并编排试卷...</span>
    </button>
    <p v-if="!isScoreMatched" class="mismatch-warning">
      <el-icon class="mr-1"><Warning /></el-icon>
      <span>需调整各题型分值，使得小计总和等于目标设定总分（当前相差 {{ Math.abs(calculatedTotalScore - examForm.totalScore) }} 分）</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { Lightning, Warning } from '@element-plus/icons-vue';
import type { ExamFormState } from './exam-generate-types';

defineProps<{
  examForm: ExamFormState;
  calculatedTotalScore: number;
  isScoreMatched: boolean;
  generating: boolean;
}>();

defineEmits<{
  generate: [];
}>();
</script>

<style scoped lang="scss">
.launch-exam-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  .capsule-generate-btn {
    width: 100%;
    max-width: 520px;
    height: 48px;
    border-radius: 9999px; // 长圆跑道
    background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
    color: #FFFFFF;
    border: none;
    font-size: 15.5px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 20px rgba(22, 119, 255, 0.35);
    transition: all 0.22s ease;

    .btn-inner-content {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 26px rgba(114, 46, 209, 0.45);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .mismatch-warning {
    margin: 0;
    font-size: 12.5px;
    color: #D97706;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}
</style>
