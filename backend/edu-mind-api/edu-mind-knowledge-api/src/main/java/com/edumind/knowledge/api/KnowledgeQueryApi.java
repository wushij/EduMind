package com.edumind.knowledge.api;

import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;

import java.util.List;

/**
 * 知识资产只读查询开放 API（提供给 AI 等跨模块调用，严禁跨模块直查 KnowledgeMapper）
 */
public interface KnowledgeQueryApi {

    KnowledgeBaseVO getKnowledgeBaseById(Long knowledgeBaseId);

    List<KnowledgeBaseVO> listKnowledgeBasesByCourseId(Long courseId);

    List<KnowledgeDocumentVO> listDocumentsByKnowledgeBaseId(Long knowledgeBaseId);

    String getDocumentText(Long documentId);

    KnowledgeDocumentVO getDocumentById(Long documentId);
}
