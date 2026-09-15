package com.edumind.knowledge.service.query;

import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;

import java.util.List;

public interface KnowledgeQueryService {

    KnowledgeBaseVO getKnowledgeBaseById(Long knowledgeBaseId);

    List<KnowledgeBaseVO> listKnowledgeBasesByCourseId(Long courseId);

    List<KnowledgeDocumentVO> listDocumentsByKnowledgeBaseId(Long knowledgeBaseId);

    String getDocumentText(Long documentId);
}
