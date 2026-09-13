<template>
  <main class="app-content">
    <div class="app-content-body">
      <router-view v-slot="{ Component, route }">
        <transition name="page-switch" mode="out-in" appear>
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </div>
  </main>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px $page-bottom-spacing;
  background-color: #F5F8FC;
  box-sizing: border-box;
  scrollbar-gutter: stable;

  .app-content-body {
    width: 100%;
    min-width: 0;
    min-height: var(--page-min-height, calc(100vh - 64px - 40px - 48px));
  }
}
</style>
