package com.edumind.job.handler.knowledge;

import com.edumind.knowledge.service.ocr.KnowledgeOcrTaskDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OCR 任务调度 Handler (供定时任务/XXL-Job/异步消息触发)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeOcrJobHandler {

    private final KnowledgeOcrTaskDispatcher dispatcher;

    public void handleProcessTask(Long taskId, Long tenantId) {
        log.info("Job: dispatch OCR task taskId={}, tenantId={}", taskId, tenantId);
        dispatcher.dispatchAsync(taskId, tenantId);
    }
}
