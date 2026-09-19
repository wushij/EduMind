<template>
  <div class="question-generate-banner">
    <div class="banner-glass-card">
      <div class="banner-top-row">
        <div class="title-meta-col">
          <h1 class="page-title">AI 智能教学出题工作台</h1>
          <p class="page-desc">
            基于课程大纲知识树、考查重点（examFocus）与大模型专属命题认知指令，分步配置并一键生成严谨试题，支持原位精修、单题变式与一键入库组卷。
          </p>
        </div>

        <div class="action-col">
          <button type="button" class="back-pill-btn" @click="$emit('back')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回 AI 广场</span>
          </button>
        </div>
      </div>

      <div class="step-nav-row">
        <AiWizardStepBar
          :steps="steps"
          :current-step="currentStep"
          @go-to-step="$emit('go-to-step', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';
import AiWizardStepBar from '@/components/ai/common/AiWizardStepBar.vue';

defineProps<{
  steps: { index: number; name: string }[];
  currentStep: number;
}>();

defineEmits<{
  back: [];
  'go-to-step': [index: number];
}>();
</script>

<style scoped lang="scss">
.question-generate-banner {
  width: 100%;
  margin-bottom: 20px;

  .banner-glass-card {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(240, 247, 255, 0.85) 100%);
    backdrop-filter: blur(12px);
    border-radius: 20px;
    padding: 24px 28px 18px 28px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 20px rgba(30, 80, 160, 0.05);

    .banner-top-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 18px;

      .page-title {
        margin: 0 0 6px 0;
        font-size: 22px;
        font-weight: 800;
        color: #0F172A;
        letter-spacing: -0.3px;
      }

      .page-desc {
        margin: 0;
        font-size: 13.5px;
        color: #64748B;
        line-height: 1.5;
        max-width: 820px;
      }

      .action-col {
        flex-shrink: 0;

        .back-pill-btn {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 36px;
          padding: 0 16px;
          border-radius: 9999px;
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          color: #475569;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            border-color: #1677FF;
            color: #1677FF;
            background: #F8FAFC;
          }
        }
      }
    }

    .step-nav-row {
      padding-top: 14px;
      border-top: 1px solid #EEF2F6;
    }
  }
}
</style>
