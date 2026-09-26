package com.edumind.knowledge.service.index;

import com.edumind.knowledge.vo.knowledge.IndexStatusVO;

public interface IndexingService {

    void triggerIndex(Long knowledgeBaseId, String mode);

    IndexStatusVO getIndexStatus(Long knowledgeBaseId);

    void reindexDocument(Long documentId);

    void reindexChunk(Long knowledgeBaseId, Long chunkId);

    /**
     * 清理某个文档的向量与索引行（删除文档时调用）。
     *
     * <p>缺失这一步会留下孤儿索引/向量：知识库「已向量化」统计与检索召回都会失真。</p>
     */
    void purgeDocumentVectors(Long documentId);

    /**
     * 内部执行通道：由索引事件监听器在事务提交后异步调用。
     *
     * <p>业务层请统一调用 {@link #triggerIndex(Long, String)}，
     * 直接调用本方法会变回同步执行并阻塞当前请求线程。</p>
     */
    void runIndexTask(Long taskId, Long knowledgeBaseId, String mode, Long operatorId, Long tenantId);

    /**
     * 启动恢复：把超过 {@code staleMinutes} 仍停留在 INDEXING 的任务复位为失败，
     * 并重新对齐其知识库的 index_status。
     *
     * <p>索引任务由异步线程执行，应用重启、线程中断或任务被新任务取代时都会留下
     * 「永久 INDEXING」僵尸记录，使大盘一直显示「进行中 0/N」。此方法用于清理这类脏数据。</p>
     *
     * @return 被复位的任务条数
     */
    int recoverStaleIndexingTasks(long staleMinutes);
}
