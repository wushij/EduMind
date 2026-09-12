package com.edumind.knowledge.service.index.impl;

import com.edumind.common.api.embedding.EmbeddingApi;
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
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.IndexStatusVO;
import com.edumind.notification.api.NotificationWriteApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerIndex(Long knowledgeBaseId, String mode) {
        KnowledgeBaseEntity knowledgeBase = knowledgeAccessService.assertAccessible(knowledgeBaseId);
        Long operatorId = UserContext.getUserId();
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
        runIndexAsync(task.getId(), knowledgeBaseId, task.getMode(), operatorId);
    }

    @Async("knowledgeTaskExecutor")
    public void runIndexAsync(Long taskId, Long knowledgeBaseId, String mode, Long operatorId) {
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
            notifyIndexResult(operatorId, knowledgeBaseId, "INDEXED", 0, 0);
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
                    vectorStore.save(milvusProperties.getCollection(), vectorId, vectors.get(i), metadata);
                    upsertChunkIndex(chunk, vectorId, "INDEXED", null);
                    indexed++;
                } catch (Exception ex) {
                    failed++;
                    upsertChunkIndex(chunk, String.valueOf(chunk.getId()), "FAILED", ex.getMessage());
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
    }

    private void notifyIndexResult(Long userId, Long knowledgeBaseId, String status, int indexed, int failed) {
        if (userId == null) {
            return;
        }
        String title = "INDEXED".equals(status) ? "向量索引完成" : "INDEX_FAILED".equals(status) ? "向量索引失败" : "向量索引更新";
        String content = "知识库 #" + knowledgeBaseId + " 索引状态：" + status
                + "，成功 " + indexed + " 条，失败 " + failed + " 条。";
        notificationWriteApi.sendToUser(userId, title, content, "KNOWLEDGE_INDEX");
    }

    @Override
    public IndexStatusVO getIndexStatus(Long knowledgeBaseId) {
        KnowledgeIndexTaskEntity task = knowledgeIndexTaskDao.findLatestByKnowledgeBaseId(knowledgeBaseId);
        IndexStatusVO vo = new IndexStatusVO();
        if (task == null) {
            vo.setStatus("PENDING");
            vo.setTotalChunks((int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId));
            vo.setIndexedChunks((int) knowledgeChunkIndexDao.countIndexedByKnowledgeBaseId(knowledgeBaseId));
            vo.setFailedChunks(0);
            vo.setEmbeddingModel(embeddingApi.getModelName());
            return vo;
        }
        vo.setStatus(task.getStatus());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setIndexedChunks(task.getIndexedChunks());
        vo.setFailedChunks(task.getFailedChunks());
        vo.setEmbeddingModel(task.getEmbeddingModel());
        vo.setStartedAt(task.getStartedAt());
        vo.setErrors(knowledgeChunkIndexDao.findFailedByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(item -> {
                    IndexStatusVO.IndexErrorVO error = new IndexStatusVO.IndexErrorVO();
                    error.setChunkId(item.getChunkId());
                    error.setMessage(item.getErrorMessage());
                    return error;
                }).collect(Collectors.toList()));
        return vo;
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

    private void upsertChunkIndex(KnowledgeDocumentChunkEntity chunk, String vectorId, String status, String error) {
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
            knowledgeChunkIndexDao.insert(entity);
        } else {
            existing.setVectorId(vectorId);
            existing.setEmbedStatus(status);
            existing.setEmbeddingModel(embeddingApi.getModelName());
            existing.setErrorMessage(error);
            knowledgeChunkIndexDao.updateById(existing);
        }
    }
}
