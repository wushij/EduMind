<template>
  <ProfilePageHero
    title="AI 智能组卷"
    subtitle="基于课程知识图谱与难度分布模型，快速生成或精细编排符合教学目标的试卷。"
  >
    <template #actions>
      <button type="button" class="hero-pill-btn is-outline" @click="$emit('back')">
        返回 AI 广场
      </button>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';

defineEmits<{
  back: [];
}>();
</script>
