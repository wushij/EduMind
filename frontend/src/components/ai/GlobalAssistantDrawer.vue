<template>
  <div class="global-assistant-root">
    <GlobalAssistantTrigger
      :active="drawerVisible"
      :is-streaming="isStreaming"
      @toggle="toggleDrawer"
    />

    <GlobalAssistantDrawerShell
      :drawer-visible="drawerVisible"
      :is-wide-mode="isWideMode"
      @close="drawerVisible = false"
    >
      <GlobalAssistantHeader />
      <GlobalAssistantContextBanner />
      <GlobalAssistantHistoryPanel />
      <GlobalAssistantMessageList />
      <GlobalAssistantScrollFab />
      <GlobalAssistantComposer />
    </GlobalAssistantDrawerShell>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, provide } from 'vue';
import logoImg from '@/assets/images/logo.png';
import GlobalAssistantTrigger from '@/components/ai/GlobalAssistantTrigger.vue';
import GlobalAssistantDrawerShell from '@/components/ai/global-assistant/GlobalAssistantDrawerShell.vue';
import GlobalAssistantHeader from '@/components/ai/global-assistant/GlobalAssistantHeader.vue';
import GlobalAssistantContextBanner from '@/components/ai/global-assistant/GlobalAssistantContextBanner.vue';
import GlobalAssistantHistoryPanel from '@/components/ai/global-assistant/GlobalAssistantHistoryPanel.vue';
import GlobalAssistantMessageList from '@/components/ai/global-assistant/GlobalAssistantMessageList.vue';
import GlobalAssistantScrollFab from '@/components/ai/global-assistant/GlobalAssistantScrollFab.vue';
import GlobalAssistantComposer from '@/components/ai/global-assistant/GlobalAssistantComposer.vue';
import { useGlobalAssistant } from '@/composables/ai/useGlobalAssistant';
import { renderChatMarkdown } from '@/utils/ai/chat-markdown';
import { useAuthStore } from '@/stores/auth/auth';
import { DEFAULT_AVATAR } from '@/constants/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';
import { resolveReasoningFolded } from '@/utils/ai/thinking-display';
import {
  globalAssistantUiKey,
  type GlobalAssistantUiContext
} from '@/components/ai/global-assistant/global-assistant-ui-key';

const authStore = useAuthStore();
const userAvatarBroken = ref(false);
const userAvatarSrc = computed(() => {
  const avatar = authStore.currentUser?.avatar;
  return normalizeAvatarUrl(avatar) || DEFAULT_AVATAR;
});
const userAvatarFallback = computed(() => {
  const name = authStore.currentUser?.realName || authStore.currentUser?.username || '我';
  return name.trim().slice(0, 1) || '我';
});

watch(
  () => authStore.currentUser?.avatar,
  () => {
    userAvatarBroken.value = false;
  }
);

const isWideMode = ref(false);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

const ga = useGlobalAssistant();
const {
  drawerVisible,
  inputContent,
  isStreaming,
  messages,
  conversationId,
  messagesScrollRef,
  streamAnchorRef,
  showScrollToBottom,
  activeCourseLabel,
  manualGlobalScope,
  toggleScopeMode,
  presetChips,
  followUpPrompts,
  showThinkingPanel,
  isHistoryPanelOpen,
  isSessionLoading,
  sessions,
  toggleHistoryPanel,
  selectSession,
  confirmDeleteSession,
  confirmClearAllSessions,
  formatSessionTime,
  streamingIntent,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  toggleDrawer,
  clearMessages,
  startNewSession,
  handleSubmit,
  handleSendPrompt,
  stopStreaming,
  handleRegenerate,
  confirmDeleteMessage,
  copyMessage,
  jumpToCitation,
  handleNavigate,
  scrollToBottomSmooth,
  scrollToBottomInstant,
  pauseAutoScrollFollow,
  handleViewportScroll,
  getIntentTagType,
  formatMatchScore
} = ga;

watch(drawerVisible, (visible) => {
  if (visible) {
    scrollToBottomInstant();
    nextTick(() => {
      textareaRef.value?.focus();
    });
  }
});

const isMultiline = computed(() => {
  const text = inputContent.value || '';
  return text.includes('\n') || text.length > 36;
});

function handleInput() {
  nextTick(() => {
    if (!textareaRef.value) return;
    textareaRef.value.style.height = 'auto';
    const scrollH = textareaRef.value.scrollHeight;
    textareaRef.value.style.height = `${Math.min(Math.max(scrollH, 24), 120)}px`;
  });
}

function handleTextareaKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    if (e.shiftKey) {
      return;
    }
    e.preventDefault();
    handleSubmit();
  }
}

const uiContext: GlobalAssistantUiContext = {
  logoImg,
  isWideMode,
  userAvatarSrc,
  userAvatarBroken,
  userAvatarFallback,
  isMultiline,
  textareaRef,
  handleInput,
  handleTextareaKeydown,
  drawerVisible,
  inputContent,
  isStreaming,
  messages,
  conversationId,
  messagesScrollRef,
  streamAnchorRef,
  showScrollToBottom,
  activeCourseLabel,
  manualGlobalScope,
  toggleScopeMode,
  presetChips,
  followUpPrompts,
  showThinkingPanel,
  isHistoryPanelOpen,
  isSessionLoading,
  sessions,
  toggleHistoryPanel,
  selectSession,
  confirmDeleteSession,
  confirmClearAllSessions,
  formatSessionTime,
  streamingIntent,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  clearMessages,
  startNewSession,
  handleSubmit,
  handleSendPrompt,
  stopStreaming,
  handleRegenerate,
  confirmDeleteMessage,
  copyMessage,
  jumpToCitation,
  handleNavigate,
  scrollToBottomSmooth,
  pauseAutoScrollFollow,
  handleViewportScroll,
  getIntentTagType,
  formatMatchScore,
  renderChatMarkdown,
  resolveReasoningFolded
};

provide(globalAssistantUiKey, uiContext);
</script>

<style lang="scss">
@import '@/components/ai/global-assistant/drawer-panel-styles.scss';

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
