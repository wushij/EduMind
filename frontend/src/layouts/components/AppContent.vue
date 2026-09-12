<template>
  <main class="app-content">
    <div class="app-content-body">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
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

  .app-content-body {
    width: 100%;
    min-width: 0;
  }
}
</style>
