import { post } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { RAGDebugRequest, RAGDebugResponse } from '@/types/knowledge/rag';
import { retrieveChunks } from './retrieval';
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

  try {
    const res = await post<Record<string, unknown>>('/ai/rag/debug', payload);
    if (res?.data) {
      return mapRagDebugResponse(res.data);
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[RAG API] Fallback to mock RAG debug pipeline', err);
  }

  const retrievedChunks = await retrieveChunks({
    query: req.query,
    knowledgeBaseId: req.knowledgeBaseId,
    topK: req.topK || 3,
    scoreThreshold: req.scoreThreshold || 0.65
  });

  const assembledContext = retrievedChunks
    .map(
      (c, i) =>
        `【参考切片 ${i + 1}】（来源：${c.documentName} P.${c.pageNo ?? '-'}，置信度：${(c.score * 100).toFixed(1)}%）\n${c.content}`
    )
    .join('\n\n');

  const assembledPrompt = `${req.systemPrompt || '你是课程 AI 助教。'}\n\n=== 检索到的参考资料 ===\n${assembledContext}\n\n=== 学生问题 ===\n${req.query}`;

  return {
    query: req.query,
    rewrittenQuery: req.query,
    retrievedChunks,
    assembledPrompt,
    llmResponse: `（Mock）基于 ${retrievedChunks.length} 条检索结果生成的示例回答。`,
    timings: [],
    tokenUsage: { promptTokens: 0, completionTokens: 0, totalTokens: 0 },
    totalLatencyMs: 0
  };
};
