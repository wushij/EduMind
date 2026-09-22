package com.edumind.knowledge.service.ocr;

import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.dao.ocr.KnowledgeOcrDao;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrPageEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrTaskEntity;
import com.edumind.knowledge.integration.ocr.OcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.OcrPageResult;
import com.edumind.knowledge.integration.ocr.OcrRecognizeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OCR 任务异步调度器 (状态机: PENDING -> PROCESSING -> PROOFREADING / FAILED)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeOcrTaskDispatcher {

    private final KnowledgeOcrDao knowledgeOcrDao;
    private final OcrEngineAdapter ocrEngineAdapter;

    @Async("knowledgeTaskExecutor")
    public void dispatchAsync(Long taskId, Long tenantId) {
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            KnowledgeOcrTaskEntity task = null;
            for (int i = 0; i < 5; i++) {
                task = knowledgeOcrDao.findTaskById(taskId);
                if (task != null) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (task == null) {
                log.warn("[OCR Dispatcher] Task not found: {}", taskId);
                return;
            }
            if (!"PENDING".equals(task.getStatus())) {
                log.info("[OCR Dispatcher] Task {} status is {}, skipping dispatch", taskId, task.getStatus());
                return;
            }

            // 1. 转为 PROCESSING 状态
            task.setStatus("PROCESSING");
            knowledgeOcrDao.updateTask(task);

            // 2. 构造请求调用引擎适配器
            OcrRecognizeRequest request = OcrRecognizeRequest.builder()
                    .taskId(taskId)
                    .documentId(task.getDocumentId())
                    .engine(task.getEngine())
                    .tenantId(tenantId)
                    .build();

            List<OcrPageResult> pageResults = ocrEngineAdapter.recognize(request);
            task.setTotalPages(pageResults != null ? pageResults.size() : 0);

            // 3. 逐页插入识别结果并更新已处理进度
            int processed = 0;
            if (pageResults != null) {
                for (OcrPageResult pr : pageResults) {
                    KnowledgeOcrPageEntity page = new KnowledgeOcrPageEntity();
                    page.setTaskId(taskId);
                    page.setPageNo(pr.getPageNo());
                    page.setRawText(pr.getRawText());
                    page.setProofreadText(pr.getRawText());
                    page.setBlocksJson(pr.getBlocksJson());
                    page.setConfidenceScore(pr.getConfidenceScore());
                    page.setProofreadStatus(0);
                    knowledgeOcrDao.insertPage(page);

                    processed++;
                    task.setProcessedPages(processed);
                    knowledgeOcrDao.updateTask(task);
                }
            }

            // 4. 全部处理完成，进入 PROOFREADING 校对状态
            task.setStatus("PROOFREADING");
            knowledgeOcrDao.updateTask(task);
            log.info("[OCR Dispatcher] Task {} recognition completed with {} pages, entered PROOFREADING", taskId, processed);

        } catch (Exception e) {
            log.error("[OCR Dispatcher] Task {} recognition failed: {}", taskId, e.getMessage(), e);
            try {
                KnowledgeOcrTaskEntity task = knowledgeOcrDao.findTaskById(taskId);
                if (task != null) {
                    task.setStatus("FAILED");
                    String msg = e.getMessage();
                    task.setErrorMsg(msg != null ? (msg.length() > 500 ? msg.substring(0, 500) : msg) : "OCR识别处理异常");
                    knowledgeOcrDao.updateTask(task);
                }
            } catch (Exception ex) {
                log.error("[OCR Dispatcher] Failed to mark task {} as FAILED: {}", taskId, ex.getMessage());
            }
        } finally {
            // 无条件清理：即使 tenantId 为空也必须清理线程上可能残留的上下文，避免线程池复用串租户
            TenantContext.clear();
        }
    }
}
