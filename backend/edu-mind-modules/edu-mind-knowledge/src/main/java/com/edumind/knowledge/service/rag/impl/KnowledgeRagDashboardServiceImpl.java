package com.edumind.knowledge.service.rag.impl;

import com.edumind.ai.api.AiQueryApi;
import com.edumind.ai.api.RagRuntimeQueryApi;
import com.edumind.ai.vo.rag.RagRuntimeConfigVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.infrastructure.vector.impl.InMemoryVectorStore;
import com.edumind.infrastructure.vector.impl.MilvusVectorStore;
import com.edumind.knowledge.api.LessonContentIndexApi;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeIndexTaskDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeIndexTaskEntity;
import com.edumind.knowledge.mapper.KnowledgeRagStatsMapper;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.rag.KnowledgeRagDashboardService;
import com.edumind.knowledge.vo.rag.KnowledgeRagDashboardVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagPurgeResultVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagSyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRagDashboardServiceImpl implements KnowledgeRagDashboardService {

    private final KnowledgeRagStatsMapper knowledgeRagStatsMapper;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeIndexTaskDao knowledgeIndexTaskDao;
    private final IndexingService indexingService;
    private final LessonContentIndexApi lessonContentIndexApi;
    private final RagRuntimeQueryApi ragRuntimeQueryApi;
    private final AiQueryApi aiQueryApi;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;

    @Override
    public KnowledgeRagDashboardVO getDashboard() {
        RagRuntimeConfigVO runtime = ragRuntimeQueryApi.getRuntimeConfig();
        long indexed = knowledgeRagStatsMapper.countIndexedChunkIndexes();
        long failed = knowledgeRagStatsMapper.countFailedChunkIndexes();
        long totalChunks = knowledgeRagStatsMapper.countTotalChunks();
        long normal = Math.max(0, indexed - failed);
        KnowledgeIndexTaskEntity latestTask = knowledgeIndexTaskDao.findLatestGlobal();
        return KnowledgeRagDashboardVO.builder()
                .totalDocuments(knowledgeRagStatsMapper.countDocuments())
                .indexedDocuments(knowledgeRagStatsMapper.countIndexedDocuments())
                .lessonDocuments(knowledgeRagStatsMapper.countLessonDocuments())
                .uploadDocuments(knowledgeRagStatsMapper.countUploadDocuments())
                .totalChunks(knowledgeRagStatsMapper.countTotalChunks())
                .lessonChunks(knowledgeRagStatsMapper.countLessonChunks())
                .uploadChunks(knowledgeRagStatsMapper.countUploadChunks())
                .indexedChunks(indexed)
                .failedChunks(failed)
                .normalChunks(Math.max(0, indexed - failed))
                .orphanChunkEstimate(knowledgeRagStatsMapper.countOrphanChunks())
                .totalSessions(aiQueryApi.countConversations())
                .embeddingDimension(runtime.getEmbeddingDimensions())
                .embeddingModelName(runtime.getEmbeddingModelName())
                .hybridEnabled(runtime.isHybridEnabled())
                .retrievalModelDescription(runtime.getRetrievalModelDescription())
                .vectorStoreLabel(resolveVectorStoreLabel())
                .indexHealthPercent(resolveIndexHealthPercent(indexed, failed, totalChunks, normal))
                .minRrfScore(runtime.getMinRrfScore())
                .latestIndexStatus(latestTask != null ? latestTask.getStatus() : null)
                .latestIndexTotalChunks(latestTask != null ? latestTask.getTotalChunks() : null)
                .latestIndexIndexedChunks(latestTask != null ? latestTask.getIndexedChunks() : null)
                .latestIndexFailedChunks(latestTask != null ? latestTask.getFailedChunks() : null)
                .latestIndexEmbeddingModel(latestTask != null ? latestTask.getEmbeddingModel() : null)
                .latestIndexStartedAt(latestTask != null ? latestTask.getStartedAt() : null)
                .build();
    }

    private String resolveVectorStoreLabel() {
        if (vectorStore instanceof MilvusVectorStore || milvusProperties.isEnabled()) {
            return "Milvus";
        }
        if (vectorStore instanceof InMemoryVectorStore) {
            return "InMemory（开发）";
        }
        String simple = vectorStore.getClass().getSimpleName();
        if (simple.toLowerCase().contains("pg")) {
            return "PgVector";
        }
        return simple.replace("VectorStore", "");
    }

    private double resolveIndexHealthPercent(long indexed, long failed, long totalChunks, long normal) {
        if (indexed > 0) {
            return Math.min(100.0, Math.round((normal * 1000.0) / indexed) / 10.0);
        }
        return totalChunks <= 0 ? 100.0 : 0.0;
    }

    @Override
    public KnowledgeRagSyncResultVO syncAll() {
        List<KnowledgeBaseEntity> bases = knowledgeBaseDao.findAll();
        int triggered = 0;
        for (KnowledgeBaseEntity base : bases) {
            try {
                indexingService.triggerIndex(base.getId(), "FULL");
                triggered++;
            } catch (Exception ex) {
                log.warn("Full index trigger failed kbId={}: {}", base.getId(), ex.getMessage());
            }
        }
        int lessons = lessonContentIndexApi.reindexPublishedLessons(null);
        return KnowledgeRagSyncResultVO.builder()
                .status("task_started")
                .knowledgeBasesTriggered(triggered)
                .lessonsReindexed(lessons)
                .build();
    }

    @Override
    public KnowledgeRagSyncResultVO syncDocument(Long documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        indexingService.reindexDocument(documentId);
        return KnowledgeRagSyncResultVO.builder()
                .status("ok")
                .documentId(documentId)
                .knowledgeBasesTriggered(1)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeRagPurgeResultVO purgeOrphanChunks() {
        List<Long> orphanIds = knowledgeRagStatsMapper.selectOrphanChunkIds();
        for (Long chunkId : orphanIds) {
            vectorStore.delete(milvusProperties.getCollection(), String.valueOf(chunkId));
        }
        int purgedChunks = knowledgeRagStatsMapper.deleteOrphanChunks();
        int purgedIndex = knowledgeRagStatsMapper.deleteOrphanIndexRows();
        return KnowledgeRagPurgeResultVO.builder()
                .purgedChunks(purgedChunks)
                .purgedIndexRows(purgedIndex)
                .build();
    }

}
