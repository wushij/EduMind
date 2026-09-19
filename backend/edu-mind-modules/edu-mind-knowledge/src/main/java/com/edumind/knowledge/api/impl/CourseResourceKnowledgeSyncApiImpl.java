package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.CourseResourceKnowledgeSyncApi;
import com.edumind.knowledge.dto.CourseResourceKnowledgeSyncDTO;
import com.edumind.knowledge.service.course.CourseResourceKnowledgeSyncService;
import com.edumind.knowledge.service.knowledge.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseResourceKnowledgeSyncApiImpl implements CourseResourceKnowledgeSyncApi {

    private final CourseResourceKnowledgeSyncService courseResourceKnowledgeSyncService;
    private final DocumentService documentService;

    @Override
    public Long syncCourseResource(CourseResourceKnowledgeSyncDTO dto) {
        return courseResourceKnowledgeSyncService.sync(dto);
    }

    @Override
    public void removeLinkedDocument(Long documentId) {
        if (documentId == null) {
            return;
        }
        documentService.delete(documentId);
    }
}
