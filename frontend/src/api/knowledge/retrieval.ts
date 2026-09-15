import { post } from '@/core/http/request';
import { RetrievalQuery, RetrievalResultItem } from '@/types/knowledge/rag';
import { mapRetrievalItem } from '@/utils/knowledge/rag-mapper';

export const retrieveChunks = async (queryReq: RetrievalQuery): Promise<RetrievalResultItem[]> => {
  const kbId = queryReq.knowledgeBaseId;
  if (!kbId) {
    throw new Error('缺少知识库 ID');
  }

  const payload = {
    query: queryReq.query,
    topK: queryReq.topK,
    minScore: queryReq.scoreThreshold,
    scoreThreshold: queryReq.scoreThreshold,
    documentId: queryReq.documentIds?.[0]
  };

  const res = await post<Record<string, unknown>[]>(`/knowledge-bases/${kbId}/retrieve`, payload);
  if (res?.data && Array.isArray(res.data)) {
    return res.data.map((item) => mapRetrievalItem(item as Record<string, unknown>));
  }
  return [];
};
