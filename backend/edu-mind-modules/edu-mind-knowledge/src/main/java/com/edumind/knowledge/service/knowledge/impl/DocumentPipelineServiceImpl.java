package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import com.edumind.knowledge.service.knowledge.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文档解析-切片-索引异步流水线
 * <p>关键约束：异步线程不在请求线程内，ThreadLocal 形式的租户上下文不会自动传递。
 * 本实现先反查文档归属租户，再以该租户上下文执行全流程，并在 finally 中无条件清理，
 * 避免线程池复用导致的「串租户读写」或「租户缺失导致流水线静默失败」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPipelineServiceImpl implements DocumentPipelineService {

    private final DocumentService documentService;
    private final ChunkService chunkService;
    private final IndexingService indexingService;
    private final KnowledgeDocumentDao knowledgeDocumentDao;

    @Override
    @Async("knowledgeTaskExecutor")
    public void parseAndChunkAsync(Long documentId) {
        if (documentId == null) {
            return;
        }
        // 1. 反查文档归属租户（忽略租户过滤，取真实归属）
        Long tenantId = resolveTenantId(documentId);
        if (tenantId == null) {
            log.warn("文档流水线跳过：无法解析 documentId={} 的租户归属", documentId);
            return;
        }
        try {
            // 2. 显式建立租户上下文后再执行，保证拦截器与业务查询均落在正确租户内
            TenantContext.setTenantId(tenantId);
            documentService.triggerParse(documentId);
            chunkService.triggerChunk(documentId);
            indexingService.reindexDocument(documentId);
        } catch (Exception ex) {
            log.warn("文档流水线失败 documentId={} tenantId={}: {}", documentId, tenantId, ex.getMessage());
        } finally {
            // 3. 无条件清理，杜绝线程池复用时的上下文残留
            TenantContext.clear();
        }
    }

    private Long resolveTenantId(Long documentId) {
        // 优先使用当前上下文（同步调用场景），否则反查文档归属租户
        Long current = TenantContext.getTenantId();
        if (current != null && current > 0) {
            return current;
        }
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findByIdIgnoreTenant(documentId);
        return document != null ? document.getTenantId() : null;
    }
}
