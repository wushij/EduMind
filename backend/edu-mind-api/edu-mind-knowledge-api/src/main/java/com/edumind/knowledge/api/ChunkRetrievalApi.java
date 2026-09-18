package com.edumind.knowledge.api;

import com.edumind.knowledge.vo.knowledge.ChunkKeywordSearchVO;

/**
 * 知识库切片关键词召回（混合 RAG 的 FTS/关键词分支，对齐 Code Compass HybridSearch）。
 */
public interface ChunkRetrievalApi {

    ChunkKeywordSearchVO searchKeywords(Long knowledgeBaseId, Long documentId, String query, int limit);
}
