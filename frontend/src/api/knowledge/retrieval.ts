import { post } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { RetrievalQuery, RetrievalResultItem } from '@/types/knowledge/rag';
import { mockChunks } from '@/mock/knowledge-chunks';
import { mapRetrievalItem } from './rag-mapper';

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

  try {
    const res = await post<Record<string, unknown>[]>(`/knowledge-bases/${kbId}/retrieve`, payload);
    if (res?.data && Array.isArray(res.data)) {
      return res.data.map((item) => mapRetrievalItem(item as Record<string, unknown>));
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Retrieval API] Fallback to mock retrieval', err);
  }

  const query = (queryReq.query || '').trim().toLowerCase();
  const topK = queryReq.topK || 5;
  const threshold = queryReq.scoreThreshold || 0.6;

  return mockChunks
    .map((chunk, idx) => {
      let baseScore = 0.65 - idx * 0.05;
      const keywords = ['多态', '接口', '向上转型', '异常', '积分'];
      keywords.forEach((kw) => {
        if (query.includes(kw.toLowerCase()) && chunk.content.includes(kw)) {
          baseScore += 0.15;
        }
      });
      const score = Math.min(0.98, Math.max(0.4, baseScore));
      return {
        id: chunk.id,
        chunkIndex: chunk.chunkIndex,
        documentId: chunk.documentId,
        documentName: chunk.documentName || `文档 #${chunk.documentId}`,
        pageNo: chunk.pageNo,
        heading: chunk.heading,
        content: chunk.content,
        score
      };
    })
    .filter((item) => item.score >= threshold)
    .sort((a, b) => b.score - a.score)
    .slice(0, topK);
};
