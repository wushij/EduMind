<template>
  <div class="app-layout">
    <AppSidebar :is-collapsed="isCollapsed" />
    <div class="layout-body">
      <AppHeader :is-collapsed="isCollapsed" @toggle-sidebar="toggle" />
      <AppContent />
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './components/AppSidebar.vue';
import AppHeader from './components/AppHeader.vue';
import AppContent from './components/AppContent.vue';
import { useSidebar } from '@/composables/layout/useSidebar';

const { isCollapsed, toggle } = useSidebar();
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;

  .layout-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}
</style>
