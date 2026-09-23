package com.edumind.knowledge.event;

import com.edumind.knowledge.service.index.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 知识库索引任务执行监听器。
 *
 * <p>这里承担两个此前缺失的职责：</p>
 * <ol>
 *   <li><b>事务提交后再启动</b>：索引任务的 task 行由 triggerIndex 在事务内写入，
 *       若在提交前就把任务交给异步线程，异步线程按 READ_COMMITTED 读不到该行，
 *       会被任务守卫直接跳过，表现为「点了向量化却什么都没发生」；</li>
 *   <li><b>真正异步</b>：原实现是在同一个 Bean 内直接调用 {@code @Async} 方法，
 *       自调用绕过 Spring 代理导致退化为同步执行，embedding 与写向量库全部压在 HTTP 请求线程上，
 *       必然撞上前端 30 秒超时。改为跨 Bean 调用后 {@code @Async} 才会生效。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIndexRequestedListener {

    private final IndexingService indexingService;

    @Async("knowledgeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onIndexRequested(KnowledgeIndexRequestedEvent event) {
        try {
            indexingService.runIndexTask(event.taskId(), event.knowledgeBaseId(), event.mode(),
                    event.operatorId(), event.tenantId());
        } catch (Exception ex) {
            // 异步线程内异常不会冒泡到请求线程，这里兜底记录，避免任务静默消失
            log.error("知识库索引任务执行异常 taskId={} knowledgeBaseId={}",
                    event.taskId(), event.knowledgeBaseId(), ex);
        }
    }
}
