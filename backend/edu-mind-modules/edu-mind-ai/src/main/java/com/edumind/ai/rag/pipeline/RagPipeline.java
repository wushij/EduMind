package com.edumind.ai.rag.pipeline;

/**
 * RAG 检索生成全流程编排管道
 * Query Rewrite -> Vector Retrieval -> Metadata Filter -> Rerank -> Context Builder -> Prompt -> LLM
 */
public interface RagPipeline {
    String execute(String query, Long knowledgeBaseId);
}
