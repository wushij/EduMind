import { computed, onUnmounted, ref, unref, type MaybeRef } from 'vue';
import { useRoute } from 'vue-router';
import { VectorStoreStats, FailedVectorItem } from '@/types/knowledge/embedding';
import {
  getVectorStats,
  getFailedVectors,
  triggerReindex,
  retryFailedVectors,
  reindexSingleChunk
} from '@/api/knowledge/embedding';
import { ElMessage } from 'element-plus';
import { parseKnowledgeBaseId } from '@/composables/knowledge/useKnowledgeRoute';

function resolveKbId(explicit?: MaybeRef<number | undefined>): number | undefined {
  const fromArg = parseKnowledgeBaseId(unref(explicit));
  if (fromArg) return fromArg;
  const route = useRoute();
  return parseKnowledgeBaseId(route.params.id);
}

export function useEmbeddingIndex(kbIdInput?: MaybeRef<number | undefined>) {
  const kbId = computed(() => resolveKbId(kbIdInput));

  const loading = ref(false);
  const reindexing = ref(false);
  const retrying = ref(false);
  const retryingChunkId = ref<number | string | null>(null);

  const stats = ref<VectorStoreStats>({
    engine: 'Milvus',
    engineVersion: 'dev',
    connectionStatus: 'ONLINE',
    collectionName: '',
    dimensions: 1536,
    metricType: 'COSINE',
    indexType: 'HNSW',
    totalVectors: 0,
    expectedVectors: 0,
    indexHealthScore: 0,
    avgQueryLatencyMs: 0,
    lastIndexedAt: ''
  });

  const failedItems = ref<FailedVectorItem[]>([]);
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  };

  const startPolling = () => {
    stopPolling();
    let pollCount = 0;
    pollTimer = setInterval(async () => {
      pollCount++;
      const id = kbId.value;
      if (!id || pollCount > 60) {
        stopPolling();
        return;
      }
      try {
        const [statsRes, failedRes] = await Promise.all([getVectorStats(id), getFailedVectors(id)]);
        stats.value = statsRes;
        failedItems.value = failedRes;
        if (
          statsRes.totalVectors >= statsRes.expectedVectors &&
          statsRes.expectedVectors > 0 &&
          statsRes.connectionStatus !== 'DEGRADED'
        ) {
          stopPolling();
        }
      } catch {
        stopPolling();
      }
    }, 2500);
  };

  const fetchStatus = async () => {
    const id = kbId.value;
    if (!id) {
      ElMessage.error('无效的知识库 ID');
      return;
    }
    loading.value = true;
    try {
      const [statsRes, failedRes] = await Promise.all([getVectorStats(id), getFailedVectors(id)]);
      stats.value = statsRes;
      failedItems.value = failedRes;
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '获取向量状态失败');
    } finally {
      loading.value = false;
    }
  };

  const startReindex = async (mode: 'FULL' | 'INCREMENTAL' = 'FULL') => {
    const id = kbId.value;
    if (!id) return;
    reindexing.value = true;
    try {
      const res = await triggerReindex(id, mode);
      ElMessage.success(res.message);
      await fetchStatus();
      startPolling();
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '启动索引失败');
    } finally {
      reindexing.value = false;
    }
  };

  const handleRetryFailed = async () => {
    const id = kbId.value;
    if (!id) return;
    retrying.value = true;
    try {
      const res = await retryFailedVectors(id);
      ElMessage.success(res.message);
      await fetchStatus();
      startPolling();
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '重试失败切片异常');
    } finally {
      retrying.value = false;
    }
  };

  const handleRetrySingleChunk = async (chunkId: number | string) => {
    const id = kbId.value;
    if (!id) return;
    retryingChunkId.value = chunkId;
    try {
      const res = await reindexSingleChunk(id, Number(chunkId));
      ElMessage.success(res.message);
      await fetchStatus();
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '重试单切片失败');
    } finally {
      retryingChunkId.value = null;
    }
  };

  onUnmounted(() => {
    stopPolling();
  });

  return {
    loading,
    reindexing,
    retrying,
    retryingChunkId,
    stats,
    failedItems,
    fetchStatus,
    startReindex,
    handleRetryFailed,
    handleRetrySingleChunk
  };
}
