export interface VectorStoreStats {
  /** 当前生效的向量模型名（Mock 实现会带 -mock 后缀） */
  embeddingModel?: string;
  /** true=向量来自 Mock 哈希伪向量（未接入真实向量模型），检索命中率不可信 */
  embeddingMocked?: boolean;
  engine: string;
  engineVersion: string;
  connectionStatus: 'ONLINE' | 'DEGRADED' | 'OFFLINE';
  collectionName: string;
  dimensions: number;
  metricType: string;
  indexType: string;
  totalVectors: number;
  expectedVectors: number;
  indexHealthScore: number; // 0 - 100
  avgQueryLatencyMs: number;
  lastIndexedAt: string;
}

export interface EmbeddingTask {
  taskId: string;
  status: 'PENDING' | 'INDEXING' | 'INDEXED' | 'FAILED';
  progress: number; // 0 - 100
  totalChunks: number;
  indexedChunks: number;
  failedChunks: number;
  startedAt: string;
  completedAt?: string;
  currentDocument?: string;
}

export interface FailedVectorItem {
  id: number | string;
  chunkIndex: number;
  documentName: string;
  snippet: string;
  errorCode: string;
  errorReason: string;
  retryCount: number;
  lastAttemptAt: string;
}
