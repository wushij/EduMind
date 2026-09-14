package com.edumind.knowledge.service.ocr.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.dao.ocr.KnowledgeOcrDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrPageEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrTaskEntity;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.ocr.KnowledgeOcrService;
import com.edumind.knowledge.service.ocr.KnowledgeOcrTaskDispatcher;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeOcrServiceImpl implements KnowledgeOcrService {

    private final KnowledgeOcrDao knowledgeOcrDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentTextDao knowledgeDocumentTextDao;
    private final ChunkService chunkService;
    private final com.edumind.knowledge.service.chunk.ChunkTriggerExecutor chunkTriggerExecutor;
    private final KnowledgeOcrTaskDispatcher knowledgeOcrTaskDispatcher;

    private void validateTaskTenant(KnowledgeOcrTaskEntity task) {
        if (task == null) {
            return;
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && task.getTenantId() != null && !currentTenantId.equals(task.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他租户的OCR识别任务 (IDOR 越权拦截)");
        }
    }

    @Override
    public KnowledgeOcrTaskVO createOcrTask(Long documentId, String engine) {
        Long tenantId = TenantContext.requireTenantId();

        if (documentId == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "关联文档ID不能为空");
        }

        // 校验文档存在性
        KnowledgeDocumentEntity doc = knowledgeDocumentDao.findById(documentId);
        if (doc == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "关联文档不存在");
        }

        // 校验文档归属租户 (IDOR 越权拦截)
        if (doc.getKnowledgeBaseId() != null) {
            final KnowledgeBaseEntity[] kbHolder = new KnowledgeBaseEntity[1];
            TenantContext.runWithoutTenant(() -> kbHolder[0] = knowledgeBaseDao.findById(doc.getKnowledgeBaseId()));
            KnowledgeBaseEntity kb = kbHolder[0];
            if (kb != null && kb.getTenantId() != null && !tenantId.equals(kb.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他租户的文档 (IDOR 越权拦截)");
            }
        }

        // 校验文档状态
        if (doc.getParseStatus() != null && "DELETED".equalsIgnoreCase(doc.getParseStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "文档已被删除，无法发起OCR识别任务");
        }

        // 插入初始化任务 (初始状态 PENDING, 进度 0)
        KnowledgeOcrTaskEntity task = new KnowledgeOcrTaskEntity();
        task.setTenantId(tenantId);
        task.setDocumentId(documentId);
        task.setEngine(engine != null && !engine.isBlank() ? engine : "PADDLE_OCR");
        task.setTotalPages(0);
        task.setProcessedPages(0);
        task.setStatus("PENDING");
        knowledgeOcrDao.insertTask(task);

        // 异步派发识别任务 (禁止在 create 阶段同步插入 pages)
        knowledgeOcrTaskDispatcher.dispatchAsync(task.getId(), tenantId);

        return toTaskVO(task);
    }

    private KnowledgeOcrTaskVO toTaskVO(KnowledgeOcrTaskEntity entity) {
        KnowledgeOcrTaskVO vo = new KnowledgeOcrTaskVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setEngine(entity.getEngine());
        vo.setTotalPages(entity.getTotalPages());
        vo.setProcessedPages(entity.getProcessedPages());
        vo.setStatus(entity.getStatus());
        int progress = (entity.getTotalPages() != null && entity.getTotalPages() > 0 && entity.getProcessedPages() != null)
                ? (int) Math.round(((double) entity.getProcessedPages() / entity.getTotalPages()) * 100)
                : 0;
        vo.setProgress(progress);
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private KnowledgeOcrTaskEntity requireAccessibleTask(Long taskId) {
        KnowledgeOcrTaskEntity task = knowledgeOcrDao.findTaskByIdIgnoreTenant(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "OCR任务不存在");
        }
        validateTaskTenant(task);
        return task;
    }

    @Override
    public KnowledgeOcrTaskVO getTaskStatus(Long taskId) {
        KnowledgeOcrTaskEntity task = requireAccessibleTask(taskId);
        return toTaskVO(task);
    }

    @Override
    public List<KnowledgeOcrPageVO> getTaskPages(Long taskId) {
        requireAccessibleTask(taskId);

        List<KnowledgeOcrPageEntity> list = knowledgeOcrDao.listPagesByTaskId(taskId);
        return list.stream().map(p -> {
            KnowledgeOcrPageVO vo = new KnowledgeOcrPageVO();
            vo.setId(p.getId());
            vo.setTaskId(p.getTaskId());
            vo.setPageNo(p.getPageNo());
            vo.setRawText(p.getRawText());
            vo.setProofreadText(p.getProofreadText());
            vo.setBlocksJson(p.getBlocksJson());
            vo.setConfidenceScore(p.getConfidenceScore());
            vo.setProofreadStatus(p.getProofreadStatus() != null && p.getProofreadStatus() == 1);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updatePageText(Long pageId, String proofreadText) {
        KnowledgeOcrPageEntity page = knowledgeOcrDao.findPageById(pageId);
        if (page == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "OCR识别页不存在");
        }
        requireAccessibleTask(page.getTaskId());

        page.setProofreadText(proofreadText);
        page.setProofreadStatus(1);
        knowledgeOcrDao.updatePage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmAndIngest(Long taskId) {
        KnowledgeOcrTaskEntity task = requireAccessibleTask(taskId);

        // 仅允许在 PROOFREADING 校对中状态下确认入库
        if (!"PROOFREADING".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "当前任务状态不是校对中(PROOFREADING)，无法确认入库");
        }

        task.setStatus("COMPLETED");
        knowledgeOcrDao.updateTask(task);

        // 联动知识库切片与解析链路
        if (task.getDocumentId() != null) {
            List<KnowledgeOcrPageEntity> pages = knowledgeOcrDao.listPagesByTaskId(taskId);
            String fullProofreadText = pages.stream()
                    .map(p -> p.getProofreadText() != null && !p.getProofreadText().isBlank() ? p.getProofreadText() : p.getRawText())
                    .collect(Collectors.joining("\n\n"));

            KnowledgeDocumentEntity doc = knowledgeDocumentDao.findById(task.getDocumentId());
            if (doc != null) {
                doc.setParseStatus("SUCCESS");
                knowledgeDocumentDao.updateById(doc);

                KnowledgeDocumentTextEntity textEntity = knowledgeDocumentTextDao.findByDocumentId(doc.getId());
                if (textEntity == null) {
                    textEntity = new KnowledgeDocumentTextEntity();
                    textEntity.setDocumentId(doc.getId());
                    textEntity.setContent(fullProofreadText);
                    knowledgeDocumentTextDao.insert(textEntity);
                } else {
                    textEntity.setContent(fullProofreadText);
                    knowledgeDocumentTextDao.updateById(textEntity);
                }

                try {
                    chunkTriggerExecutor.triggerChunkInNewTransaction(doc.getId());
                    log.info("[OCR校对入库] 任务 ID: {} 关联文档 ID: {} 已写入校对全文本并自动触发切片任务", taskId, doc.getId());
                } catch (Exception e) {
                    log.warn("[OCR校对入库] 自动切片异常: {}", e.getMessage());
                }
            }
        }
        log.info("[OCR校对入库] 任务 ID: {} 确认完成", taskId);
    }
}
