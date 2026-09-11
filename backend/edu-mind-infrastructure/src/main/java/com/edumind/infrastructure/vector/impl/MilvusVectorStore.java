package com.edumind.infrastructure.vector.impl;

import com.edumind.infrastructure.vector.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MilvusVectorStore implements VectorStore {

    @Override
    public void save(String collectionName, String id, List<Float> vector, Map<String, Object> metadata) {
        // Milvus 存储底层实现桩
    }

    @Override
    public void delete(String collectionName, String id) {
        // Milvus 删除底层实现桩
    }

    @Override
    public List<String> searchNearestIds(String collectionName, List<Float> queryVector, int topK) {
        // Milvus 近邻查询桩
        return Collections.emptyList();
    }
}
