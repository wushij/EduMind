<template>
  <div
    class="global-assistant-trigger"
    :class="{ 'is-active': active, 'is-streaming': isStreaming }"
    @click="handleClick"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <!-- 悬浮提示微胶囊 -->
    <transition name="tip-fade">
      <div v-if="isHovered && !active" class="copilot-hover-tip">
        <span class="tip-sparkle">✦</span>
        <span class="tip-title">智教云 AI · 智能副驾驶</span>
        <span class="tip-kbd">Alt+C</span>
      </div>
    </transition>

    <!-- 按钮圆盘主体 -->
    <div class="copilot-btn-body">
      <!-- 极光环境脉冲光环 -->
      <div class="aurora-pulse-ring" />

      <!-- 动态科技罗盘 SVG -->
      <svg viewBox="0 0 48 48" class="compass-svg" fill="none">
        <defs>
          <linearGradient id="copilotCoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#1677ff" />
            <stop offset="50%" stop-color="#4096ff" />
            <stop offset="100%" stop-color="#722ed1" />
          </linearGradient>
          <linearGradient id="copilotGoldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f59e0b" />
            <stop offset="100%" stop-color="#fbbf24" />
          </linearGradient>
          <filter id="copilotStarGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="1.8" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        <!-- 外层刻度圈 -->
        <circle
          cx="24"
          cy="24"
          r="20"
          stroke="url(#copilotCoreGrad)"
          stroke-width="1.8"
          stroke-dasharray="2 4"
          class="compass-dial"
        />

        <!-- 内核圆盘背景 -->
        <circle
          cx="24"
          cy="24"
          r="15"
          class="compass-inner-core"
          fill="rgba(15, 23, 42, 0.92)"
          stroke="rgba(22, 119, 255, 0.45)"
          stroke-width="1.2"
        />

        <!-- 罗盘四向极光星芒 -->
        <path
          d="M24 10 L26 22 L38 24 L26 26 L24 38 L22 26 L10 24 L22 22 Z"
          fill="url(#copilotCoreGrad)"
          filter="url(#copilotStarGlow)"
          class="compass-star"
        />

        <!-- 北针高亮极星 -->
        <path d="M24 10 L26 24 L24 22 Z" fill="#ffffff" opacity="0.95" />
        <path d="M24 10 L22 24 L24 22 Z" fill="url(#copilotGoldGrad)" />

        <!-- 中心罗盘核心宝石 -->
        <circle cx="24" cy="24" r="3" fill="#ffffff" />
        <circle cx="24" cy="24" r="1.5" fill="#1677ff" />
      </svg>

      <!-- 问答状态微徽标 (当正在流式生成时跳动) -->
      <span v-if="isStreaming" class="streaming-ping" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const props = withDefaults(
  defineProps<{
    active?: boolean;
    isStreaming?: boolean;
  }>(),
  {
    active: false,
    isStreaming: false
  }
);

const emit = defineEmits<{
  toggle: [];
}>();

const isHovered = ref(false);

function handleClick() {
  emit('toggle');
}

function handleGlobalKeydown(e: KeyboardEvent) {
  // 忽略在输入框中的快捷键
  const target = e.target as HTMLElement | null;
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)) {
    return;
  }

  const isAltC = e.altKey && (e.key === 'c' || e.key === 'C');
  const isCtrlShiftK = (e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'k' || e.key === 'K');

  if (isAltC || isCtrlShiftK) {
    e.preventDefault();
    e.stopPropagation();
    emit('toggle');
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});
</script>

<style scoped lang="scss">
.global-assistant-trigger {
  position: fixed;
  right: 28px;
  bottom: 32px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.copilot-hover-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(22, 119, 255, 0.35);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25), 0 0 12px rgba(22, 119, 255, 0.2);
  color: #ffffff;
  font-size: 12px;
  white-space: nowrap;

  .tip-sparkle {
    color: #4096ff;
    font-size: 13px;
    animation: sparkleRotate 3s linear infinite;
  }

  .tip-title {
    font-weight: 600;
    color: #f1f5f9;
  }

  .tip-kbd {
    padding: 1px 6px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.12);
    border: 1px solid rgba(255, 255, 255, 0.2);
    font-family: monospace;
    font-size: 10px;
    color: #91caff;
  }
}

.tip-fade-enter-active,
.tip-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.tip-fade-enter-from,
.tip-fade-leave-to {
  opacity: 0;
  transform: translateX(8px) scale(0.92);
}

.copilot-btn-body {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(145deg, #101c38, #0c1222) !important;
  border: 1.6px solid rgba(22, 119, 255, 0.5) !important;
  box-shadow: 0 6px 24px rgba(22, 119, 255, 0.35), 0 0 16px rgba(114, 46, 209, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: visible;

  &:hover {
    transform: translateY(-3px) scale(1.08);
    border-color: rgba(64, 150, 255, 0.9) !important;
    box-shadow: 0 10px 32px rgba(22, 119, 255, 0.5), 0 0 24px rgba(114, 46, 209, 0.4);

    .compass-star {
      transform: rotate(45deg);
    }
  }
}

.aurora-pulse-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid rgba(64, 150, 255, 0.55);
  animation: auroraPulse 2.8s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  pointer-events: none;
}

.compass-svg {
  width: 34px;
  height: 34px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.compass-dial {
  transform-origin: center;
  animation: dialSlowSpin 24s linear infinite;
}

.compass-star {
  transform-origin: center;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.streaming-ping {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 10px #52c41a;
  animation: pingDot 1.2s ease-in-out infinite alternate;
}

.global-assistant-trigger.is-active .copilot-btn-body {
  border-color: #4096ff !important;
  box-shadow: 0 0 24px rgba(22, 119, 255, 0.6);
  transform: scale(0.94);
}

@keyframes auroraPulse {
  0% {
    transform: scale(0.95);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.18);
    opacity: 0;
  }
  100% {
    transform: scale(0.95);
    opacity: 0;
  }
}

@keyframes dialSlowSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes sparkleRotate {
  0% {
    transform: scale(1) rotate(0deg);
  }
  50% {
    transform: scale(1.2) rotate(180deg);
  }
  100% {
    transform: scale(1) rotate(360deg);
  }
}

@keyframes pingDot {
  from {
    opacity: 0.4;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1.2);
  }
}
</style>
