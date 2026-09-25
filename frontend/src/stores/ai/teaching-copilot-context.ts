import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { TeachingCopilotContext } from '@/types/ai/teaching-copilot-context';

export const OPEN_GLOBAL_ASSISTANT_EVENT = 'edumind:open-global-assistant';

export const useTeachingCopilotStore = defineStore('teaching-copilot-context', () => {
  const activeContext = ref<TeachingCopilotContext | null>(null);

  /**
   * 最近一次「以课节上下文发起对话」的课节 ID。
   *
   * <p>activeContext 会在课节工作台卸载时被 clearContext 清空，因此无法用来判断
   * 「这次提问是不是换了课节」。这里独立留一个锚点：切换到另一个课节（尤其是
   * 教情报告「新建备课课节」刚生成的新课节）首次提问时，自动开启新会话，
   * 避免侧栏里显示上一节课甚至上一个入口遗留的旧对话。</p>
   */
  const lastCopilotLessonId = ref<number | null>(null);

  function setContext(ctx: TeachingCopilotContext | null) {
    activeContext.value = ctx;
  }

  function patchContext(patch: Partial<TeachingCopilotContext>) {
    if (!activeContext.value) {
      activeContext.value = { contextModule: 'global', ...patch };
      return;
    }
    activeContext.value = { ...activeContext.value, ...patch };
  }

  function clearContext() {
    activeContext.value = null;
  }

  function openAssistantWithContext(
    ctx: Partial<TeachingCopilotContext> & { contextModule: TeachingCopilotContext['contextModule'] },
    optionalPrefillPrompt?: string,
    options?: { autoSend?: boolean; startNewSession?: boolean }
  ) {
    const merged: TeachingCopilotContext = {
      ...(activeContext.value || {}),
      ...ctx
    } as TeachingCopilotContext;
    activeContext.value = merged;

    // 课节锚点比对必须在覆盖 lastCopilotLessonId 之前完成
    const nextLessonId = ctx.lessonChapterId;
    const lessonSwitched =
      typeof nextLessonId === 'number' &&
      lastCopilotLessonId.value !== null &&
      lastCopilotLessonId.value !== nextLessonId;
    if (typeof nextLessonId === 'number') {
      lastCopilotLessonId.value = nextLessonId;
    }

    const prefill = optionalPrefillPrompt?.trim() || '';
    window.dispatchEvent(
      new CustomEvent(OPEN_GLOBAL_ASSISTANT_EVENT, {
        detail: {
          prefill,
          autoSend: options?.autoSend ?? Boolean(prefill),
          startNewSession: Boolean(options?.startNewSession) || lessonSwitched
        }
      })
    );
  }

  return {
    activeContext,
    lastCopilotLessonId,
    setContext,
    patchContext,
    clearContext,
    openAssistantWithContext
  };
});
