package com.edumind.job.handler.knowledge;

import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPipelineJobHandler {

    private final DocumentPipelineService documentPipelineService;

    public void handleParseAndChunk(Long documentId) {
        log.info("Job: parse and chunk documentId={}", documentId);
        // 走统一业务入口：由监听器在事务提交后异步执行，避免异步线程读不到未提交的文档行
        documentPipelineService.requestPipeline(documentId);
    }
}
