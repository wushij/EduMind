package com.edumind.ai.rag.retrieval;

import java.util.List;

/**
 * AI RAG 向量与混合检索策略器（包含 topK、score threshold、元数据过滤等业务策略）
 */
public interface RagRetriever {
    List<String> retrieve(String query, Long knowledgeBaseId, int topK, double minScore);
}
