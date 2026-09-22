import { get, post } from '@/core/http/request';
import { VectorStoreStats, FailedVectorItem } from '@/types/knowledge/embedding';

interface IndexErrorItemVO {
  chunkId?: number;
  chunkIndex?: number;
  documentId?: number;
  documentName?: string;
  snippet?: string;
  errorCode?: string;
  message?: string;
  retryCount?: number;
  failedAt?: string;
}

interface IndexStatusVO {
  status?: string;
  totalChunks?: number;
  indexedChunks?: number;
  failedChunks?: number;
  embeddingModel?: string;
  dimensions?: number;
  engine?: string;
  engineVersion?: string;
  connectionStatus?: 'ONLINE' | 'DEGRADED' | 'OFFLINE';
  collectionName?: string;
  indexType?: string;
  metricType?: string;
  avgQueryLatencyMs?: number;
  startedAt?: string;
  errors?: IndexErrorItemVO[];
}

function mapIndexStatusToStats(status: IndexStatusVO, kbId: number): VectorStoreStats {
  const total = status.totalChunks ?? 0;
  const indexed = status.indexedChunks ?? 0;
  const health = total > 0 ? Math.round((indexed / total) * 100) : 0;
  return {
    engine: status.engine || 'InMemory',
    engineVersion: status.engineVersion || 'dev',
    connectionStatus: status.connectionStatus || (status.status === 'INDEX_FAILED' ? 'DEGRADED' : 'ONLINE'),
    collectionName: status.collectionName || `kb_${kbId}_vectors`,
    dimensions: status.dimensions || 1536,
    metricType: status.metricType || 'COSINE',
    indexType: status.indexType || 'HNSW',
    totalVectors: indexed,
    expectedVectors: total,
    indexHealthScore: health,
    avgQueryLatencyMs: status.avgQueryLatencyMs ?? 0,
    lastIndexedAt: status.startedAt || ''
  };
}

export const getVectorStats = async (kbId?: number): Promise<VectorStoreStats> => {
  const id = kbId || 0;
  if (!id) {
    return mapIndexStatusToStats({}, 0);
  }
  const res = await get<IndexStatusVO>(`/knowledge-bases/${id}/index/status`);
  return mapIndexStatusToStats(res?.data ?? {}, id);
};

export const getFailedVectors = async (kbId?: number): Promise<FailedVectorItem[]> => {
  const id = kbId || 0;
  if (!id) {
    return [];
  }
  const res = await get<IndexStatusVO>(`/knowledge-bases/${id}/index/status`);
  const errors = res?.data?.errors || [];
  return errors.map((item, idx) => ({
    id: item.chunkId ?? idx,
    chunkIndex: item.chunkIndex ?? idx,
    documentName: item.documentName || '知识库文档',
    snippet: item.snippet || item.message || '',
    errorCode: item.errorCode || 'INDEX_FAILED',
    errorReason: item.message || '索引失败',
    retryCount: item.retryCount ?? 0,
    lastAttemptAt: item.failedAt || new Date().toISOString()
  }));
};

export const triggerReindex = async (
  kbId: number,
  mode: 'FULL' | 'INCREMENTAL' = 'FULL'
): Promise<{ success: boolean; taskId: string; message: string }> => {
  await post(`/knowledge-bases/${kbId}/index`, { mode });
  return {
    success: true,
    taskId: `TASK_IDX_${Date.now()}`,
    message: mode === 'FULL' ? '全量向量索引任务已启动' : '增量向量索引任务已启动'
  };
};

export const retryFailedVectors = async (
  kbId: number,
  _vectorIds?: (number | string)[]
): Promise<{ success: boolean; message: string }> => {
  await post(`/knowledge-bases/${kbId}/index`, { mode: 'INCREMENTAL' });
  return { success: true, message: '失败切片重试任务已提交' };
};

export const reindexSingleChunk = async (
  kbId: number,
  chunkId: number
): Promise<{ success: boolean; message: string }> => {
  await post(`/knowledge-bases/${kbId}/chunks/${chunkId}/reindex`);
  return { success: true, message: '切片重试索引成功' };
};
