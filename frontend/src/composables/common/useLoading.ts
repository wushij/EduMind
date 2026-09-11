import { ref } from 'vue';

export function useLoading(init = false) {
  const loading = ref(init);
  return { loading };
}
