import { ref, computed } from 'vue';
import { DocumentChunk, ChunkStatsVO, ChunkQueryRequest } from '@/types/knowledge/chunk';
import { getChunks, triggerChunk, getChunkStats } from '@/api/knowledge/chunk';
import { ElMessage } from 'element-plus';

export function useDocumentChunk(initialDocId?: number) {
  const loading = ref(false);
  const rechunking = ref(false);
  const chunks = ref<DocumentChunk[]>([]);
  const stats = ref<ChunkStatsVO>({
    totalChunks: 0,
    indexedChunks: 0,
    pendingChunks: 0,
    failedChunks: 0,
    avgTokens: 0,
    totalTokens: 0
  });

  const selectedDocumentId = ref<number | undefined>(initialDocId);
  const searchKeyword = ref('');
  const statusFilter = ref<string>('ALL');

  // 当前激活的查看切片
  const currentChunk = ref<DocumentChunk | null>(null);
  const viewerVisible = ref(false);

  // 分页状态
  const currentPage = ref(1);
  const pageSize = ref(8);

  const fetchChunks = async () => {
    loading.value = true;
    try {
      const params: ChunkQueryRequest = {
        keyword: searchKeyword.value || undefined,
        status: statusFilter.value === 'ALL' ? undefined : statusFilter.value
      };
      chunks.value = await getChunks(selectedDocumentId.value, params);
    } catch (err: any) {
      console.warn('[useDocumentChunk] fetchChunks failed:', err);
    } finally {
      loading.value = false;
    }
  };

  const fetchStats = async (kbId?: number) => {
    try {
      stats.value = await getChunkStats(kbId);
    } catch (err) {
      console.error(err);
    }
  };

  const handleRechunk = async (docId?: number) => {
    const targetDocId = docId || selectedDocumentId.value;
    if (!targetDocId) {
      ElMessage.warning('请选择需要重新切片的文档');
      return;
    }
    rechunking.value = true;
    try {
      const res = await triggerChunk(targetDocId);
      ElMessage.success(res.message || '重新切片任务已提交');
      await fetchChunks();
      await fetchStats();
    } catch (err: any) {
      ElMessage.error(err.message || '触发切片失败');
    } finally {
      rechunking.value = false;
    }
  };

  const openViewer = (chunk: DocumentChunk) => {
    currentChunk.value = chunk;
    viewerVisible.value = true;
  };

  const closeViewer = () => {
    viewerVisible.value = false;
  };

  const paginatedChunks = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return chunks.value.slice(start, start + pageSize.value);
  });

  const totalCount = computed(() => chunks.value.length);

  return {
    loading,
    rechunking,
    chunks,
    stats,
    selectedDocumentId,
    searchKeyword,
    statusFilter,
    currentChunk,
    viewerVisible,
    currentPage,
    pageSize,
    paginatedChunks,
    totalCount,
    fetchChunks,
    fetchStats,
    handleRechunk,
    openViewer,
    closeViewer
  };
}
