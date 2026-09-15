import { post } from '@/core/http/request';
import { RAGDebugRequest, RAGDebugResponse } from '@/types/knowledge/rag';
import { mapRagDebugResponse } from './rag-mapper';

export const debugRagPipeline = async (req: RAGDebugRequest): Promise<RAGDebugResponse> => {
  const payload = {
    query: req.query,
    knowledgeBaseId: req.knowledgeBaseId,
    topK: req.topK,
    minScore: req.scoreThreshold,
    scoreThreshold: req.scoreThreshold,
    documentId: req.documentIds?.[0],
    skipLlm: false
  };

  const res = await post<Record<string, unknown>>('/ai/rag/debug', payload);
  if (!res?.data) {
    throw new Error('RAG 诊断服务未返回数据');
  }
  return mapRagDebugResponse(res.data);
};
