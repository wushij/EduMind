import { Directive, DirectiveBinding } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';

function checkRole(el: HTMLElement, binding: DirectiveBinding) {
  const authStore = useAuthStore();
  const value = binding.value as string | string[] | undefined;
  if (!value) return;
  const roles = Array.isArray(value) ? value : [value];
  if (!authStore.hasAnyRole(roles)) {
    el.parentNode?.removeChild(el);
  }
}

export const vRole: Directive = {
  mounted(el, binding) {
    checkRole(el as HTMLElement, binding);
  },
  updated(el, binding) {
    checkRole(el as HTMLElement, binding);
  }
};
