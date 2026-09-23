package com.edumind.knowledge.event;

import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档流水线异步触发监听器。
 *
 * <p>为什么必须等事务提交：文档行由调用方在事务内写入，若在提交前就把 documentId 交给异步线程，
 * 异步线程按 READ_COMMITTED 读不到该行，{@code resolveTenantId} 会返回 null 并直接跳过整条流水线
 * （日志里只有一条 warn：「无法解析 documentId=… 的租户归属」），
 * 表现就是「课件同步进知识库了，但文档永远没有切片和向量」。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentPipelineRequestedListener {

    private final DocumentPipelineService documentPipelineService;

    @Async("knowledgeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPipelineRequested(KnowledgeDocumentPipelineRequestedEvent event) {
        try {
            documentPipelineService.parseAndChunkAsync(event.documentId());
        } catch (Exception ex) {
            log.error("文档流水线触发异常 documentId={}", event.documentId(), ex);
        }
    }
}
