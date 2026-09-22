import type {
  PipelineStageTiming,
  RetrievalResultItem,
  RAGDebugResponse
} from '@/types/knowledge/rag';

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

function mapStageTiming(raw: Record<string, unknown>): PipelineStageTiming {
  return {
    stage: (raw.stage as PipelineStageTiming['stage']) ?? 'vector_search',
    stageName: (raw.stageName as string) || '',
    durationMs: (raw.durationMs as number) ?? 0,
    status: (raw.status as PipelineStageTiming['status']) ?? 'SUCCESS',
    summary: raw.summary as string | undefined
  };
}

export function mapRagDebugResponse(raw: Record<string, unknown>): RAGDebugResponse {
  const retrievalResults = Array.isArray(raw.retrievalResults)
    ? (raw.retrievalResults as Record<string, unknown>[]).map(mapRetrievalItem)
    : [];

  // 阶段耗时与 Token 均由后端埋点/估算返回，缺失时保持空值而不是伪造 0 值展示
  const timings = Array.isArray(raw.stageTimings)
    ? (raw.stageTimings as Record<string, unknown>[]).map(mapStageTiming)
    : [];

  const promptTokens = (raw.promptTokens as number) ?? 0;
  const completionTokens = (raw.completionTokens as number) ?? 0;

  return {
    query: (raw.originalQuery as string) || '',
    rewrittenQuery: raw.rewrittenQuery as string | undefined,
    retrievedChunks: retrievalResults,
    assembledPrompt: [raw.context, raw.promptPreview].filter(Boolean).join('\n\n'),
    llmResponse: (raw.answer as string) || '',
    timings,
    tokenUsage: {
      promptTokens,
      completionTokens,
      totalTokens: (raw.totalTokens as number) ?? promptTokens + completionTokens
    },
    totalLatencyMs: (raw.totalLatencyMs as number) ?? 0,
    modelKey: raw.modelKey as string | undefined,
    systemPromptOverridden: raw.systemPromptOverridden as boolean | undefined
  };
}
