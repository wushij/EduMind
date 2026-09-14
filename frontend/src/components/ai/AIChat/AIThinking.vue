<template>
  <div
    v-if="displayText || (active && !hasAnswerBody)"
    class="reasoning-card"
  >
    <div class="reasoning-header" @click="toggleFold">
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
        <span class="reasoning-title">深度思考</span>
      </span>
      <span class="reasoning-toggle-icon">{{ folded ? '展开 ▼' : '收起 ▲' }}</span>
    </div>

    <!-- 思考详情展开区 -->
    <div v-show="!folded" class="reasoning-body">
      <div
        v-if="displayText"
        ref="reasoningContentRef"
        class="reasoning-content reasoning-md chat-md-content"
        v-html="renderedHtml"
      />
      <div v-else-if="active && !hasAnswerBody" class="thinking-inline-status">
        <span class="thinking-spinner" />
        <span>{{ phaseMessage || '正在推理思考与检索切片中…' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { cleanReasoningText } from '@/utils/ai/copilot-stream-split';
import { bindMarkdownCodeCopy, renderReasoningMarkdown } from '@/utils/ai/chat-markdown';

const props = withDefaults(
  defineProps<{
    content?: string;
    folded?: boolean;
    active?: boolean;
    hasAnswerBody?: boolean;
    phaseMessage?: string;
  }>(),
  {
    content: '',
    folded: true,
    active: false,
    hasAnswerBody: false,
    phaseMessage: ''
  }
);

const emit = defineEmits<{
  'update:folded': [value: boolean];
}>();

const displayText = computed(() => cleanReasoningText(props.content || ''));

const title = computed(() => {
  if (props.active && !props.hasAnswerBody) return '思考过程中…';
  return '深度思考';
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
  emit('update:folded', !props.folded);
}
</script>

<style scoped lang="scss">
.reasoning-card {
  margin-bottom: 12px;
  border-radius: 10px;
  border: 1px solid rgba(22, 119, 255, 0.2);
  background: linear-gradient(180deg, rgba(235, 245, 255, 0.8) 0%, rgba(248, 250, 252, 0.95) 100%);
  overflow: hidden;
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
  padding: 10px 14px 12px 16px;
  border-top: 1px dashed rgba(22, 119, 255, 0.15);
  font-size: 12.5px;
  line-height: 1.68;
  color: #334155;
}

.reasoning-content {
  font-size: 12.5px;
  color: #334155;
  min-width: 0;

  :deep(p) {
    margin: 6px 0;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 10px 0 6px;
    font-size: 13px;
    font-weight: 600;
    color: #0f172a;
  }

  :deep(ul),
  :deep(ol) {
    margin: 6px 0 10px;
    padding-left: 1.45em;
    list-style-position: outside;
  }

  :deep(ul) {
    list-style-type: disc;
  }

  :deep(ol) {
    list-style-type: decimal;
  }

  :deep(li) {
    margin: 4px 0;
    padding-left: 0.2em;
  }

  :deep(blockquote) {
    margin: 8px 0;
    padding: 6px 10px;
    border-left: 3px solid rgba(22, 119, 255, 0.35);
    background: rgba(255, 255, 255, 0.55);
    color: #64748b;
    border-radius: 4px;
  }

  :deep(strong) {
    color: #0f172a;
    font-weight: 600;
  }

  :deep(code) {
    background: rgba(15, 23, 42, 0.06);
    padding: 1px 4px;
    border-radius: 4px;
    font-size: 11.5px;
  }

  :deep(.katex) {
    word-break: normal;
    overflow-wrap: normal;
  }

  :deep(.table-wrap) {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 10px 0 12px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
    background: #fafbfc;
  }

  :deep(.table-wrap::-webkit-scrollbar) {
    height: 4px;
  }

  :deep(.table-wrap::-webkit-scrollbar-button) {
    display: none;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    line-height: 1.55;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #e2e8f0;
    padding: 6px 10px;
    text-align: left;
    vertical-align: top;
    word-break: break-word;
    min-width: 72px;
  }

  :deep(th) {
    background: #f8fafc;
    font-weight: 600;
    color: #1677ff;
  }

  :deep(.math-fallback) {
    font-family: 'Fira Code', Consolas, monospace;
    font-size: 11.5px;
    color: #64748b;
  }

  :deep(.mermaid-diagram-wrapper) {
    display: none;
  }

  :deep(.code-block-wrapper) {
    margin: 10px 0;
    border-radius: 8px;
    background: #1e1e1e;
    overflow: hidden;
    border: 1px solid #333333;

    .code-header {
      background: #252526;
      padding: 6px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;

      .code-lang {
        font-size: 11px;
        color: #9cdcfe;
        font-weight: 700;
        text-transform: uppercase;
      }

      .code-copy-btn {
        flex-shrink: 0;
        background: #3c3c3c;
        color: #cccccc;
        border: none;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 10.5px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #505050;
          color: #ffffff;
        }

        &.is-copied {
          background: #52c41a;
          color: #ffffff;
        }
      }
    }

    pre {
      margin: 0;
      padding: 10px 12px;
      overflow-x: auto;
      font-size: 12px;
      line-height: 1.55;
    }
  }
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
