-- Wave2 回滚（仅移除 tenant_id 列，生产慎用）
USE edumind;

ALTER TABLE question_bank_item DROP INDEX uk_tenant_bank_question, ADD UNIQUE KEY uk_bank_question (bank_id, question_id);
ALTER TABLE prompt_template DROP INDEX uk_tenant_code, ADD UNIQUE KEY uk_code (code);
ALTER TABLE knowledge_mastery DROP INDEX uk_tenant_student_kp, ADD UNIQUE KEY uk_student_kp (student_id, knowledge_point_id);
ALTER TABLE course_statistics DROP INDEX uk_tenant_course_date, ADD UNIQUE KEY uk_course_date (course_id, stat_date);

ALTER TABLE question_bank DROP COLUMN tenant_id;
ALTER TABLE question_option DROP COLUMN tenant_id;
ALTER TABLE question_bank_item DROP COLUMN tenant_id;
ALTER TABLE grading_result DROP COLUMN tenant_id;
ALTER TABLE teaching_resource DROP COLUMN tenant_id;
ALTER TABLE agent_step DROP COLUMN tenant_id;
ALTER TABLE agent_tool_call DROP COLUMN tenant_id;
ALTER TABLE prompt_template DROP COLUMN tenant_id;
ALTER TABLE prompt_template_version DROP COLUMN tenant_id;
ALTER TABLE sys_user_preference DROP COLUMN tenant_id;
ALTER TABLE statistics_daily_snapshot DROP COLUMN tenant_id;
ALTER TABLE learning_record DROP COLUMN tenant_id;
ALTER TABLE knowledge_mastery DROP COLUMN tenant_id;
ALTER TABLE wrong_question_record DROP COLUMN tenant_id;
ALTER TABLE course_statistics DROP COLUMN tenant_id;
ALTER TABLE knowledge_point_relation DROP COLUMN tenant_id;
