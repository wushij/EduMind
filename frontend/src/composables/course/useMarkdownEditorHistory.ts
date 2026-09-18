import { ref, computed, type Ref } from 'vue';

export function useMarkdownEditorHistory(
  model: Ref<string>,
  onChange: (val: string) => void,
  maxHistory = 80
) {
  const historyStack = ref<string[]>([]);
  const historyIndex = ref(-1);
  const isHistoryAction = ref(false);
  let typingTimer: ReturnType<typeof setTimeout> | null = null;

  function pushHistory(val: string) {
    if (isHistoryAction.value) return;
    if (historyIndex.value >= 0 && historyStack.value[historyIndex.value] === val) {
      return;
    }
    historyStack.value = historyStack.value.slice(0, historyIndex.value + 1);
    historyStack.value.push(val);
    if (historyStack.value.length > maxHistory) {
      historyStack.value.shift();
    }
    historyIndex.value = historyStack.value.length - 1;
  }

  function initHistory(val: string) {
    historyStack.value = [val];
    historyIndex.value = 0;
  }

  const canUndo = computed(() => historyIndex.value > 0);
  const canRedo = computed(() => historyIndex.value >= 0 && historyIndex.value < historyStack.value.length - 1);

  function undo() {
    if (historyIndex.value <= 0) return;
    isHistoryAction.value = true;
    historyIndex.value -= 1;
    onChange(historyStack.value[historyIndex.value]);
    isHistoryAction.value = false;
  }

  function redo() {
    if (historyIndex.value >= historyStack.value.length - 1) return;
    isHistoryAction.value = true;
    historyIndex.value += 1;
    onChange(historyStack.value[historyIndex.value]);
    isHistoryAction.value = false;
  }

  function onInput(val: string) {
    onChange(val);
    if (typingTimer) clearTimeout(typingTimer);
    typingTimer = setTimeout(() => pushHistory(val), 600);
  }

  function beforeMutation(current: string) {
    pushHistory(current);
  }

  return {
    canUndo,
    canRedo,
    undo,
    redo,
    onInput,
    beforeMutation,
    pushHistory,
    initHistory
  };
}
