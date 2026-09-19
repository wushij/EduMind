<template>
  <div class="ai-cognitive-thinking">
    <div class="thinking-radar-box">
      <div class="pulse-ring ring-1"></div>
      <div class="pulse-ring ring-2"></div>
      <div class="pulse-ring ring-3"></div>

      <div class="compass-pill-core">
        <svg viewBox="0 0 48 48" class="compass-svg" fill="none" aria-hidden="true">
          <defs>
            <linearGradient :id="gradCompass" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#2563EB" />
              <stop offset="50%" stop-color="#3B82F6" />
              <stop offset="100%" stop-color="#7C3AED" />
            </linearGradient>
            <linearGradient :id="gradNorth" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#F59E0B" />
              <stop offset="100%" stop-color="#FBBF24" />
            </linearGradient>
            <linearGradient :id="gradInner" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#FFFFFF" />
              <stop offset="100%" stop-color="#F0F7FF" />
            </linearGradient>
            <filter :id="gradGlow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="1.2" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
          </defs>

          <circle
            cx="24"
            cy="24"
            r="20"
            :stroke="`url(#${gradCompass})`"
            stroke-width="1.8"
            stroke-dasharray="2 4"
            class="compass-dial"
          />
          <circle
            cx="24"
            cy="24"
            r="15"
            :fill="`url(#${gradInner})`"
            stroke="rgba(37, 99, 235, 0.28)"
            stroke-width="1.2"
            class="compass-inner-core"
          />
          <path
            d="M24 11 L26 22 L37 24 L26 26 L24 37 L22 26 L11 24 L22 22 Z"
            :fill="`url(#${gradCompass})`"
            :filter="`url(#${gradGlow})`"
            class="compass-star"
          />
          <path d="M24 11 L26 24 L24 22 Z" fill="#FFFFFF" opacity="0.95" />
          <path d="M24 11 L22 24 L24 22 Z" :fill="`url(#${gradNorth})`" />
          <circle cx="24" cy="24" r="2.8" fill="#FFFFFF" stroke="rgba(37, 99, 235, 0.4)" stroke-width="0.8" />
          <circle cx="24" cy="24" r="1.4" fill="#2563EB" />
        </svg>
      </div>
    </div>

    <div class="thinking-header">
      <h4 class="thinking-title">
        {{ title }}
      </h4>
      <span class="thinking-timer">
        {{ elapsedTimeText }}
      </span>
    </div>

    <div v-if="steps.length" class="pipeline-steps">
      <div
        v-for="(label, idx) in steps"
        :key="idx"
        class="pipeline-step"
        :class="{
          active: currentStep >= idx + 1,
          done: currentStep > idx + 1
        }"
      >
        <span class="step-dot"></span>
        <span class="step-label">{{ label }}</span>
      </div>
    </div>

    <div v-if="showFooterActions" class="thinking-action-buttons">
      <button type="button" class="pill-ctrl-btn abort-btn" @click="$emit('abort')">
        <el-icon><Close /></el-icon>
        <span>{{ abortLabel }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRef, useId } from 'vue';
import { Close } from '@element-plus/icons-vue';
import { useAiThinkingTimer } from '@/composables/ai/useAiThinkingTimer';

const props = withDefaults(
  defineProps<{
    active: boolean;
    title: string;
    steps?: string[];
    showFooterActions?: boolean;
    abortLabel?: string;
  }>(),
  {
    steps: () => [],
    showFooterActions: false,
    abortLabel: '中止并退出'
  }
);

defineEmits<{
  abort: [];
}>();

const uid = useId();
const gradCompass = computed(() => `aiCompassGrad${uid}`);
const gradNorth = computed(() => `aiNorthGrad${uid}`);
const gradInner = computed(() => `aiInnerGrad${uid}`);
const gradGlow = computed(() => `aiStarGlow${uid}`);

const { currentStep, elapsedTimeText } = useAiThinkingTimer(toRef(props, 'active'), {
  stepCount: props.steps.length || 3
});
</script>

<style scoped lang="scss">
.ai-cognitive-thinking {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px 12px;
}

.thinking-radar-box {
  position: relative;
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;

  .compass-pill-core {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
    border: 1.4px solid rgba(59, 130, 246, 0.38);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow:
      0 4px 16px rgba(37, 99, 235, 0.16),
      0 0 10px rgba(124, 58, 237, 0.08);
    z-index: 2;

    .compass-svg {
      width: 32px;
      height: 32px;
    }

    .compass-dial {
      transform-origin: center;
      animation: dialSlowSpin 20s linear infinite;
    }

    .compass-star {
      transform-origin: center;
      animation: starGlowBreath 3.5s ease-in-out infinite;
    }
  }

  .pulse-ring {
    position: absolute;
    border-radius: 50%;
    border: 1.2px solid rgba(59, 130, 246, 0.3);
    animation: pulse-out 2.4s ease-out infinite;

    &.ring-1 {
      width: 52px;
      height: 52px;
      animation-delay: 0s;
    }
    &.ring-2 {
      width: 64px;
      height: 64px;
      animation-delay: 0.8s;
    }
    &.ring-3 {
      width: 76px;
      height: 76px;
      animation-delay: 1.6s;
    }
  }
}

.thinking-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;

  .thinking-title {
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    margin: 0;
    text-align: center;
  }

  .thinking-timer {
    font-size: 13px;
    font-weight: 700;
    color: #2563eb;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    border-radius: 9999px;
    padding: 2px 10px;
    transition: all 0.3s ease;
  }
}

.pipeline-steps {
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .pipeline-step {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    border-radius: 9999px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    transition: all 0.3s ease;

    .step-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #cbd5e1;
      transition: all 0.3s ease;
      flex-shrink: 0;
    }

    .step-label {
      font-size: 13px;
      color: #64748b;
      font-weight: 500;
      line-height: 1.4;
    }

    &.active {
      background: #eff6ff;
      border-color: #93c5fd;

      .step-dot {
        background: #3b82f6;
        box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
      }

      .step-label {
        color: #1d4ed8;
        font-weight: 600;
      }
    }

    &.done {
      background: #f0fdf4;
      border-color: #bbf7d0;

      .step-dot {
        background: #10b981;
      }

      .step-label {
        color: #15803d;
      }
    }
  }
}

.thinking-action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
  width: 100%;

  .pill-ctrl-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 36px;
    padding: 0 18px;
    border-radius: 9999px;
    border: 1px solid;
    font-size: 13px;
    font-weight: 600;
    line-height: 1;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease;

    .el-icon {
      font-size: 15px;
    }

    &:focus-visible {
      outline: 2px solid #3b82f6;
      outline-offset: 2px;
    }

    &:active {
      transform: scale(0.98);
    }
  }

  .abort-btn {
    background: #ffffff;
    border-color: #fca5a5;
    color: #dc2626;

    &:hover {
      background: #fef2f2;
      border-color: #ef4444;
      color: #b91c1c;
      box-shadow: 0 2px 10px rgba(239, 68, 68, 0.18);
    }
  }
}

@keyframes pulse-out {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

@keyframes dialSlowSpin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes starGlowBreath {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.95;
  }
  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}
</style>
