import type { Ref } from 'vue';
import type { CitationItem } from '@/types/ai/assistant';
import { getDefaultReasoningFolded } from '@/utils/ai/thinking-display';

export type AIStreamStreamingResetDeps = {
  streamingReasoning: Ref<string>;
  streamingContent: Ref<string>;
  streamingCitations: Ref<CitationItem[]>;
  isReasoningFolded: Ref<boolean>;
  isReasoningActive: Ref<boolean>;
  streamPhaseMessage: Ref<string>;
  answerStreamStarted: Ref<boolean>;
  resetStreamingMarkdown: () => void;
};

export function createResetStreamingState(deps: AIStreamStreamingResetDeps) {
  const getInitialReasoningFolded = getDefaultReasoningFolded;

  return function resetStreamingState() {
    deps.streamingReasoning.value = '';
    deps.streamingContent.value = '';
    deps.streamingCitations.value = [];
    deps.isReasoningFolded.value = getInitialReasoningFolded();
    deps.isReasoningActive.value = false;
    deps.streamPhaseMessage.value = '';
    deps.answerStreamStarted.value = false;
    deps.resetStreamingMarkdown();
  };
}
