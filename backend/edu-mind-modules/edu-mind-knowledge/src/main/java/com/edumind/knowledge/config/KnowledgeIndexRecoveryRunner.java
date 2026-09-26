package com.edumind.knowledge.config;

import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.service.index.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 索引任务启动恢复器。
 *
 * <p>向量索引任务由 {@code knowledgeTaskExecutor} 异步执行。应用重启、线程池中断、
 * 或任务被同一知识库的新任务取代时，都会在 {@code knowledge_index_task} 中留下
 * 「永久 INDEXING、finished_at 为 NULL」的僵尸记录，使 RAG 大盘长期显示
 * 「向量索引任务进行中 · 进度 0 / N」且永不结束。</p>
 *
 * <p>启动时执行一次恢复：把超时仍无进展的任务复位为失败，并重新对齐其知识库的 index_status。
 * 由于是平台级运维动作，需在忽略租户过滤的上下文（{@link TenantContext#callWithoutTenant}）中执行，
 * 才能扫描到全部租户的僵尸任务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIndexRecoveryRunner implements ApplicationRunner {

    /**
     * 超过该分钟数「无任何进展更新」仍停留在 INDEXING 的任务视为中断任务。
     * 索引过程中会周期性刷新进度（update_time 随之更新），异步索引正常以秒级完成，
     * 15 分钟没有任何进度写入基本可判定为应用重启 / 线程中断导致的僵尸任务。
     */
    private static final long STALE_MINUTES = 15;

    private final IndexingService indexingService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int recovered = TenantContext.callWithoutTenant(
                    () -> indexingService.recoverStaleIndexingTasks(STALE_MINUTES));
            if (recovered > 0) {
                log.warn("[索引恢复] 启动时发现并复位 {} 条中断的 INDEXING 僵尸任务", recovered);
            } else {
                log.info("[索引恢复] 未发现中断的索引任务");
            }
        } catch (Exception ex) {
            // 恢复失败不应阻断应用启动
            log.error("[索引恢复] 启动清理中断索引任务失败", ex);
        }
    }
}
