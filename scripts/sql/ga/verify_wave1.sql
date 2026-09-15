-- GA Wave1 tenant_id 迁移验证（期望：null_count = 0）
USE edumind;

SELECT 'course_chapter' AS tbl, COUNT(*) AS null_count FROM course_chapter WHERE tenant_id IS NULL
UNION ALL SELECT 'course_member', COUNT(*) FROM course_member WHERE tenant_id IS NULL
UNION ALL SELECT 'course_resource', COUNT(*) FROM course_resource WHERE tenant_id IS NULL
UNION ALL SELECT 'course_knowledge_point', COUNT(*) FROM course_knowledge_point WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_document', COUNT(*) FROM knowledge_document WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_document_text', COUNT(*) FROM knowledge_document_text WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_document_chunk', COUNT(*) FROM knowledge_document_chunk WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_index_task', COUNT(*) FROM knowledge_index_task WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_chunk_index', COUNT(*) FROM knowledge_chunk_index WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_ocr_page', COUNT(*) FROM knowledge_ocr_page WHERE tenant_id IS NULL
UNION ALL SELECT 'teaching_exam', COUNT(*) FROM teaching_exam WHERE tenant_id IS NULL
UNION ALL SELECT 'exam_question', COUNT(*) FROM exam_question WHERE tenant_id IS NULL
UNION ALL SELECT 'assignment', COUNT(*) FROM assignment WHERE tenant_id IS NULL
UNION ALL SELECT 'assignment_submission', COUNT(*) FROM assignment_submission WHERE tenant_id IS NULL
UNION ALL SELECT 'submission_answer', COUNT(*) FROM submission_answer WHERE tenant_id IS NULL
UNION ALL SELECT 'ai_message', COUNT(*) FROM ai_message WHERE tenant_id IS NULL
UNION ALL SELECT 'ai_memory_item', COUNT(*) FROM ai_memory_item WHERE tenant_id IS NULL
UNION ALL SELECT 'ai_memory_feedback', COUNT(*) FROM ai_memory_feedback WHERE tenant_id IS NULL
UNION ALL SELECT 'agent_run', COUNT(*) FROM agent_run WHERE tenant_id IS NULL;

-- 孤儿检测：chapter 与 course 租户不一致
SELECT 'orphan_course_chapter' AS check_name, COUNT(*) AS cnt
FROM course_chapter cc
INNER JOIN course c ON cc.course_id = c.id
WHERE cc.tenant_id <> c.tenant_id;
