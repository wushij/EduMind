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
        <span>生成试卷方案并进入排版预览</span>
      </span>
      <span v-else>正在智能抽取题目并编排试卷...</span>
    </button>
    <div v-if="!isScoreMatched" class="mismatch-warning-pill">
      <el-icon><Warning /></el-icon>
      <span>需调节各题型分值，使得小计总和等于目标设定总分（当前相差 {{ Math.abs(calculatedTotalScore - examForm.totalScore) }} 分）</span>
    </div>
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
  (e: 'generate'): void;
}>();
</script>

<style scoped lang="scss">
.launch-exam-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 8px;

  .capsule-generate-btn {
    width: 100%;
    max-width: 680px;
    height: 50px;
    border-radius: 9999px; // 长圆跑道
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    color: #ffffff;
    border: none;
    font-size: 15.5px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
    transition: all 0.25s ease;

    .btn-inner-content {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 28px rgba(37, 99, 235, 0.45);
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      box-shadow: none;
    }
  }

  .mismatch-warning-pill {
    padding: 6px 16px;
    border-radius: 9999px; // 长圆边框
    background: #fffbeb;
    border: 1px solid #fde68a;
    font-size: 12.5px;
    color: #d97706;
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}
</style>
