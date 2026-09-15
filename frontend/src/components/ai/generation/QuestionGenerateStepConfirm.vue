<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 5 步：确认出题指令参数并启动生成</h3>
    <p class="pane-desc">请核对以下出题约束，点击按钮后 AI 将立即调用 DeepSeek 引擎进行题目编排：</p>

    <div class="summary-check-panel">
      <div class="summary-item">
        <span class="s-label">课程空间：</span>
        <strong class="s-val">{{ selectedCourseName }}</strong>
      </div>
      <div class="summary-item">
        <span class="s-label">包含章节：</span>
        <span class="s-val">已选择 {{ formState.chapterIds.length }} 个大章节</span>
      </div>
      <div class="summary-item">
        <span class="s-label">核心考点：</span>
        <span class="s-val">{{ formState.knowledgePointNames.join('、') }}</span>
      </div>
      <div class="summary-item">
        <span class="s-label">命题难度：</span>
        <strong class="s-val">{{ difficultyLabel }}</strong>
      </div>
      <div class="summary-item">
        <span class="s-label">生成规模：</span>
        <strong class="s-val">{{ formState.count }} 道题目（每题 {{ formState.scorePerQuestion }} 分）</strong>
      </div>
    </div>

    <div class="launch-generate-box">
      <button
        type="button"
        class="capsule-generate-btn"
        :disabled="generating"
        @click="$emit('generate')"
      >
        <span v-if="!generating" class="btn-inner-content">
          <el-icon><Lightning /></el-icon>
          <span>启动 AI 智能出题（跳转卡片预览）</span>
        </span>
        <span v-else class="generating-text">
          <span class="spinner"></span> 正在运用 DeepSeek 深度命题与构建解析中...
        </span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lightning } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState } from './question-generate-types';

defineProps<{
  formState: QuestionGenerateFormState;
  selectedCourseName: string;
  difficultyLabel: string;
  generating: boolean;
}>();

defineEmits<{
  generate: [];
}>();
</script>

<style scoped lang="scss">
.step-content-pane {
  .pane-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 700;
    color: #0F172A;
  }

  .pane-desc {
    margin: 0 0 24px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.summary-check-panel {
  background: #F8FAFC;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 28px;

  .summary-item {
    display: flex;
    align-items: center;
    font-size: 14px;

    .s-label {
      color: #64748B;
      width: 90px;
      flex-shrink: 0;
    }

    .s-val {
      color: #0F172A;
    }
  }
}

.launch-generate-box {
  display: flex;
  justify-content: center;

  .capsule-generate-btn {
    width: 100%;
    max-width: 480px;
    height: 50px;
    border-radius: 9999px; // 长圆跑道
    background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
    color: #FFFFFF;
    border: none;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 20px rgba(22, 119, 255, 0.35);
    transition: all 0.25s ease;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 28px rgba(114, 46, 209, 0.45);
    }

    &:disabled {
      opacity: 0.75;
      cursor: wait;
    }

    .generating-text {
      display: inline-flex;
      align-items: center;
      gap: 10px;

      .spinner {
        width: 18px;
        height: 18px;
        border: 2.5px solid rgba(255, 255, 255, 0.3);
        border-top-color: #FFFFFF;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
