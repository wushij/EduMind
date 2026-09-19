-- 课程课件与知识库文档关联（RAG 同步溯源）
ALTER TABLE knowledge_document
    ADD COLUMN course_resource_id BIGINT DEFAULT NULL COMMENT '关联 course_resource.id' AFTER course_id;

CREATE INDEX idx_knowledge_doc_course_resource ON knowledge_document (course_resource_id);
