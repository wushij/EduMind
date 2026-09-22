package com.edumind.infrastructure.vector.impl;

import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "edumind.milvus.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, Map<String, StoredVector>> collections = new ConcurrentHashMap<>();

    @Override
    public void save(String collectionName, String id, List<Float> vector, Map<String, Object> metadata) {
        Map<String, Object> safeMeta = metadata == null ? new java.util.HashMap<>() : new java.util.HashMap<>(metadata);
        // Fail-Closed：向量必须携带有效租户归属，否则不落库，防止污染其他租户的召回结果
        Object tenantId = safeMeta.get("tenantId");
        if (tenantId == null) {
            tenantId = com.edumind.common.context.TenantContext.getTenantId();
            if (tenantId != null) {
                safeMeta.put("tenantId", tenantId);
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException("向量写入缺少有效租户上下文，已拒绝写入 (Tenant Required)");
        }
        collections.computeIfAbsent(collectionName, key -> new ConcurrentHashMap<>())
                .put(id, new StoredVector(vector, safeMeta));
    }

    @Override
    public void delete(String collectionName, String id) {
        Map<String, StoredVector> bucket = collections.get(collectionName);
        if (bucket != null) {
            bucket.remove(id);
        }
    }

    @Override
    public List<String> searchNearestIds(String collectionName, List<Float> queryVector, int topK) {
        return searchNearest(collectionName, queryVector, topK).stream()
                .map(VectorSearchResult::getId)
                .toList();
    }

    @Override
    public List<VectorSearchResult> searchNearest(String collectionName, List<Float> queryVector, int topK,
                                                Map<String, Object> filter) {
        Map<String, StoredVector> bucket = collections.getOrDefault(collectionName, Map.of());
        List<VectorSearchResult> results = new ArrayList<>();
        for (Map.Entry<String, StoredVector> entry : bucket.entrySet()) {
            if (!matchesFilter(entry.getValue().metadata, filter)) {
                continue;
            }
            float score = cosineSimilarity(queryVector, entry.getValue().vector);
            results.add(VectorSearchResult.builder()
                    .id(entry.getKey())
                    .score(score)
                    .metadata(entry.getValue().metadata)
                    .build());
        }
        results.sort(Comparator.comparing(VectorSearchResult::getScore).reversed());
        return results.subList(0, Math.min(topK, results.size()));
    }

    @Override
    public String getEngineType() {
        return "InMemory";
    }

    @Override
    public String getVersion() {
        return "Embedded Engine";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        Map<String, Object> effective = filter == null ? new java.util.HashMap<>() : new java.util.HashMap<>(filter);
        // Fail-Closed：没有租户维度的检索一律 0 命中，绝不退化为全量检索
        if (effective.get("tenantId") == null) {
            Object current = com.edumind.common.context.TenantContext.getTenantId();
            if (current == null) {
                return false;
            }
            effective.put("tenantId", current);
        }
        for (Map.Entry<String, Object> entry : effective.entrySet()) {
            Object actual = metadata.get(entry.getKey());
            if (actual == null || !actual.toString().equals(entry.getValue().toString())) {
                return false;
            }
        }
        return true;
    }

    private float cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0f;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0 || normB == 0) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private record StoredVector(List<Float> vector, Map<String, Object> metadata) {
    }
}
