export type VectorStatus = 'SYNCED' | 'PARSING' | 'PENDING';

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string;
  category: 'COMMON' | 'MAJOR' | 'EXAM' | 'COURSEWARE';
  categoryLabel: string;
  documentCount: number;
  chunkCount: number;
  vectorStatus: VectorStatus;
  vectorStatusLabel: string;
  vectorProgress?: number;
  embeddingModel: string;
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
