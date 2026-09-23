<template>
  <main class="app-content" :class="{ 'is-full-height': isFullHeight, 'is-no-padding': isNoPadding }">
    <div class="app-content-body">
      <router-view v-slot="{ Component, route: currentRoute }">
        <transition name="page-switch" mode="out-in" appear>
          <keep-alive :include="cachedViews">
            <component :is="Component" :key="getRouteKey(currentRoute)" />
          </keep-alive>
        </transition>
      </router-view>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

/**
 * 缓存的组件名列表：保活课程 AI 助教、课程空间等长连接/会话级视图，避免菜单切换时流式输出中断与状态重置
 */
const cachedViews = computed(() => {
  return ['CourseAIPage', 'CourseDetail'];
});

function getRouteKey(currentRoute: any) {
  if (currentRoute.meta?.keepAlive || currentRoute.path === '/course/ai') {
    return currentRoute.path;
  }
  return currentRoute.fullPath;
}

const isFullHeight = computed(
  () => !!route.meta?.fullHeight || route.path.startsWith('/course/ai') || route.path.endsWith('/ai')
);
const isNoPadding = computed(
  () => !!route.meta?.noPadding || route.name === 'AssignmentTake' || (route.path.includes('/assignments/') && route.path.endsWith('/take'))
);
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 20px calc(24px + #{$copilot-safe-right}) $page-bottom-spacing;
  background-color: #F5F8FC;
  box-sizing: border-box;
  scrollbar-gutter: stable;

  &.is-full-height {
    padding: 14px 20px 14px;
    display: flex;
    flex-direction: column;
    overflow-y: auto;

    .app-content-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
      height: 100%;
    }
  }

  &.is-no-padding {
    padding: 0 !important;
  }

  .app-content-body {
    width: 100%;
    min-width: 0;
    flex: 0 0 auto;
  }
}
</style>
