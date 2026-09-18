import { ref, watch, nextTick, type Ref } from 'vue';
import { bindMarkdownCodeCopy } from '@/utils/ai/chat-markdown';
import {
  getDistanceToBottom,
  shouldPauseAutoFollow,
  shouldResumeAutoFollow
} from '@/utils/ai/stream-scroll';

export function useAIStreamScrollFollow(options: {
  streaming: Ref<boolean>;
  streamingRenderedHtml: Ref<string>;
  streamingReasoning: Ref<string>;
  messagesScrollRef: Ref<HTMLDivElement | null>;
  streamAnchorRef: Ref<HTMLDivElement | null>;
}) {
  const { streaming, streamingRenderedHtml, streamingReasoning, messagesScrollRef, streamAnchorRef } =
    options;

  const showScrollToBottom = ref(false);
  const userScrolledUp = ref(false);
  let followAnimationFrameId: number | null = null;

  function pauseAutoScrollFollow() {
    userScrolledUp.value = true;
    showScrollToBottom.value = true;
  }

  function scheduleFollowStreamOutput() {
    if (!streaming.value || userScrolledUp.value || showScrollToBottom.value) return;
    if (followAnimationFrameId) return;
    followAnimationFrameId = requestAnimationFrame(() => {
      followAnimationFrameId = null;
      nextTick(() => {
        if (!streaming.value || userScrolledUp.value || showScrollToBottom.value) return;
        if (messagesScrollRef.value) {
          messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
          bindMarkdownCodeCopy(messagesScrollRef.value);
        }
      });
    });
  }

  function handleViewportScroll(e: Event) {
    const el = e.target as HTMLElement;
    if (!el) return;
    const distanceToBottom = getDistanceToBottom(el);
    if (shouldPauseAutoFollow(distanceToBottom)) {
      userScrolledUp.value = true;
      showScrollToBottom.value = true;
    } else if (shouldResumeAutoFollow(distanceToBottom)) {
      userScrolledUp.value = false;
      showScrollToBottom.value = false;
    }
  }

  function scrollToBottomInstant() {
    nextTick(() => {
      requestAnimationFrame(() => {
        if (messagesScrollRef.value) {
          messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
          bindMarkdownCodeCopy(messagesScrollRef.value);
        }
      });
    });
  }

  function scrollToBottomSmooth() {
    userScrolledUp.value = false;
    showScrollToBottom.value = false;
    nextTick(() => {
      if (streamAnchorRef.value) {
        streamAnchorRef.value.scrollIntoView({ behavior: 'smooth', block: 'end' });
      } else if (messagesScrollRef.value) {
        messagesScrollRef.value.scrollTo({
          top: messagesScrollRef.value.scrollHeight,
          behavior: 'smooth'
        });
      }
    });
  }

  watch(
    () => streamingRenderedHtml.value,
    () => {
      if (streaming.value) {
        scheduleFollowStreamOutput();
      }
    }
  );

  watch(
    () => streamingReasoning.value,
    () => {
      if (streaming.value) {
        scheduleFollowStreamOutput();
      }
    }
  );

  return {
    showScrollToBottom,
    userScrolledUp,
    pauseAutoScrollFollow,
    handleViewportScroll,
    scrollToBottomInstant,
    scrollToBottomSmooth,
    scheduleFollowStreamOutput
  };
}
