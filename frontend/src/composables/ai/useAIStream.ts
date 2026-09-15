import { ref, computed } from 'vue';
import { SSEClient } from '@/core/sse/client';
import { splitCopilotStream, cleanReasoningText } from '@/utils/ai/copilot-stream-split';
import { useStreamingMarkdown } from '@/composables/ai/useStreamingMarkdown';
import { useAIStreamScrollFollow } from '@/composables/ai/useAIStreamScrollFollow';
import type { CitationItem } from '@/types/ai/assistant';
import { isThinkingPanelHidden } from '@/utils/ai/thinking-display';
import {
  type ChatMessage,
  type ChatSession,
  type StreamOptions,
  type LoadSessionsOptions
} from '@/services/ai/stream-service';
import { getDefaultReasoningFolded } from '@/utils/ai/thinking-display';
import { createResetStreamingState } from '@/composables/ai/useAIStreamStreamingReset';
import { createAIStreamSessionActions } from '@/composables/ai/useAIStreamSessions';
import { createAIStreamOperations } from '@/composables/ai/useAIStreamOperations';

export type { ChatMessage, ChatSession, StreamOptions, LoadSessionsOptions };

export function useAIStream() {
  const sseClient = new SSEClient();
  const streaming = ref(false);
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<string>('');
  const messages = ref<ChatMessage[]>([]);

  // 滚动容器与物理底端锚点 (用于 rAF 平滑向上跟随贴底)
  const messagesScrollRef = ref<HTMLDivElement | null>(null);
  const streamAnchorRef = ref<HTMLDivElement | null>(null);

  const getInitialReasoningFolded = getDefaultReasoningFolded;

  // 流式过程中的临时状态 (对标全局副驾驶独立顶级 ref)
  const streamingReasoning = ref('');
  const streamingContent = ref('');
  const streamingCitations = ref<CitationItem[]>([]);
  const isReasoningFolded = ref(getInitialReasoningFolded());
  const isReasoningActive = ref(false);
  const streamPhaseMessage = ref('');
  const answerStreamStarted = ref(false);
  const followUpPrompts = ref<string[]>([]);
  /** 用户手动点击停止后置位，避免 fallback / 重载消息覆盖已结算内容 */
  const userStoppedGeneration = ref(false);
  const activeStreamCourseId = ref<number | undefined>();

  // 增量高效流式 Markdown 渲染引擎
  const {
    renderedHtml: streamingRenderedHtml,
    appendChunk: appendStreamingMarkdown,
    finish: finishStreamingMarkdown,
    reset: resetStreamingMarkdown
  } = useStreamingMarkdown();

  const streamingSplit = computed(() => splitCopilotStream(streamingContent.value));
  const streamingThinkingBody = computed(() => streamingSplit.value.thinking);
  const streamingAnswerBody = computed(() => streamingSplit.value.answer);
  const streamingThinkingDisplay = computed(() => {
    const raw = streamingReasoning.value || streamingThinkingBody.value;
    return cleanReasoningText(raw);
  });

  const {
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll,
    scrollToBottomInstant,
    scrollToBottomSmooth,
    scheduleFollowStreamOutput
  } = useAIStreamScrollFollow({
    streaming,
    streamingRenderedHtml,
    streamingReasoning,
    messagesScrollRef,
    streamAnchorRef
  });

  const resetStreamingState = createResetStreamingState({
    streamingReasoning,
    streamingContent,
    streamingCitations,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    answerStreamStarted,
    resetStreamingMarkdown
  });

  const sessionActions = createAIStreamSessionActions({
    sessions,
    currentSessionId,
    messages,
    followUpPrompts,
    streaming,
    resetStreamingState,
    scrollToBottomInstant
  });

  const {
    loadSessions,
    loadMessages,
    createNewSession,
    startNewChat,
    switchSession,
    removeSession,
    renameSession,
    syncSessionTitleIfDefaultLocal,
    refreshSessionsMeta
  } = sessionActions;

  const { sendMessage, stopStream, handleRegenerate, confirmDeleteMessage } =
    createAIStreamOperations({
      sseClient,
      streaming,
      sessions,
      currentSessionId,
      messages,
      messagesScrollRef,
      streamingReasoning,
      streamingContent,
      streamingCitations,
      isReasoningFolded,
      isReasoningActive,
      streamPhaseMessage,
      answerStreamStarted,
      followUpPrompts,
      userStoppedGeneration,
      activeStreamCourseId,
      streamingThinkingBody,
      streamingAnswerBody,
      appendStreamingMarkdown,
      finishStreamingMarkdown,
      resetStreamingState,
      scrollToBottomInstant,
      scheduleFollowStreamOutput,
      syncSessionTitleIfDefaultLocal,
      refreshSessionsMeta,
      loadSessions,
      createNewSession
    });

  return {
    sessions,
    currentSessionId,
    messages,
    streaming,
    messagesScrollRef,
    streamAnchorRef,
    // 流式状态
    streamingReasoning,
    streamingContent,
    streamingCitations,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    followUpPrompts,
    showThinkingPanel: computed(() => !isThinkingPanelHidden()),
    // 方法
    loadSessions,
    loadMessages,
    sendMessage,
    stopStream,
    createNewSession,
    startNewChat,
    switchSession,
    deleteSession: removeSession,
    renameSession,
    handleRegenerate,
    confirmDeleteMessage,
    scrollToBottomSmooth,
    scrollToBottomInstant,
    scheduleFollowStreamOutput,
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll
  };
}
