<template>
  <div class="ai-cognitive-thinking-slot">
    <AiCognitiveThinkingPanel
      v-if="active"
      :active="active"
      v-bind="preset"
      :show-footer-actions="showFooterActions"
      :abort-label="abortLabel"
      @abort="$emit('abort')"
    />
    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import {
  AI_COGNITIVE_THINKING_PRESETS,
  type AiCognitiveThinkingPreset
} from '@/constants/ai/cognitive-thinking';

const props = withDefaults(
  defineProps<{
    active: boolean;
    /** 对应 cognitive-thinking.ts 中的预设键 */
    presetKey: keyof typeof AI_COGNITIVE_THINKING_PRESETS;
    showFooterActions?: boolean;
    abortLabel?: string;
    /** 完全自定义预设时传入，优先级高于 presetKey */
    presetOverride?: AiCognitiveThinkingPreset;
  }>(),
  {
    showFooterActions: false,
    abortLabel: '中止'
  }
);

defineEmits<{
  abort: [];
}>();

const preset = computed(() => props.presetOverride ?? AI_COGNITIVE_THINKING_PRESETS[props.presetKey]);
</script>

<style scoped lang="scss">
.ai-cognitive-thinking-slot {
  width: 100%;
}
</style>
