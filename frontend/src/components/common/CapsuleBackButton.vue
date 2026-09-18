<template>
  <button type="button" class="capsule-back-btn" @click="emit('click')">
    <svg viewBox="0 0 24 24" class="back-arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
      <line x1="19" y1="12" x2="5" y2="12" />
      <polyline points="12 19 5 12 12 5" />
    </svg>
    <span>{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
  }>(),
  {
    label: '返回'
  }
);

const emit = defineEmits<{ click: [] }>();
</script>

<style scoped lang="scss">
.capsule-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 16px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);

  .back-arrow-svg {
    width: 15px;
    height: 15px;
  }

  &:hover {
    color: #1677ff;
    border-color: #93c5fd;
    background: #fff;
    transform: translateX(-2px);
  }
}
</style>
