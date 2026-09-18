import type { VectorStatus } from '@/types/knowledge/knowledge-base';

/** 将后端 knowledge_base.index_status 映射为列表/详情展示 */
export function mapKnowledgeBaseVectorStatus(indexStatus?: string | null): {
  vectorStatus: VectorStatus;
  vectorStatusLabel: string;
} {
  const status = (indexStatus || 'PENDING').toUpperCase();
  switch (status) {
    case 'INDEXED':
      return { vectorStatus: 'SYNCED', vectorStatusLabel: '已向量化' };
    case 'INDEXING':
      return { vectorStatus: 'PARSING', vectorStatusLabel: '向量化中' };
    case 'INDEX_FAILED':
      return { vectorStatus: 'PENDING', vectorStatusLabel: '索引失败' };
    case 'PENDING':
    case 'IDLE':
    default:
      return { vectorStatus: 'PENDING', vectorStatusLabel: '待向量化' };
  }
}
