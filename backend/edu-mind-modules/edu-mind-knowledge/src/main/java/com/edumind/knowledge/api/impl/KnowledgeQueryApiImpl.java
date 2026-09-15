package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.service.query.KnowledgeQueryService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryApiImpl implements KnowledgeQueryApi {

    private final KnowledgeQueryService knowledgeQueryService;

    @Override
    public KnowledgeBaseVO getKnowledgeBaseById(Long knowledgeBaseId) {
        return knowledgeQueryService.getKnowledgeBaseById(knowledgeBaseId);
    }

    @Override
    public List<KnowledgeBaseVO> listKnowledgeBasesByCourseId(Long courseId) {
        return knowledgeQueryService.listKnowledgeBasesByCourseId(courseId);
    }

    @Override
    public List<KnowledgeDocumentVO> listDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeQueryService.listDocumentsByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public String getDocumentText(Long documentId) {
        return knowledgeQueryService.getDocumentText(documentId);
    }
}
