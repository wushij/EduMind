<template>
  <div class="question-banner-stage">
    <div class="banner-ratio-box">
      <img
        class="banner-image"
        :src="bannerImg"
        alt="AI 智能出题"
        draggable="false"
      />
      <div class="banner-float-actions">
        <button type="button" class="capsule-back-btn" @click="$emit('back')">
          <span>← 返回 AI 广场</span>
        </button>
      </div>

      <!-- 5 步向导步骤进度条 (嵌入 Banner 内部左下方，紧凑长圆跑道胶囊) -->
      <div class="banner-wizard-dock">
        <div
          v-for="step in steps"
          :key="step.index"
          class="wizard-step-item"
          :class="{
            active: currentStep === step.index,
            completed: currentStep > step.index
          }"
          @click="$emit('go-to-step', step.index)"
        >
          <span class="step-num">
            <el-icon v-if="currentStep > step.index"><Check /></el-icon>
            <span v-else>{{ step.index }}</span>
          </span>
          <span class="step-name">{{ step.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue';

defineProps<{
  bannerImg: string;
  steps: { index: number; name: string }[];
  currentStep: number;
}>();

defineEmits<{
  back: [];
  'go-to-step': [index: number];
}>();
</script>

<style scoped lang="scss">
.question-banner-stage {
  width: 100%;
  margin-bottom: 20px;

  .banner-ratio-box {
    position: relative;
    width: 100%;
    aspect-ratio: 2508 / 627;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 6px 24px rgba(22, 119, 255, 0.08);
    border: 1px solid #E2E8F0;

    .banner-image {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      user-select: none;
    }

    .banner-float-actions {
      position: absolute;
      top: 16px;
      right: 20px;
      z-index: 2;

      .capsule-back-btn {
        height: 34px;
        padding: 0 16px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(8px);
        border: 1px solid rgba(226, 232, 240, 0.85);
        color: #334155;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

        &:hover {
          color: #1677FF;
          background: #FFFFFF;
          border-color: #93C5FD;
        }
      }
    }

    // 2. 嵌入 Banner 内部左下方的 5 步向导紧凑长圆跑道指示条
    .banner-wizard-dock {
        position: absolute;
        left: 4.27%;
        top: 83%;
        z-index: 2;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 4px 6px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.94);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.9);
        box-shadow: 0 4px 18px rgba(22, 119, 255, 0.12);

        .wizard-step-item {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          border-radius: 9999px;
          font-size: 13px;
          font-weight: 500;
          color: #64748B;
          cursor: pointer;
          transition: all 0.2s ease;
          white-space: nowrap;

          .step-num {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: #F1F5F9;
            color: #475569;
            font-size: 11.5px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s ease;
          }

          &:hover:not(.active) {
            color: #1677FF;
            background: rgba(239, 246, 255, 0.6);
          }

          &.active {
            background: #1677FF;
            color: #FFFFFF;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);

            .step-num {
              background: #FFFFFF;
              color: #1677FF;
            }
          }

          &.completed {
            color: #1677FF;

            .step-num {
              background: #EFF6FF;
              color: #1677FF;
            }
          }
        }
      }

      @media (max-width: 1400px) {
        .banner-wizard-dock {
          top: 81%;
          gap: 2px;
          padding: 3px 4px;

          .wizard-step-item {
            padding: 5px 10px;
            font-size: 12px;
            gap: 5px;

            .step-num {
              width: 18px;
              height: 18px;
              font-size: 11px;
            }
          }
        }
      }

      @media (max-width: 1100px) {
        .banner-wizard-dock {
          top: 79%;
          left: 4.27%;
          gap: 2px;
          padding: 2px 4px;

          .wizard-step-item {
            padding: 4px 8px;
            font-size: 11px;
            gap: 4px;

            .step-num {
              width: 16px;
              height: 16px;
              font-size: 10px;
            }
          }
        }
      }
    }
  }
</style>
