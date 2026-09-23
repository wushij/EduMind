package com.edumind.knowledge.service.index.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.api.embedding.EmbeddingApi;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeIndexTaskDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeChunkIndexEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeIndexTaskEntity;
import com.edumind.common.model.UserContext;
import com.edumind.knowledge.event.KnowledgeIndexRequestedEvent;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.IndexStatusVO;
import com.edumind.notification.api.NotificationWriteApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeIndexTaskDao knowledgeIndexTaskDao;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;
    private final EmbeddingApi embeddingApi;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;
    private final KnowledgeAccessService knowledgeAccessService;
    private final NotificationWriteApi notificationWriteApi;
    private final com.edumind.ai.api.RagRuntimeQueryApi ragRuntimeQueryApi;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerIndex(Long knowledgeBaseId, String mode) {
        KnowledgeBaseEntity knowledgeBase = knowledgeAccessService.assertAccessible(knowledgeBaseId);
        Long operatorId = UserContext.getUserId();
        Long tenantId = knowledgeBase.getTenantId() != null ? knowledgeBase.getTenantId() : TenantContext.getTenantId();
        KnowledgeIndexTaskEntity task = new KnowledgeIndexTaskEntity();
        task.setKnowledgeBaseId(knowledgeBaseId);
        task.setMode(mode != null ? mode : "FULL");
        task.setStatus("INDEXING");
        task.setTotalChunks((int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId));
        task.setIndexedChunks(0);
        task.setFailedChunks(0);
        task.setEmbeddingModel(embeddingApi.getModelName());
        task.setStartedAt(LocalDateTime.now());
        knowledgeIndexTaskDao.insert(task);

        knowledgeBase.setIndexStatus("INDEXING");
        knowledgeBaseDao.updateById(knowledgeBase);
        // 交给监听器在事务提交后异步执行：既避免 embedding 阻塞本请求（前端 30s 超时），
        // 又保证异步线程能读到上面刚插入的 task 行（提交前读不到会被任务守卫跳过）
        applicationEventPublisher.publishEvent(new KnowledgeIndexRequestedEvent(
                task.getId(), knowledgeBaseId, task.getMode(), operatorId, tenantId));
    }

    @Override
    public void runIndexTask(Long taskId, Long knowledgeBaseId, String mode, Long operatorId, Long tenantId) {
        if (tenantId != null && tenantId > 0) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            KnowledgeIndexTaskEntity task = knowledgeIndexTaskDao.findLatestByKnowledgeBaseId(knowledgeBaseId);
            if (task == null || !task.getId().equals(taskId)) {
                return;
            }
            List<KnowledgeDocumentChunkEntity> chunks = knowledgeDocumentChunkDao.findByKnowledgeBaseId(knowledgeBaseId);
            if ("INCREMENTAL".equalsIgnoreCase(mode)) {
                chunks = chunks.stream()
                        .filter(chunk -> {
                            KnowledgeChunkIndexEntity index = knowledgeChunkIndexDao.findByChunkId(chunk.getId());
                            return index == null || "FAILED".equals(index.getEmbedStatus());
                        })
                        .collect(Collectors.toList());
            }
            int indexed = 0;
            int failed = 0;
            List<IndexStatusVO.IndexErrorVO> errors = new ArrayList<>();
            if (chunks.isEmpty()) {
                task.setStatus("INDEXED");
                task.setIndexedChunks(0);
                task.setFailedChunks(0);
                task.setFinishedAt(LocalDateTime.now());
                knowledgeIndexTaskDao.updateById(task);
                KnowledgeBaseEntity emptyKb = knowledgeBaseDao.findById(knowledgeBaseId);
                if (emptyKb != null) {
                    emptyKb.setIndexStatus("INDEXED");
                    knowledgeBaseDao.updateById(emptyKb);
                }
                log.debug("知识库 #{} 当前无待增量索引切片，任务完成", knowledgeBaseId);
                return;
            }
            List<String> texts = chunks.stream().map(KnowledgeDocumentChunkEntity::getContent).toList();
            try {
                List<List<Float>> vectors = embeddingApi.embed(texts);
                if (vectors == null || vectors.size() != chunks.size()) {
                    throw new IllegalStateException("Embedding 结果数量与 Chunk 不一致");
                }
                for (int i = 0; i < chunks.size(); i++) {
                    KnowledgeDocumentChunkEntity chunk = chunks.get(i);
                    try {
                        String vectorId = String.valueOf(chunk.getId());
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("chunkId", chunk.getId());
                        metadata.put("documentId", chunk.getDocumentId());
                        metadata.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
                        metadata.put("pageNo", chunk.getPageNo());
                        if (tenantId != null && tenantId > 0) {
                            metadata.put("tenantId", tenantId);
                        }
                        vectorStore.save(milvusProperties.getCollection(), vectorId, vectors.get(i), metadata);
                        upsertChunkIndex(chunk, vectorId, "INDEXED", null, vectors.get(i));
                        indexed++;
                    } catch (Exception ex) {
                        failed++;
                        upsertChunkIndex(chunk, String.valueOf(chunk.getId()), "FAILED", ex.getMessage(), null);
                        IndexStatusVO.IndexErrorVO error = new IndexStatusVO.IndexErrorVO();
                        error.setChunkId(chunk.getId());
                        error.setMessage(ex.getMessage());
                        errors.add(error);
                    }
                }
                task.setStatus(failed > 0 && indexed == 0 ? "INDEX_FAILED" : "INDEXED");
                task.setIndexedChunks(indexed);
                task.setFailedChunks(failed);
                task.setFinishedAt(LocalDateTime.now());
                knowledgeIndexTaskDao.updateById(task);

                KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
                if (knowledgeBase != null) {
                    knowledgeBase.setIndexStatus(task.getStatus());
                    knowledgeBaseDao.updateById(knowledgeBase);
                }
                notifyIndexResult(operatorId, knowledgeBaseId, task.getStatus(), indexed, failed);
            } catch (Exception ex) {
                log.error("索引任务失败 knowledgeBaseId={}", knowledgeBaseId, ex);
                task.setStatus("INDEX_FAILED");
                task.setErrorMessage(ex.getMessage());
                task.setFinishedAt(LocalDateTime.now());
                knowledgeIndexTaskDao.updateById(task);
                notifyIndexResult(operatorId, knowledgeBaseId, "INDEX_FAILED", 0, 0);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void notifyIndexResult(Long userId, Long knowledgeBaseId, String status, int indexed, int failed) {
        if (userId == null) {
            return;
        }
        String title = "INDEXED".equals(status) ? "向量索引完成" : "INDEX_FAILED".equals(status) ? "向量索引失败" : "向量索引更新";
        String content = "知识库 #" + knowledgeBaseId + " 索引状态：" + status
                + "，成功 " + indexed + " 条，失败 " + failed + " 条。";
        notificationWriteApi.sendToUser(userId, title, content, "KNOWLEDGE_INDEX", knowledgeBaseId);
    }

    @Override
    public IndexStatusVO getIndexStatus(Long knowledgeBaseId) {
        KnowledgeIndexTaskEntity task = knowledgeIndexTaskDao.findLatestByKnowledgeBaseId(knowledgeBaseId);
        IndexStatusVO vo = new IndexStatusVO();
        populateEngineMetadata(vo, knowledgeBaseId);

        int currentTotalChunks = (int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId);
        int currentIndexedChunks = (int) knowledgeChunkIndexDao.countIndexedByKnowledgeBaseId(knowledgeBaseId);
        int currentFailedChunks = (int) knowledgeChunkIndexDao.countFailedByKnowledgeBaseId(knowledgeBaseId);

        vo.setTotalChunks(currentTotalChunks);
        vo.setIndexedChunks(currentIndexedChunks);
        vo.setFailedChunks(currentFailedChunks);
        vo.setEmbeddingModel(embeddingApi.getModelName());
        // Mock 伪向量必须显式暴露：否则界面"已向量化"会被误读为语义检索可用
        vo.setEmbeddingMocked(embeddingApi.isMockVector());

        if (task == null) {
            vo.setStatus(currentIndexedChunks > 0 ? (currentIndexedChunks >= currentTotalChunks ? "INDEXED" : "PARTIAL") : "PENDING");
            vo.setErrors(List.of());
            return vo;
        }
        vo.setStatus(task.getStatus());
        vo.setStartedAt(task.getStartedAt());

        List<KnowledgeChunkIndexEntity> failedEntities = knowledgeChunkIndexDao.findFailedByKnowledgeBaseId(knowledgeBaseId);
        List<IndexStatusVO.IndexErrorVO> errors = new ArrayList<>();
        for (KnowledgeChunkIndexEntity item : failedEntities) {
            IndexStatusVO.IndexErrorVO error = new IndexStatusVO.IndexErrorVO();
            error.setChunkId(item.getChunkId());
            error.setDocumentId(item.getDocumentId());
            error.setErrorCode("INDEX_FAILED");
            error.setMessage(item.getErrorMessage() != null ? item.getErrorMessage() : "向量计算或存储失败");
            error.setRetryCount(0);
            error.setFailedAt(item.getUpdateTime() != null ? item.getUpdateTime() : item.getCreateTime());

            if (item.getChunkId() != null) {
                KnowledgeDocumentChunkEntity chunk = knowledgeDocumentChunkDao.findById(item.getChunkId());
                if (chunk != null) {
                    error.setChunkIndex(chunk.getChunkIndex());
                    String text = chunk.getContent();
                    if (text != null) {
                        error.setSnippet(text.length() > 120 ? text.substring(0, 120) + "..." : text);
                    }
                }
            }
            if (item.getDocumentId() != null) {
                KnowledgeDocumentEntity doc = knowledgeDocumentDao.findById(item.getDocumentId());
                error.setDocumentName(doc != null ? doc.getFileName() : "知识库文档");
            } else {
                error.setDocumentName("知识库文档");
            }
            errors.add(error);
        }
        vo.setErrors(errors);
        return vo;
    }

    private void populateEngineMetadata(IndexStatusVO vo, Long knowledgeBaseId) {
        vo.setEngine(vectorStore.getEngineType());
        vo.setEngineVersion(vectorStore.getVersion());
        vo.setConnectionStatus(vectorStore.isHealthy() ? "ONLINE" : "DEGRADED");
        vo.setCollectionName(milvusProperties.getCollection());
        vo.setDimensions(embeddingApi.getDimensions());
        vo.setIndexType(milvusProperties.isEnabled() ? "HNSW" : "FLAT");
        vo.setMetricType("COSINE");
        vo.setAvgQueryLatencyMs(ragRuntimeQueryApi != null ? ragRuntimeQueryApi.getAverageRecallLatencyMs(knowledgeBaseId) : 0L);
    }

    @Override
    public void reindexDocument(Long documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        knowledgeChunkIndexDao.deleteByDocumentId(documentId);
        triggerIndex(document.getKnowledgeBaseId(), "INCREMENTAL");
    }

    @Override
    public void purgeDocumentVectors(Long documentId) {
        if (documentId == null) {
            return;
        }
        String collection = milvusProperties.getCollection();
        for (KnowledgeDocumentChunkEntity chunk : knowledgeDocumentChunkDao.listByDocumentId(documentId)) {
            try {
                vectorStore.delete(collection, String.valueOf(chunk.getId()));
            } catch (Exception ex) {
                // 向量删除失败不能阻断文档记录清理，否则用户会卡在"删不掉"
                log.warn("删除文档向量失败 chunkId={}: {}", chunk.getId(), ex.getMessage());
            }
        }
        knowledgeChunkIndexDao.deleteByDocumentId(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reindexChunk(Long knowledgeBaseId, Long chunkId) {
        KnowledgeDocumentChunkEntity chunk = knowledgeDocumentChunkDao.findById(chunkId);
        if (chunk == null || !knowledgeBaseId.equals(chunk.getKnowledgeBaseId())) {
            throw new BusinessException("切片不存在或不属于该知识库");
        }
        try {
            List<List<Float>> vectors = embeddingApi.embed(List.of(chunk.getContent()));
            if (vectors == null || vectors.isEmpty()) {
                throw new IllegalStateException("Embedding 计算返回空向量");
            }
            List<Float> vector = vectors.get(0);
            String vectorId = String.valueOf(chunk.getId());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkId", chunk.getId());
            metadata.put("documentId", chunk.getDocumentId());
            metadata.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
            metadata.put("pageNo", chunk.getPageNo());

            vectorStore.save(milvusProperties.getCollection(), vectorId, vector, metadata);
            upsertChunkIndex(chunk, vectorId, "INDEXED", null, vector);
        } catch (Exception ex) {
            log.error("单切片重试失败 chunkId={}: {}", chunkId, ex.getMessage());
            upsertChunkIndex(chunk, String.valueOf(chunk.getId()), "FAILED", ex.getMessage(), null);
            throw new BusinessException("重试切片索引失败: " + ex.getMessage());
        }
    }

    private void upsertChunkIndex(KnowledgeDocumentChunkEntity chunk, String vectorId, String status, String error,
                                  List<Float> embeddingVector) {
        KnowledgeChunkIndexEntity existing = knowledgeChunkIndexDao.findByChunkId(chunk.getId());
        if (existing == null) {
            KnowledgeChunkIndexEntity entity = new KnowledgeChunkIndexEntity();
            entity.setChunkId(chunk.getId());
            entity.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
            entity.setDocumentId(chunk.getDocumentId());
            entity.setVectorId(vectorId);
            entity.setEmbedStatus(status);
            entity.setEmbeddingModel(embeddingApi.getModelName());
            entity.setErrorMessage(error);
            if (embeddingVector != null && !embeddingVector.isEmpty()) {
                entity.setEmbeddingVector(JSON.toJSONString(embeddingVector));
            }
            knowledgeChunkIndexDao.insert(entity);
        } else {
            existing.setVectorId(vectorId);
            existing.setEmbedStatus(status);
            existing.setEmbeddingModel(embeddingApi.getModelName());
            existing.setErrorMessage(error);
            if (embeddingVector != null && !embeddingVector.isEmpty()) {
                existing.setEmbeddingVector(JSON.toJSONString(embeddingVector));
            }
            knowledgeChunkIndexDao.updateById(existing);
        }
    }
}
