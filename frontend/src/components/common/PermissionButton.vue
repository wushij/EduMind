<template>
  <slot v-if="visible" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';

const props = withDefaults(
  defineProps<{
    permission?: string | string[];
    role?: string | string[];
  }>(),
  {}
);

const authStore = useAuthStore();

const visible = computed(() => {
  if (props.role) {
    const roles = Array.isArray(props.role) ? props.role : [props.role];
    if (!authStore.hasAnyRole(roles)) return false;
  }
  if (props.permission) {
    const codes = Array.isArray(props.permission) ? props.permission : [props.permission];
    if (!authStore.hasAnyPermission(codes)) return false;
  }
  return true;
});
</script>
