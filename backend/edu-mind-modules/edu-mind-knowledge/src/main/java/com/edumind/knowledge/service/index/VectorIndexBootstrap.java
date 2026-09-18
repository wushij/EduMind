package com.edumind.knowledge.service.index;

import com.alibaba.fastjson2.JSON;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.entity.KnowledgeChunkIndexEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 关闭时使用内存向量库；启动时从 MySQL embedding_vector 恢复，避免重启后检索失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edumind.milvus.enabled", havingValue = "false", matchIfMissing = true)
public class VectorIndexBootstrap {

    private static final int BATCH_SIZE = 200;

    private final MilvusProperties milvusProperties;
    private final VectorStore vectorStore;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;

    @EventListener(ApplicationReadyEvent.class)
    public void reloadVectorsFromDatabase() {
        long offset = 0;
        int loaded = 0;
        while (true) {
            List<KnowledgeChunkIndexEntity> batch =
                    knowledgeChunkIndexDao.listIndexedWithEmbeddingVectors(offset, BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }
            for (KnowledgeChunkIndexEntity index : batch) {
                if (!StringUtils.hasText(index.getEmbeddingVector())) {
                    continue;
                }
                List<Float> vector = JSON.parseArray(index.getEmbeddingVector(), Float.class);
                if (vector == null || vector.isEmpty()) {
                    continue;
                }
                KnowledgeDocumentChunkEntity chunk = knowledgeDocumentChunkDao.findById(index.getChunkId());
                if (chunk == null) {
                    continue;
                }
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunkId", chunk.getId());
                metadata.put("documentId", chunk.getDocumentId());
                metadata.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
                metadata.put("pageNo", chunk.getPageNo());
                vectorStore.save(
                        milvusProperties.getCollection(),
                        String.valueOf(chunk.getId()),
                        vector,
                        metadata);
                loaded++;
            }
            offset += batch.size();
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        if (loaded > 0) {
            log.info("已从 MySQL 恢复 {} 条向量到内存向量库 collection={}", loaded, milvusProperties.getCollection());
        }
    }
}
