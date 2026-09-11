import { ref } from 'vue';

export function useTable() {
  const loading = ref(false);
  return { loading };
}
