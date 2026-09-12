export interface RetrievalQuery {
  query: string;
  knowledgeBaseId?: number;
  documentId?: number;
  documentIds?: number[];
  topK?: number;
  scoreThreshold?: number;
  hybridSearch?: boolean;
}

export interface RetrievalResultItem {
  id: string | number;
  chunkIndex: number;
  documentId: number;
  documentName: string;
  pageNo?: number;
  heading?: string;
  content: string;
  score: number; // 0.00 - 1.00
  matchedKeywords?: string[];
}

export interface PipelineStageTiming {
  stage: 'query_rewrite' | 'vector_search' | 'keyword_search' | 'rerank' | 'context_assembly' | 'llm_generation';
  stageName: string;
  durationMs: number;
  status: 'SUCCESS' | 'SKIPPED' | 'FAILED';
  summary?: string;
}

export interface RAGDebugRequest {
  query: string;
  knowledgeBaseId?: number;
  documentIds?: number[];
  topK?: number;
  scoreThreshold?: number;
  model?: string;
  temperature?: number;
  systemPrompt?: string;
}

export interface RAGDebugResponse {
  query: string;
  rewrittenQuery?: string;
  retrievedChunks: RetrievalResultItem[];
  assembledPrompt: string;
  llmResponse: string;
  timings: PipelineStageTiming[];
  tokenUsage: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  totalLatencyMs: number;
}
