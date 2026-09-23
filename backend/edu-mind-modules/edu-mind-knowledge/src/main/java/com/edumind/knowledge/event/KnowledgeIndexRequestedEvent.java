package com.edumind.knowledge.event;

/**
 * 知识库索引（向量化）执行请求。
 *
 * <p>由 {@code IndexingServiceImpl.triggerIndex} 在事务内发布，监听器在事务提交后异步执行，
 * 目的是把 embedding 调用与向量库写入从 HTTP 请求线程剥离，避免前端 30 秒超时。</p>
 *
 * @param taskId          已落库的索引任务 ID
 * @param knowledgeBaseId 知识库 ID
 * @param mode            FULL | INCREMENTAL
 * @param operatorId      触发人（用于完成后站内通知，可为空）
 * @param tenantId        租户 ID（异步线程需要显式带上，否则租户拦截器取不到）
 */
public record KnowledgeIndexRequestedEvent(Long taskId,
                                           Long knowledgeBaseId,
                                           String mode,
                                           Long operatorId,
                                           Long tenantId) {
}
