<template>
  <div
    v-if="displayText || (active && !hasAnswerBody)"
    ref="cardRef"
    class="reasoning-card"
    :class="{ 'is-active-reasoning': active && !hasAnswerBody }"
  >
    <div class="reasoning-header" @click="toggleFold">
      <div class="reasoning-header-left">
        <span class="reasoning-tag">
          <span class="reasoning-icon-wrap" :class="{ 'is-spinning': active && !hasAnswerBody }">
            <svg class="reasoning-compass-icon" viewBox="0 0 16 16" fill="none">
              <defs>
                <linearGradient id="thinkingGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#1677ff" />
                  <stop offset="100%" stop-color="#722ed1" />
                </linearGradient>
              </defs>
              <circle cx="8" cy="8" r="6.6" stroke="currentColor" stroke-width="1" stroke-dasharray="1.6 1.6" opacity="0.6" />
              <circle cx="8" cy="8" r="4.8" stroke="currentColor" stroke-width="0.8" opacity="0.4" />
              <path d="M8 2.6 L9.2 6.8 L13.4 8 L9.2 9.2 L8 13.4 L6.8 9.2 L2.6 8 L6.8 6.8 Z" fill="url(#thinkingGrad)" />
              <circle cx="8" cy="8" r="1.1" fill="#ffffff" />
            </svg>
          </span>
          <span class="reasoning-title">{{ title }}</span>
        </span>

        <!-- 实时秒表计时胶囊 / 结算用时标签 -->
        <span v-if="displayTimerText" class="reasoning-timer-badge" :class="{ 'is-ticking': active && !hasAnswerBody }">
          <span class="timer-dot" v-if="active && !hasAnswerBody"></span>
          <span class="timer-text">{{ displayTimerText }}</span>
        </span>
      </div>

      <div class="reasoning-header-right">
        <!-- 阶段中止响应按钮 (对标课程中心同款 VideoPause 控制) -->
        <button
          v-if="active && allowAbort"
          type="button"
          class="reasoning-abort-btn"
          title="停止当前生成与推理"
          @click.stop="$emit('abort')"
        >
          <span class="abort-icon">⏸</span>
          <span>停止响应</span>
        </button>

        <span class="reasoning-toggle-icon">{{ folded ? '展开 ▼' : '收起 ▲' }}</span>
      </div>
    </div>

    <!-- 思考详情展开区 -->
    <div v-show="!folded" ref="bodyRef" class="reasoning-body">
      <div
        v-if="displayText"
        ref="reasoningContentRef"
        class="reasoning-content reasoning-md reasoning-markdown-body"
        v-html="renderedHtml"
      />
      <div v-else-if="active && !hasAnswerBody" class="thinking-inline-status">
        <span class="thinking-spinner" />
        <span>{{ phaseMessage || '正在推理思考与检索切片中…' }}</span>
      </div>

      <button
        type="button"
        class="reasoning-collapse-btn"
        aria-label="收起深度思考"
        title="收起"
        @click.stop="toggleFold"
      >
        <svg class="reasoning-collapse-icon" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            d="M4.5 10.5 8 7l3.5 3.5"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M4.5 6.5 8 3l3.5 3.5"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
            opacity="0.55"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { cleanReasoningText } from '@/utils/ai/copilot-stream-split';
import { bindMarkdownCodeCopy, renderReasoningMarkdown } from '@/utils/ai/chat-markdown';
import { findChatScrollParent, preserveScrollOnCollapse } from '@/utils/dom/preserve-scroll-on-collapse';

const props = withDefaults(
  defineProps<{
    content?: string;
    folded?: boolean;
    active?: boolean;
    hasAnswerBody?: boolean;
    phaseMessage?: string;
    timerText?: string;
    showTimer?: boolean;
    allowAbort?: boolean;
  }>(),
  {
    content: '',
    folded: true,
    active: false,
    hasAnswerBody: false,
    phaseMessage: '',
    timerText: '',
    showTimer: true,
    allowAbort: false
  }
);

const emit = defineEmits<{
  'update:folded': [value: boolean];
  'user-collapse': [];
  abort: [];
}>();

const cardRef = ref<HTMLElement | null>(null);
const bodyRef = ref<HTMLElement | null>(null);

const displayText = computed(() => cleanReasoningText(props.content || ''));

const title = computed(() => {
  if (props.active && !props.hasAnswerBody) return '深度思考中…';
  return '深度思考';
});

// 秒表计时器状态
const elapsedSeconds = ref(0);
const finalDurationSeconds = ref<number | null>(null);
let timerHandle: ReturnType<typeof setInterval> | null = null;

function startTimer() {
  stopTimer();
  elapsedSeconds.value = 0;
  finalDurationSeconds.value = null;
  const startTime = Date.now();
  timerHandle = setInterval(() => {
    elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - startTime) / 1000));
  }, 500);
}

function stopTimer() {
  if (timerHandle) {
    clearInterval(timerHandle);
    timerHandle = null;
    if (elapsedSeconds.value > 0) {
      finalDurationSeconds.value = elapsedSeconds.value;
    }
  }
}

