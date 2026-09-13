import { del, get, post } from '@/core/http/request';
import type { GraphGapVO, GraphRelationSuggestion, KnowledgeGraphVO } from '@/types/knowledge/graph';

export const getKnowledgeGraph = (kbId: number, depth = 2, types?: string) =>
  get<KnowledgeGraphVO>(`/knowledge-bases/${kbId}/graph`, { depth, types });

export const getGraphGaps = (kbId: number, studentId?: number, masteryThreshold = 0.6) =>
  get<GraphGapVO[]>(`/knowledge-bases/${kbId}/graph/gaps`, { studentId, masteryThreshold });

export const createKnowledgePointRelation = (
  knowledgePointId: number,
  data: { targetKnowledgePointId: number; relationType: string }
) => post<void>(`/knowledge-points/${knowledgePointId}/relations`, data);

export const listKnowledgePointRelations = (knowledgePointId: number) =>
  get<Array<{ id: number; sourceKnowledgePointId: number; targetKnowledgePointId: number; relationType: string }>>(
    `/knowledge-points/${knowledgePointId}/relations`
  );

export const deleteKnowledgePointRelation = (relationId: number) =>
  del<void>(`/knowledge-points/relations/${relationId}`);

export const suggestRelations = (
  kbId: number,
  params?: { sourceKnowledgePointId?: number; maxSuggestions?: number }
) =>
  post<GraphRelationSuggestion[]>(`/knowledge-bases/${kbId}/graph/suggest-relations`, params ?? {});
