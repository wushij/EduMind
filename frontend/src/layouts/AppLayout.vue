<template>
  <div class="app-layout">
    <AppSidebar :is-collapsed="isCollapsed" />
    <div class="layout-body">
      <BroadcastMarqueeBanner />
      <AppHeader :is-collapsed="isCollapsed" @toggle-sidebar="toggle" />
      <AppContent />
    </div>
    <GlobalAssistantDrawer />
    <BroadcastAlertModal />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { cleanupOrphanMermaidDom } from '@/utils/markdown';
import AppSidebar from './components/AppSidebar.vue';
import AppHeader from './components/AppHeader.vue';
import AppContent from './components/AppContent.vue';
import GlobalAssistantDrawer from '@/components/ai/GlobalAssistantDrawer.vue';
import BroadcastAlertModal from '@/components/notification/BroadcastAlertModal.vue';
import BroadcastMarqueeBanner from '@/components/notification/BroadcastMarqueeBanner.vue';
import { useSidebar } from '@/composables/layout/useSidebar';
import { useBroadcastPush } from '@/composables/notification/useBroadcastPush';
import { useNotifyStore } from '@/stores/notification/notify';
import { tokenUtil } from '@/core/auth/token';

const { isCollapsed, toggle } = useSidebar();
const notifyStore = useNotifyStore();
const { setupListener, checkUnreadPriority } = useBroadcastPush();
const route = useRoute();

onMounted(() => {
  cleanupOrphanMermaidDom();
  if (tokenUtil.get()) {
    notifyStore.startWs();
    setupListener();
    void checkUnreadPriority();
  }
});

onUnmounted(() => {
  notifyStore.stopWs();
});

watch(
  () => route.fullPath,
  () => cleanupOrphanMermaidDom()
);
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
