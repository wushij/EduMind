-- Wave1 回滚（仅移除 tenant_id 列，生产慎用）
USE edumind;

ALTER TABLE course_member DROP INDEX uk_tenant_course_user, ADD UNIQUE KEY uk_course_user (course_id, user_id);
ALTER TABLE assignment_submission DROP INDEX uk_tenant_assignment_student, ADD UNIQUE KEY uk_assignment_student (assignment_id, student_id);

ALTER TABLE course_chapter DROP COLUMN tenant_id;
ALTER TABLE course_member DROP COLUMN tenant_id;
ALTER TABLE course_resource DROP COLUMN tenant_id;
ALTER TABLE course_knowledge_point DROP COLUMN tenant_id;
ALTER TABLE knowledge_document DROP COLUMN tenant_id;
ALTER TABLE knowledge_document_text DROP COLUMN tenant_id;
ALTER TABLE knowledge_document_chunk DROP COLUMN tenant_id;
ALTER TABLE knowledge_index_task DROP COLUMN tenant_id;
ALTER TABLE knowledge_chunk_index DROP COLUMN tenant_id;
ALTER TABLE knowledge_ocr_page DROP COLUMN tenant_id;
ALTER TABLE teaching_exam DROP COLUMN tenant_id;
ALTER TABLE exam_question DROP COLUMN tenant_id;
ALTER TABLE assignment DROP COLUMN tenant_id;
ALTER TABLE assignment_submission DROP COLUMN tenant_id;
ALTER TABLE submission_answer DROP COLUMN tenant_id;
ALTER TABLE ai_message DROP COLUMN tenant_id;
ALTER TABLE ai_memory_item DROP COLUMN tenant_id;
ALTER TABLE ai_memory_feedback DROP COLUMN tenant_id;
ALTER TABLE agent_run DROP COLUMN tenant_id;
