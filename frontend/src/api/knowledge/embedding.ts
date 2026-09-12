import { get, post } from '@/core/http/request';
import { VectorStoreStats, FailedVectorItem } from '@/types/knowledge/embedding';
import { USE_MOCK } from '@/config/mock';

interface IndexStatusVO {
  status?: string;
  totalChunks?: number;
  indexedChunks?: number;
  failedChunks?: number;
  embeddingModel?: string;
  startedAt?: string;
  errors?: Array<{ chunkId?: number; message?: string }>;
}

function mapIndexStatusToStats(status: IndexStatusVO, kbId: number): VectorStoreStats {
  const total = status.totalChunks ?? 0;
  const indexed = status.indexedChunks ?? 0;
  const health = total > 0 ? Math.round((indexed / total) * 100) : 0;
  return {
    engine: 'Milvus',
    engineVersion: 'dev',
    connectionStatus: status.status === 'INDEX_FAILED' ? 'DEGRADED' : 'ONLINE',
    collectionName: `kb_${kbId}_vectors`,
    dimensions: 1536,
    metricType: 'COSINE',
    indexType: 'HNSW',
    totalVectors: indexed,
    expectedVectors: total,
    indexHealthScore: health,
    avgQueryLatencyMs: 0,
    lastIndexedAt: status.startedAt || new Date().toISOString()
  };
}

export const mockVectorStats: VectorStoreStats = {
  engine: 'Milvus',
  engineVersion: 'v2.4.5-standalone',
  connectionStatus: 'ONLINE',
  collectionName: 'kb_vectors_prod_math',
  dimensions: 1536,
  metricType: 'COSINE',
  indexType: 'HNSW',
  totalVectors: 124,
  expectedVectors: 128,
  indexHealthScore: 97,
  avgQueryLatencyMs: 14.8,
  lastIndexedAt: '2026-09-11 11:20:45'
};

export const mockFailedVectors: FailedVectorItem[] = [];

export const getVectorStats = async (kbId?: number): Promise<VectorStoreStats> => {
  const id = kbId || 1;
  try {
    const res = await get<IndexStatusVO>(`/knowledge-bases/${id}/index/status`);
    if (res?.data) {
      return mapIndexStatusToStats(res.data, id);
    }
    if (!USE_MOCK) {
      return mapIndexStatusToStats({}, id);
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Vector API] Fallback to mockVectorStats', err);
  }
  return USE_MOCK ? mockVectorStats : mapIndexStatusToStats({}, id);
};

export const getFailedVectors = async (kbId?: number): Promise<FailedVectorItem[]> => {
  try {
    const res = await get<IndexStatusVO>(`/knowledge-bases/${kbId || 1}/index/status`);
    const errors = res?.data?.errors || [];
    if (errors.length > 0) {
      return errors.map((item, idx) => ({
        id: item.chunkId ?? idx,
        chunkIndex: idx,
        documentName: '未知文档',
        snippet: item.message || '',
        errorCode: 'INDEX_FAILED',
        errorReason: item.message || '索引失败',
        retryCount: 0,
        lastAttemptAt: new Date().toISOString()
      }));
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Vector API] Fallback to mockFailedVectors', err);
  }
  return mockFailedVectors;
};

export const triggerReindex = async (
  kbId: number,
  mode: 'FULL' | 'INCREMENTAL' = 'FULL'
): Promise<{ success: boolean; taskId: string; message: string }> => {
  try {
    await post(`/knowledge-bases/${kbId}/index`, { mode });
    return {
      success: true,
      taskId: `TASK_IDX_${Date.now()}`,
      message: mode === 'FULL' ? '全量向量索引任务已启动' : '增量向量索引任务已启动'
    };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Vector API] Fallback triggerReindex mock', err);
  }
  return {
    success: true,
    taskId: 'TASK_IDX_' + Date.now(),
    message: mode === 'FULL' ? '全量向量索引任务已启动！' : '增量向量索引任务已启动！'
  };
};

export const retryFailedVectors = async (
  kbId: number,
  vectorIds?: (number | string)[]
): Promise<{ success: boolean; message: string }> => {
  try {
    await post(`/knowledge-bases/${kbId}/index`, { mode: 'INCREMENTAL' });
    return { success: true, message: '失败切片重试任务已提交' };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Vector API] Fallback retryFailedVectors mock', err);
  }
  return {
    success: true,
    message: '失败切片重试任务已提交至后台处理流水线！'
  };
};
