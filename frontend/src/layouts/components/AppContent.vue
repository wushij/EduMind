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
.app-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px;
  background-color: #F5F8FC;
  box-sizing: border-box;

  .app-content-body {
    flex: 1 1 auto;
    min-height: 0;
    min-width: 0;
  }
}
</style>
