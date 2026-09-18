import { ref, computed, watch } from 'vue';
import { DocumentChunk, ChunkStatsVO, ChunkQueryRequest } from '@/types/knowledge/chunk';
import { KBDocument } from '@/types/knowledge/document';
import { getDocuments } from '@/api/knowledge/document';
import { getChunks, triggerChunk, getChunkStats } from '@/api/knowledge/chunk';
import { ElMessage } from 'element-plus';

function deriveStatsFromChunks(list: DocumentChunk[]): ChunkStatsVO {
  const total = list.length;
  const indexed = list.filter((c) => c.status === 'INDEXED').length;
  const pending = list.filter((c) => c.status === 'PENDING').length;
  const failed = list.filter((c) => c.status === 'INDEX_FAILED').length;
  const totalTokens = list.reduce((sum, c) => sum + (c.tokenCount || 0), 0);
  return {
    totalChunks: total,
    indexedChunks: indexed,
    pendingChunks: pending,
    failedChunks: failed,
    totalTokens,
    avgTokens: total > 0 ? Math.round(totalTokens / total) : 0
  };
}

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

  const knowledgeBaseId = ref<number | undefined>();
  const selectedDocumentId = ref<number | undefined>(initialDocId);
  const searchKeyword = ref('');
  const statusFilter = ref<string>('ALL');

  // 当前激活的查看切片
  const currentChunk = ref<DocumentChunk | null>(null);
  const viewerVisible = ref(false);

  // 分页状态
  const currentPage = ref(1);
  const pageSize = ref(10);

  const fetchChunks = async () => {
    if (!selectedDocumentId.value) {
      chunks.value = [];
      return;
    }
    loading.value = true;
    try {
      const params: ChunkQueryRequest = {
        keyword: searchKeyword.value || undefined,
        status: statusFilter.value === 'ALL' ? undefined : statusFilter.value
      };
      chunks.value = await getChunks(selectedDocumentId.value, params);
      currentPage.value = 1;
    } catch {
      chunks.value = [];
      ElMessage.error('加载切片列表失败');
    } finally {
      loading.value = false;
    }
  };

  watch(statusFilter, () => {
    void fetchChunks();
  });

  const fetchStats = async (kbId?: number) => {
    const targetKbId = kbId ?? knowledgeBaseId.value;
    const fallback = deriveStatsFromChunks(chunks.value);
    if (!targetKbId) {
      if (fallback.totalChunks > 0) {
        stats.value = fallback;
      }
      return;
    }
    try {
      const apiStats = await getChunkStats(targetKbId);
      const apiEmpty =
        (apiStats.totalChunks ?? 0) === 0 && (apiStats.indexedChunks ?? 0) === 0;
      stats.value =
        apiEmpty && fallback.totalChunks > 0
          ? { ...apiStats, ...fallback }
          : apiStats;
    } catch {
      if (fallback.totalChunks > 0) {
        stats.value = fallback;
      }
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
      await fetchStats(knowledgeBaseId.value);
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

  const documents = ref<KBDocument[]>([]);

  async function loadDocuments(kbId?: number) {
    if (!kbId) {
      documents.value = [];
      return;
    }
    try {
      const res = await getDocuments(kbId);
      documents.value = res.data || [];
      if (documents.value.length > 0 && !selectedDocumentId.value) {
        selectedDocumentId.value = documents.value[0].id;
      }
    } catch {
      documents.value = [];
    }
  }

  async function initializeChunksPage(kbId?: number) {
    if (kbId) {
      knowledgeBaseId.value = kbId;
      await loadDocuments(kbId);
      if (selectedDocumentId.value) {
        await fetchChunks();
      }
      await fetchStats(kbId);
    } else if (selectedDocumentId.value) {
      await fetchChunks();
    }
  }

  function resetFilters() {
    selectedDocumentId.value = undefined;
    searchKeyword.value = '';
    statusFilter.value = 'ALL';
    fetchChunks();
  }

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
    documents,
    fetchChunks,
    fetchStats,
    loadDocuments,
    initializeChunksPage,
    resetFilters,
    handleRechunk,
    openViewer,
    closeViewer
  };
}
