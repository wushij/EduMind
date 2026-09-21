import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { TeachingCopilotContext } from '@/types/ai/teaching-copilot-context';

export const OPEN_GLOBAL_ASSISTANT_EVENT = 'edumind:open-global-assistant';

export const useTeachingCopilotStore = defineStore('teaching-copilot-context', () => {
  const activeContext = ref<TeachingCopilotContext | null>(null);

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
    const prefill = optionalPrefillPrompt?.trim() || '';
    window.dispatchEvent(
      new CustomEvent(OPEN_GLOBAL_ASSISTANT_EVENT, {
        detail: {
          prefill,
          autoSend: options?.autoSend ?? Boolean(prefill),
          startNewSession: Boolean(options?.startNewSession)
        }
      })
    );
  }

  return {
    activeContext,
    setContext,
    patchContext,
    clearContext,
    openAssistantWithContext
  };
});
