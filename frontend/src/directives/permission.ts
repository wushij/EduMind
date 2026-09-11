import { Directive, DirectiveBinding } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const authStore = useAuthStore();
  const value = binding.value as string | string[] | undefined;
  if (!value) return;
  const codes = Array.isArray(value) ? value : [value];
  if (!authStore.hasAnyPermission(codes)) {
    el.parentNode?.removeChild(el);
  }
}

export const vPermission: Directive = {
  mounted(el, binding) {
    checkPermission(el as HTMLElement, binding);
  },
  updated(el, binding) {
    checkPermission(el as HTMLElement, binding);
  }
};
