<template>
  <div class="module-page-hero">
    <div v-if="$slots.nav" class="module-page-hero__nav">
      <slot name="nav" />
    </div>

    <div class="module-page-hero__content" :class="{ 'module-page-hero__content--single': !$slots.actions }">
      <div class="module-page-hero__main">
        <div class="module-page-hero__title-row">
          <div class="module-page-hero__icon-orb" aria-hidden="true">
            <el-icon><component :is="icon" /></el-icon>
          </div>
          <div class="module-page-hero__meta">
            <div class="module-page-hero__badges">
              <h1 class="module-page-hero__title">{{ title }}</h1>
              <span v-if="badge" class="module-page-hero__badge">{{ badge }}</span>
            </div>
            <p v-if="description" class="module-page-hero__desc">{{ description }}</p>
            <div v-if="$slots.metaExtra" class="module-page-hero__meta-extra">
              <slot name="metaExtra" />
            </div>
          </div>
        </div>
      </div>
      <div v-if="$slots.actions" class="module-page-hero__actions">
        <div class="module-page-hero__toolbar">
          <slot name="actions" />
        </div>
      </div>
    </div>

    <div v-if="$slots.stats" class="module-page-hero__stats">
      <slot name="stats" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue';

defineProps<{
  icon: Component;
  title: string;
  badge?: string;
  description?: string;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
