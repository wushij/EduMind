package com.edumind.course.api;

/**
 * 课程与知识库绑定（供 knowledge 模块在自动建库时回写）
 */
public interface CourseKnowledgeBaseCommandApi {

    void bindKnowledgeBaseId(Long courseId, Long knowledgeBaseId);
}
