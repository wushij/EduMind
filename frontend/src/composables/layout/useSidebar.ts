import { ref } from 'vue';

export function useSidebar() {
  const isCollapsed = ref(false);
  const toggle = () => {
    isCollapsed.value = !isCollapsed.value;
  };
  return { isCollapsed, toggle };
}
