package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.KnowledgeAccessApi;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeAccessApiImpl implements KnowledgeAccessApi {

    private final KnowledgeAccessService knowledgeAccessService;

    @Override
    public void assertAccessible(Long knowledgeBaseId) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
    }

    @Override
    public void assertCourseAccessible(Long courseId) {
        knowledgeAccessService.assertCourseAccessible(courseId);
    }

    @Override
    public void assertDocumentAccessible(Long documentId, Long knowledgeBaseId) {
        knowledgeAccessService.assertDocumentAccessible(documentId, knowledgeBaseId);
    }
}
