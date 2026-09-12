import { get } from '@/core/http/request';
import type { KnowledgeGraphVO } from '@/types/knowledge/graph';

export const getKnowledgeGraph = (kbId: number) =>
  get<KnowledgeGraphVO>(`/knowledge-bases/${kbId}/graph`);
