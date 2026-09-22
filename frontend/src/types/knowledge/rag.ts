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
  /** 网关模型配置键（后端会经模型治理白名单校验，未通过则回落平台策略） */
  modelKey?: string;
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
  /** 实际命中并用于生成的模型配置键（可能因回落与请求值不同）；跳过生成时为 undefined */
  modelKey?: string;
  /** 本次是否使用了调用方覆盖的系统提示词 */
  systemPromptOverridden?: boolean;
}

/** RAG 诊断可选模型（来自后端真实模型配置，非前端写死） */
export interface RagDebugModelOption {
  /** 网关模型配置键，直接作为请求 modelKey 提交 */
  value: string;
  label: string;
  provider: string;
  isDefault: boolean;
}
