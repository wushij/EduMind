<template>
<!-- 悬浮微圆钮：轻巧触底按钮 (不遮挡正文) -->
            <transition name="fade">
              <button
                v-if="showScrollToBottom"
                class="scroll-bottom-btn"
                type="button"
                aria-label="回到底部"
                title="查看最新回复"
                @click="scrollToBottomSmooth"
              >
                <svg class="scroll-bottom-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" class="scroll-bottom-svg__bg" />
                  <path d="M12 7.5v6.5" class="scroll-bottom-svg__shaft" />
                  <path d="M8.8 11.8 12 15 15.2 11.8" class="scroll-bottom-svg__head" />
                </svg>
              </button>
            </transition>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const { showScrollToBottom, scrollToBottomSmooth } = inject(globalAssistantUiKey)!;
</script>
