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

    /**
     * 带元数据过滤的近邻检索（租户隔离强制依赖此重载）。
     * <p>刻意不做「忽略 filter 直接全量检索」的默认降级实现：任何未正确实现租户过滤的向量存储
     * 都必须显式抛错，而不是静默返回全量结果造成跨租户召回。</p>
     */
    default List<VectorSearchResult> searchNearest(String collectionName, List<Float> queryVector, int topK,
                                                   Map<String, Object> filter) {
        throw new UnsupportedOperationException(
                "当前 VectorStore 实现未支持带过滤条件的近邻检索，无法保证租户隔离，已拒绝执行");
    }

    default String getEngineType() {
        return "InMemory";
    }

    default String getVersion() {
        return "v1.0";
    }

    default boolean isHealthy() {
        return true;
    }
}
