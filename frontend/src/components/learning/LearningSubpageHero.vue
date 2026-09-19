<template>
  <PageHeroBanner
    :title="title"
    :subtitle="subtitle"
    :background-variant="variant"
    size="large"
  >
    <template v-if="$slots.actions" #actions>
      <slot name="actions" />
    </template>
    <template v-if="$slots.toolbar" #toolbar>
      <slot name="toolbar" />
    </template>
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </PageHeroBanner>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';

withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
    variant?: 'learning' | 'analytics' | 'ai';
  }>(),
  {
    subtitle: '',
    variant: 'learning'
  }
);
</script>

<style scoped lang="scss">
:deep(.page-hero-banner) {
  margin-bottom: 0;

  &.page-hero-banner--large {
    padding: 18px 28px 14px;
  }
}
</style>
