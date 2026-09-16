<template>
  <button
    type="button"
    class="table-action-pill"
    :class="`table-action-pill--${variant}`"
    :disabled="disabled"
    @click="emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'danger' | 'warning' | 'success';
    disabled?: boolean;
  }>(),
  {
    variant: 'primary',
    disabled: false
  }
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();
</script>
