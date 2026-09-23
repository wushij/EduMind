package com.edumind.knowledge.event;

/**
 * 文档「解析 → 切片 → 向量化」异步流水线执行请求。
 *
 * <p>由文档落库方在事务内发布，监听器在事务提交后异步触发，
 * 避免异步线程读不到尚未提交的文档行而静默跳过整条流水线。</p>
 *
 * @param documentId 知识库文档 ID
 */
public record KnowledgeDocumentPipelineRequestedEvent(Long documentId) {
}
