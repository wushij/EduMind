export interface DocumentChunk {
  id: number | string;
  documentId: number;
  documentName?: string;
  chunkIndex: number;
  content: string;
  tokenCount: number;
  heading?: string;
  pageNo?: number;
  status: 'PENDING' | 'INDEXED' | 'INDEX_FAILED';
  charCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChunkQueryRequest {
  documentId?: number;
  knowledgeBaseId?: number;
  keyword?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface ChunkStatsVO {
  totalChunks: number;
  indexedChunks: number;
  pendingChunks: number;
  failedChunks: number;
  avgTokens: number;
  totalTokens: number;
}

export interface ChunkOperationResult {
  success: boolean;
  message: string;
  chunkCount?: number;
}
