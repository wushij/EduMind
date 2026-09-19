<template>
  <div class="feedback-panel">
    <div class="feedback-header">
      <div class="status-indicator">
        <el-icon v-if="correct" class="icon-right"><Check /></el-icon>
        <el-icon v-else class="icon-wrong"><Close /></el-icon>
        <span class="status-text">
          {{ correct ? '回答正确！思维逻辑严谨' : '回答有误，请注意概念辨析' }}
        </span>
      </div>
      <div class="kp-badge">
        <span>考点关联：{{ kpTitle }}</span>
      </div>
    </div>

    <div class="analysis-body">
      <div class="reference-ans">
        <strong>参考标准答案：</strong>
        <MathText :text="referenceAnswer" />
      </div>
      <div class="ai-comment">
        <strong>AI 深度解析与变式溯源：</strong>
        <p>{{ analysis }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Close } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';

defineProps<{
  correct: boolean;
  kpTitle: string;
  referenceAnswer: string;
  analysis: string;
}>();
</script>

<style scoped lang="scss">
.feedback-panel {
  margin-top: 20px;
  border-radius: 12px;
  border: 1px solid #d6e4ff;
  background: #f4f8ff;
  padding: 16px 20px;

  .feedback-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    flex-wrap: wrap;
    gap: 8px;

    .status-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 700;

      .icon-right {
        color: #52c41a;
        font-size: 20px;
      }

      .icon-wrong {
        color: #f5222d;
        font-size: 20px;
      }
    }

    .kp-badge {
      font-size: 12px;
      color: #64748b;
    }
  }

  .analysis-body {
    font-size: 14px;
    line-height: 1.7;
    color: #334155;

    .reference-ans {
      margin-bottom: 8px;
    }
  }
}
</style>
