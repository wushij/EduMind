import { onMounted, reactive, ref, type Ref } from 'vue';
import type { PageQuery, PageResult } from '@/types/common/page';

export function useTableQuery<T, Q extends PageQuery = PageQuery>(
  fetcher: (query: Q) => Promise<PageResult<T>>,
  extra?: Partial<Omit<Q, keyof PageQuery>>,
  options?: { immediate?: boolean; defaultPageSize?: number }
) {
  const loading = ref(false);
  const tableData = ref<T[]>([]) as Ref<T[]>;
  const total = ref(0);

  const initialExtra = extra ? { ...extra } : {};

  const query = reactive({
    pageNum: 1,
    pageSize: options?.defaultPageSize ?? 10,
    keyword: '',
    ...initialExtra
  }) as Q;

  async function fetchData() {
    loading.value = true;
    try {
      const page = await fetcher(query);
      tableData.value = page?.list || [];
      total.value = Number(page?.total || 0);
    } finally {
      loading.value = false;
    }
  }

  function search() {
    query.pageNum = 1;
    return fetchData();
  }

  function resetQuery() {
    query.pageNum = 1;
    query.pageSize = options?.defaultPageSize ?? 10;
    query.keyword = '';
    Object.assign(query, initialExtra);
    return fetchData();
  }

  function onPageSizeChange() {
    query.pageNum = 1;
    return fetchData();
  }

  if (options?.immediate !== false) {
    onMounted(fetchData);
  }

  return {
    loading,
    tableData,
    total,
    query,
    fetchData,
    search,
    resetQuery,
    onPageSizeChange
  };
}
