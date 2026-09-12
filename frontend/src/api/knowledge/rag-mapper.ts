import type { RetrievalResultItem, RAGDebugResponse } from '@/types/knowledge/rag';

export function mapRetrievalItem(raw: Record<string, unknown>): RetrievalResultItem {
  return {
    id: (raw.chunkId ?? raw.id ?? 0) as string | number,
    chunkIndex: (raw.chunkIndex as number) ?? 0,
    documentId: (raw.documentId as number) ?? 0,
    documentName: (raw.documentName as string) || '未知文档',
    pageNo: raw.pageNo as number | undefined,
    heading: raw.heading as string | undefined,
    content: ((raw.content ?? raw.excerpt) as string) || '',
    score: (raw.score as number) ?? 0
  };
}

export function mapRagDebugResponse(raw: Record<string, unknown>): RAGDebugResponse {
  const retrievalResults = Array.isArray(raw.retrievalResults)
    ? (raw.retrievalResults as Record<string, unknown>[]).map(mapRetrievalItem)
    : [];

  return {
    query: (raw.originalQuery as string) || '',
    rewrittenQuery: raw.rewrittenQuery as string | undefined,
    retrievedChunks: retrievalResults,
    assembledPrompt: [raw.context, raw.promptPreview].filter(Boolean).join('\n\n'),
    llmResponse: (raw.answer as string) || '',
    timings: [],
    tokenUsage: {
      promptTokens: 0,
      completionTokens: 0,
      totalTokens: 0
    },
    totalLatencyMs: 0
  };
}
