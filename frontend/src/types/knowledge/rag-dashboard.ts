export interface KnowledgeRagDashboardVO {
  totalDocuments: number
  indexedDocuments: number
  lessonDocuments: number
  uploadDocuments: number
  totalChunks: number
  lessonChunks: number
  uploadChunks: number
  indexedChunks: number
  failedChunks: number
  normalChunks: number
  orphanChunkEstimate: number
  totalSessions: number
  embeddingDimension?: number
  embeddingModelName?: string
  /** true=当前向量为 Mock 哈希伪向量（未接入真实向量模型），检索命中率不可信 */
  embeddingMocked?: boolean
  hybridEnabled: boolean
  retrievalModelDescription?: string
  vectorStoreLabel?: string
  indexHealthPercent?: number
  minRrfScore?: number
  latestIndexStatus?: string
  latestIndexTotalChunks?: number
  latestIndexIndexedChunks?: number
  latestIndexFailedChunks?: number
  latestIndexEmbeddingModel?: string
  latestIndexStartedAt?: string
}

export interface KnowledgeRagSyncResultVO {
  status: string
  knowledgeBasesTriggered?: number
  lessonsReindexed?: number
  documentId?: number
}

export interface KnowledgeRagPurgeResultVO {
  purgedChunks: number
  purgedIndexRows: number
}