watch(
  () => [props.active, props.hasAnswerBody, props.showTimer],
  ([active, hasAnswer, showTimer]) => {
    if (showTimer && active && !hasAnswer) {
      startTimer();
    } else {
      stopTimer();
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  stopTimer();
});

function formatTimer(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

const displayTimerText = computed(() => {
  if (!props.showTimer) return '';
  if (props.timerText) return props.timerText;
  if (props.active && !props.hasAnswerBody) {
    return `已思考 ${formatTimer(elapsedSeconds.value)}`;
  }
  if (finalDurationSeconds.value !== null && finalDurationSeconds.value > 0) {
    return `用时 ${finalDurationSeconds.value} 秒`;
  }
  return '';
});

const renderedHtml = computed(() => renderReasoningMarkdown(displayText.value));
const reasoningContentRef = ref<HTMLDivElement | null>(null);

function refreshReasoningMarkdownUi() {
  nextTick(() => {
    if (reasoningContentRef.value) {
      bindMarkdownCodeCopy(reasoningContentRef.value, { renderMermaid: false });
    }
  });
}

watch(renderedHtml, () => refreshReasoningMarkdownUi());
watch(
  () => props.folded,
  (folded) => {
    if (!folded) refreshReasoningMarkdownUi();
  }
);
onMounted(() => refreshReasoningMarkdownUi());

function toggleFold() {
  const nextFolded = !props.folded;
  if (!nextFolded) {
    emit('update:folded', false);
    return;
  }

  const scrollEl = findChatScrollParent(cardRef.value);
  const collapsingEl = bodyRef.value;

  preserveScrollOnCollapse(scrollEl, collapsingEl, () => {
    emit('update:folded', true);
    emit('user-collapse');
  });
}
</script>

<style scoped lang="scss">
.reasoning-card {
  margin-bottom: 12px;
  border-radius: 10px;
  border: 1px solid rgba(22, 119, 255, 0.2);
  background: linear-gradient(180deg, rgba(235, 245, 255, 0.8) 0%, rgba(248, 250, 252, 0.95) 100%);
  overflow: hidden;
  overflow-anchor: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.05);

  &.is-active-reasoning {
    border-color: rgba(22, 119, 255, 0.45);
    box-shadow: 0 0 12px rgba(22, 119, 255, 0.15), inset 0 0 0 1px rgba(22, 119, 255, 0.1);
  }
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  transition: background 0.18s ease;

  &:hover {
    background: rgba(22, 119, 255, 0.06);
  }
}

.reasoning-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.reasoning-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reasoning-timer-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  padding: 1px 8px;
  line-height: 1.4;

  &.is-ticking {
    color: #1d4ed8;
    background: rgba(37, 99, 235, 0.12);
    border-color: rgba(37, 99, 235, 0.35);
  }

  .timer-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #3b82f6;
    animation: pulse-dot 1.2s ease-in-out infinite;
  }
}

.reasoning-abort-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fca5a5;
  border-radius: 999px;
  padding: 2px 9px;
  cursor: pointer;
  transition: all 0.2s ease;

  .abort-icon {
    font-size: 10px;
  }

  &:hover {
    background: #fee2e2;
    border-color: #ef4444;
    color: #b91c1c;
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
    transform: scale(1.03);
  }

  &:active {
    transform: scale(0.96);
  }
}

@keyframes pulse-dot {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.3);
    opacity: 1;
  }
}

.reasoning-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.reasoning-icon-wrap {
  display: inline-flex;
  width: 16px;
  height: 16px;
  color: #1677ff;

  &.is-spinning {
    animation: spin 1.4s linear infinite;
  }
}

.reasoning-compass-icon {
  width: 16px;
  height: 16px;
}

.reasoning-title {
  font-size: 12.5px;
  color: #1d39c4;
  font-weight: 600;
}

.reasoning-phase-badge {
  font-size: 11px;
  color: #69b1ff;
  background: rgba(22, 119, 255, 0.08);
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  font-weight: normal;
}

.reasoning-toggle-icon {
  font-size: 11px;
  color: #8c8c8c;
  transition: color 0.18s;

  &:hover {
    color: #1677ff;
  }
}

.reasoning-body {
  position: relative;
  padding: 10px 14px 30px 16px;
  border-top: 1px dashed rgba(22, 119, 255, 0.15);
  min-width: 0;
  overflow-x: auto;
}

.reasoning-collapse-btn {
  position: absolute;
  right: 8px;
  bottom: 6px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.88);
  color: #94a3b8;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    color: #1677ff;
    border-color: rgba(22, 119, 255, 0.35);
    background: rgba(235, 245, 255, 0.95);
    box-shadow: 0 2px 6px rgba(22, 119, 255, 0.12);
  }
}

.reasoning-collapse-icon {
  width: 12px;
  height: 12px;
}

.reasoning-content {
  min-width: 0;
}

.thinking-inline-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
  padding: 8px 0;
}

.thinking-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(22, 119, 255, 0.2);
  border-top-color: #1677ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
