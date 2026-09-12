package com.edumind.infrastructure.vector;

import java.util.List;
import java.util.Map;

/**
 * 底层向量存储抽象接口（对接 Milvus / PgVector / ES）
 * 仅负责向量底层的存储、删除与裸相似度近邻计算，不含 RAG 业务编排
 */
public interface VectorStore {

    void save(String collectionName, String id, List<Float> vector, Map<String, Object> metadata);

    void delete(String collectionName, String id);

    List<String> searchNearestIds(String collectionName, List<Float> queryVector, int topK);

    default List<VectorSearchResult> searchNearest(String collectionName, List<Float> queryVector, int topK) {
        return searchNearestIds(collectionName, queryVector, topK).stream()
                .map(id -> VectorSearchResult.builder().id(id).score(0f).build())
                .toList();
    }

    default List<VectorSearchResult> searchNearest(String collectionName, List<Float> queryVector, int topK,
                                                   Map<String, Object> filter) {
        return searchNearest(collectionName, queryVector, topK);
    }
}
