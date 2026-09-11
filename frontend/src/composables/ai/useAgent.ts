import { ref } from 'vue';

export function useAgent() {
  const status = ref('idle');
  return { status };
}
