<template>
  <teleport to="body">
    <transition name="copilot-drawer">
      <div
        v-if="drawerVisible"
        class="copilot-drawer-overlay"
        @click.self="onClose"
        @wheel.self.prevent
        @touchmove.self.prevent
      >
        <div class="copilot-drawer-panel" :class="{ 'is-wide-panel': isWideMode }" @wheel.stop>
          <div class="panel-glow-border" />
          <slot />
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
defineProps<{
  drawerVisible: boolean;
  isWideMode: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

function onClose() {
  emit('close');
}
</script>
