<template>
  <ProfilePageHero
    title="AI 智能出题"
    subtitle="根据课程知识点、难度等级与题型要求，分步配置并一键生成高质量试题。"
  >
    <template #actions>
      <button type="button" class="hero-pill-btn is-outline" @click="$emit('back')">
        返回 AI 广场
      </button>
    </template>
    <template #footer>
      <AiWizardStepBar
        :steps="steps"
        :current-step="currentStep"
        @go-to-step="$emit('go-to-step', $event)"
      />
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import AiWizardStepBar from '@/components/ai/common/AiWizardStepBar.vue';

defineProps<{
  steps: { index: number; name: string }[];
  currentStep: number;
}>();

defineEmits<{
  back: [];
  'go-to-step': [index: number];
}>();
</script>
