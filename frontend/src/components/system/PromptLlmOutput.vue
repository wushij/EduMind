<template>
  <div
    ref="rootRef"
    class="prompt-llm-output markdown-body chat-md-content"
    v-html="renderedHtml"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { bindMarkdownCodeCopy, renderChatMarkdown, renderMermaidInElement } from '@/utils/ai/chat-markdown';

const props = defineProps<{
  content: string;
}>();

const rootRef = ref<HTMLElement | null>(null);
const renderedHtml = computed(() => renderChatMarkdown(props.content || ''));

function refreshMarkdownUi() {
  nextTick(() => {
    if (!rootRef.value) return;
    bindMarkdownCodeCopy(rootRef.value);
    renderMermaidInElement(rootRef.value);
  });
}

watch(renderedHtml, () => refreshMarkdownUi());
onMounted(() => refreshMarkdownUi());
</script>

<style scoped lang="scss">
.prompt-llm-output {
  font-size: 13.5px;
  line-height: 1.7;
  color: #1e293b;
  word-break: break-word;

  :deep(p) {
    margin: 8px 0;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(strong) {
    font-weight: 700;
    color: #0958d9;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 14px 0 8px;
    color: #0f172a;
    font-weight: 700;
  }

  :deep(h1) {
    font-size: 1.25em;
  }

  :deep(h2) {
    font-size: 1.15em;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 4px;
  }

  :deep(h3) {
    font-size: 1.05em;
  }

  :deep(ul),
  :deep(ol) {
    margin: 8px 0;
    padding-left: 1.5em;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(blockquote) {
    margin: 10px 0;
    padding: 8px 12px;
    background: #f8fafc;
    border-left: 3px solid #1677ff;
    color: #64748b;
    border-radius: 4px;
    font-size: 12.5px;
  }

  :deep(hr) {
    margin: 14px 0;
    border: none;
    border-top: 1px solid #e2e8f0;
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
    box-sizing: border-box;
  }

  :deep(table) {
    width: 100%;
    min-width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    line-height: 1.55;
    table-layout: auto;
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
    white-space: nowrap;
  }

  :deep(.code-block-wrapper) {
    margin: 12px 0;
    border-radius: 8px;
    background: #1e1e1e;
    overflow: hidden;
    border: 1px solid #333;

    .code-header {
      background: #252526;
      padding: 6px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .code-lang {
        font-size: 11px;
        color: #9cdcfe;
        font-weight: 700;
        text-transform: uppercase;
      }

      .code-copy-btn {
        background: #3c3c3c;
        color: #ccc;
        border: none;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 10.5px;
        cursor: pointer;
      }
    }

    pre {
      margin: 0;
      padding: 10px 12px;
      overflow-x: auto;
      font-size: 12px;
      line-height: 1.55;
      color: #d4d4d4;
    }
  }

  :deep(code:not(.hljs)) {
    background: rgba(15, 23, 42, 0.06);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Fira Code', Consolas, monospace;
    font-size: 12px;
    color: #0284c7;
  }
}
</style>
