import { ref } from 'vue';

export function usePagination(defaultSize = 10) {
  const pageNum = ref(1);
  const pageSize = ref(defaultSize);
  const total = ref(0);
  return { pageNum, pageSize, total };
}
