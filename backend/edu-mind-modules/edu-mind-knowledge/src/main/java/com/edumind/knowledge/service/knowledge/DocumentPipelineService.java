package com.edumind.knowledge.service.knowledge;

public interface DocumentPipelineService {

    /**
     * 业务入口：发起「解析 → 切片 → 向量化」。
     *
     * <p>发布领域事件、由监听器在**事务提交后**异步执行；直接调用
     * {@link #parseAndChunkAsync(Long)} 时若当前事务尚未提交，异步线程读不到文档行会导致流水线被静默跳过。</p>
     */
    void requestPipeline(Long documentId);

    /** 内部执行通道：由事件监听器调用，业务层请使用 {@link #requestPipeline(Long)} */
    void parseAndChunkAsync(Long documentId);
}
