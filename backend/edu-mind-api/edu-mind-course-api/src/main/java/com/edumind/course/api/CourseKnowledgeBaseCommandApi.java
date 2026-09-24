package com.edumind.course.api;

/**
 * 课程与知识库绑定（供 knowledge 模块在自动建库时回写）
 */
public interface CourseKnowledgeBaseCommandApi {

    void bindKnowledgeBaseId(Long courseId, Long knowledgeBaseId);

    /**
     * 解除所有课程对指定知识库的绑定。
     *
     * <p>course.knowledge_base_id 是无外键约束的裸列，知识库被删除后若不解绑就会留下悬空引用，
     * 课程详情页会持续请求一个已不存在的知识库，对外表现为「知识库不存在」报错。
     * 知识库删除流程必须调用本方法。</p>
     *
     * @param knowledgeBaseId 被删除的知识库 ID
     * @return 受影响的课程数
     */
    int unbindByKnowledgeBaseId(Long knowledgeBaseId);
}
