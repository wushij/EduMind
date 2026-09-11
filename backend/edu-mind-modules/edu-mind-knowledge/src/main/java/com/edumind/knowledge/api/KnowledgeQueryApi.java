package com.edumind.knowledge.api;

import java.util.List;

/**
 * 知识资产只读查询开放 API（提供给 AI 等跨模块调用，严禁跨模块直查 KnowledgeMapper）
 */
public interface KnowledgeQueryApi {

    Object getKnowledgeBaseById(Long knowledgeBaseId);

    List<?> listKnowledgeBasesByCourseId(Long courseId);

    List<?> listDocumentsByKnowledgeBaseId(Long knowledgeBaseId);

    String getDocumentText(Long documentId);
}
