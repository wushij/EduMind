export interface VectorStoreStats {
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
