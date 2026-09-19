package com.edumind.knowledge.api;

import com.edumind.knowledge.dto.CourseResourceKnowledgeSyncDTO;

/**
 * 课程课件 → 关联知识库文档 → 解析切片向量化
 */
public interface CourseResourceKnowledgeSyncApi {

    /**
     * 将已上传至对象存储的课件注册为知识库文档并异步解析、切片、索引。
     *
     * @return 知识库文档 ID
     */
    Long syncCourseResource(CourseResourceKnowledgeSyncDTO dto);

    /**
     * 删除与课件关联的知识库文档（含切片与向量）。
     */
    void removeLinkedDocument(Long documentId);
}
