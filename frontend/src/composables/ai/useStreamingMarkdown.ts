import { ref, onBeforeUnmount } from 'vue';
import { renderChatMarkdown } from '@/utils/ai/chat-markdown';

/** 流式阶段 Markdown 重绘最小间隔（ms），避免高频全文 parse 阻塞主线程导致 UI 假死 */
const STREAM_RENDER_INTERVAL_MS = 80;

/**
 * 流式 Markdown 渲染：rAF + 时间节流，并在单次 parse 期间继续排队后续帧
 */
export function useStreamingMarkdown() {
  const rawText = ref('');
  const renderedHtml = ref('');
  const isStreaming = ref(false);

  let rafId: number | null = null;
  let timerId: ReturnType<typeof setTimeout> | null = null;
  let lastRenderAt = 0;

  function cancelScheduledRender() {
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (timerId !== null) {
      clearTimeout(timerId);
      timerId = null;
    }
  }

  function runRenderPass() {
    rafId = null;
    const snapshot = rawText.value;

    // 让出当前微任务主线程，避免 markdown 长解析饿死 SSE 与 Vue 组件响应式更新
    setTimeout(() => {
      const textToRender = rawText.value;
      if (!textToRender && !snapshot) return;

      const html = renderChatMarkdown(textToRender);
      renderedHtml.value = typeof html === 'string' ? html : '';
      lastRenderAt = performance.now();

      if (rawText.value !== textToRender) {
        scheduleRender();
      }
    }, 0);
  }

  function scheduleRender() {
    if (rafId !== null || timerId !== null) return;

    const elapsed = performance.now() - lastRenderAt;
    const delay = Math.max(0, STREAM_RENDER_INTERVAL_MS - elapsed);

    if (delay === 0) {
      rafId = requestAnimationFrame(runRenderPass);
    } else {
      timerId = setTimeout(() => {
        timerId = null;
        rafId = requestAnimationFrame(runRenderPass);
      }, delay);
    }
  }

  function appendChunk(delta: string) {
    if (!delta) return;
    rawText.value += delta;
    isStreaming.value = true;
    scheduleRender();
  }

  function setText(text: string) {
    rawText.value = text || '';
    cancelScheduledRender();
    renderedHtml.value = renderChatMarkdown(rawText.value);
    lastRenderAt = performance.now();
  }

  function finish() {
    isStreaming.value = false;
    cancelScheduledRender();
    renderedHtml.value = renderChatMarkdown(rawText.value);
    lastRenderAt = performance.now();
  }

  function reset() {
    isStreaming.value = false;
    rawText.value = '';
    renderedHtml.value = '';
    cancelScheduledRender();
    lastRenderAt = 0;
  }

  onBeforeUnmount(() => {
    cancelScheduledRender();
  });

  return {
    rawText,
    renderedHtml,
    isStreaming,
    appendChunk,
    setText,
    finish,
    reset
  };
}
