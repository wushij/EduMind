-- GA Wave2 tenant_id 迁移验证
USE edumind;

SELECT 'question_bank' AS tbl, COUNT(*) AS null_count FROM question_bank WHERE tenant_id IS NULL
UNION ALL SELECT 'question_option', COUNT(*) FROM question_option WHERE tenant_id IS NULL
UNION ALL SELECT 'question_bank_item', COUNT(*) FROM question_bank_item WHERE tenant_id IS NULL
UNION ALL SELECT 'grading_result', COUNT(*) FROM grading_result WHERE tenant_id IS NULL
UNION ALL SELECT 'teaching_resource', COUNT(*) FROM teaching_resource WHERE tenant_id IS NULL
UNION ALL SELECT 'agent_step', COUNT(*) FROM agent_step WHERE tenant_id IS NULL
UNION ALL SELECT 'agent_tool_call', COUNT(*) FROM agent_tool_call WHERE tenant_id IS NULL
UNION ALL SELECT 'prompt_template', COUNT(*) FROM prompt_template WHERE tenant_id IS NULL
UNION ALL SELECT 'prompt_template_version', COUNT(*) FROM prompt_template_version WHERE tenant_id IS NULL
UNION ALL SELECT 'sys_user_preference', COUNT(*) FROM sys_user_preference WHERE tenant_id IS NULL
UNION ALL SELECT 'statistics_daily_snapshot', COUNT(*) FROM statistics_daily_snapshot WHERE tenant_id IS NULL
UNION ALL SELECT 'learning_record', COUNT(*) FROM learning_record WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_mastery', COUNT(*) FROM knowledge_mastery WHERE tenant_id IS NULL
UNION ALL SELECT 'wrong_question_record', COUNT(*) FROM wrong_question_record WHERE tenant_id IS NULL
UNION ALL SELECT 'course_statistics', COUNT(*) FROM course_statistics WHERE tenant_id IS NULL
UNION ALL SELECT 'knowledge_point_relation', COUNT(*) FROM knowledge_point_relation WHERE tenant_id IS NULL;
