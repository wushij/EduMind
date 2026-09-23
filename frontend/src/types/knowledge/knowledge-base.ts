export type VectorStatus = 'SYNCED' | 'PARSING' | 'PENDING';

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string;
  category: 'COMMON' | 'MAJOR' | 'EXAM' | 'COURSEWARE';
  categoryLabel: string;
  documentCount?: number;
  docCount?: number;
  chunkCount: number;
  vectorStatus: VectorStatus;
  vectorStatusLabel: string;
  vectorProgress?: number;
  embeddingModel: string;
  /** true=当前向量为 Mock 哈希伪向量（未接入真实向量模型），检索命中率不可信 */
  embeddingMocked?: boolean;
  updatedAt: string;
  courseId?: number;
  courseName?: string;
  colorGradient?: string;
}

export interface KnowledgeBaseCreateRequest {
  name: string;
  description: string;
  category: string;
  embeddingModel: string;
  courseId?: number;
}
