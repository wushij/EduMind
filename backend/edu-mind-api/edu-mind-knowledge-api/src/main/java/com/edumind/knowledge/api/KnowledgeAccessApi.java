package com.edumind.knowledge.api;

/**
 * 知识库访问权限跨模块公开 API
 */
public interface KnowledgeAccessApi {

    void assertAccessible(Long knowledgeBaseId);

    void assertCourseAccessible(Long courseId);

    void assertDocumentAccessible(Long documentId, Long knowledgeBaseId);
}
