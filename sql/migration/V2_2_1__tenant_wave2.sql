-- =============================================================================
-- V2.2.1 GA Wave2 · 剩余业务表 tenant_id 迁移（16 张）
-- =============================================================================

USE edumind;

-- Expand
ALTER TABLE question_bank ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE question_option ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE question_bank_item ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE grading_result ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE teaching_resource ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE agent_step ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE agent_tool_call ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE prompt_template ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE prompt_template_version ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE sys_user_preference ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER user_id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE statistics_daily_snapshot ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE learning_record ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_mastery ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE wrong_question_record ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE course_statistics ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_point_relation ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);

-- Backfill
UPDATE question_bank qb LEFT JOIN course c ON qb.course_id = c.id SET qb.tenant_id = COALESCE(c.tenant_id, 1) WHERE qb.tenant_id IS NULL;
UPDATE question_option qo INNER JOIN edu_question eq ON qo.question_id = eq.id SET qo.tenant_id = eq.tenant_id WHERE qo.tenant_id IS NULL;
UPDATE question_bank_item qbi INNER JOIN question_bank qb ON qbi.bank_id = qb.id SET qbi.tenant_id = qb.tenant_id WHERE qbi.tenant_id IS NULL;
UPDATE grading_result gr INNER JOIN assignment_submission asub ON gr.submission_id = asub.id SET gr.tenant_id = asub.tenant_id WHERE gr.tenant_id IS NULL;
UPDATE teaching_resource tr LEFT JOIN course c ON tr.course_id = c.id SET tr.tenant_id = COALESCE(c.tenant_id, 1) WHERE tr.tenant_id IS NULL;
UPDATE agent_step ast INNER JOIN agent_run ar ON ast.run_id = ar.run_id SET ast.tenant_id = ar.tenant_id WHERE ast.tenant_id IS NULL;
UPDATE agent_tool_call atc INNER JOIN agent_run ar ON atc.run_id = ar.run_id SET atc.tenant_id = ar.tenant_id WHERE atc.tenant_id IS NULL;
UPDATE prompt_template SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE prompt_template_version ptv INNER JOIN prompt_template pt ON ptv.template_id = pt.id SET ptv.tenant_id = pt.tenant_id WHERE ptv.tenant_id IS NULL;
UPDATE sys_user_preference sup LEFT JOIN sys_tenant_member stm ON sup.user_id = stm.user_id AND stm.is_default = 1 SET sup.tenant_id = COALESCE(stm.tenant_id, 1) WHERE sup.tenant_id IS NULL;
UPDATE statistics_daily_snapshot sds LEFT JOIN course c ON sds.course_id = c.id SET sds.tenant_id = COALESCE(c.tenant_id, 1) WHERE sds.tenant_id IS NULL;
UPDATE learning_record lr INNER JOIN course c ON lr.course_id = c.id SET lr.tenant_id = c.tenant_id WHERE lr.tenant_id IS NULL;
UPDATE knowledge_mastery km INNER JOIN course c ON km.course_id = c.id SET km.tenant_id = c.tenant_id WHERE km.tenant_id IS NULL;
UPDATE wrong_question_record wqr INNER JOIN course c ON wqr.course_id = c.id SET wqr.tenant_id = c.tenant_id WHERE wqr.tenant_id IS NULL;
UPDATE course_statistics cs INNER JOIN course c ON cs.course_id = c.id SET cs.tenant_id = c.tenant_id WHERE cs.tenant_id IS NULL;
UPDATE knowledge_point_relation kpr INNER JOIN course_knowledge_point ckp ON kpr.source_knowledge_point_id = ckp.id SET kpr.tenant_id = ckp.tenant_id WHERE kpr.tenant_id IS NULL;

-- 兜底
UPDATE question_bank SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE question_option SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE question_bank_item SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE grading_result SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE teaching_resource SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE agent_step SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE agent_tool_call SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE prompt_template_version SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE sys_user_preference SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE statistics_daily_snapshot SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE learning_record SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_mastery SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE wrong_question_record SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE course_statistics SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_point_relation SET tenant_id = 1 WHERE tenant_id IS NULL;

-- Enforce NOT NULL
ALTER TABLE question_bank MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE question_option MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE question_bank_item MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE grading_result MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE teaching_resource MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE agent_step MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE agent_tool_call MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE prompt_template MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE prompt_template_version MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE sys_user_preference MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE statistics_daily_snapshot MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE learning_record MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_mastery MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE wrong_question_record MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE course_statistics MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_point_relation MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';

-- 复合唯一键
ALTER TABLE question_bank_item DROP INDEX uk_bank_question;
ALTER TABLE question_bank_item ADD UNIQUE KEY uk_tenant_bank_question (tenant_id, bank_id, question_id);

ALTER TABLE prompt_template DROP INDEX uk_code;
ALTER TABLE prompt_template ADD UNIQUE KEY uk_tenant_code (tenant_id, code);

ALTER TABLE knowledge_mastery DROP INDEX uk_student_kp;
ALTER TABLE knowledge_mastery ADD UNIQUE KEY uk_tenant_student_kp (tenant_id, student_id, knowledge_point_id);

ALTER TABLE course_statistics DROP INDEX uk_course_date;
ALTER TABLE course_statistics ADD UNIQUE KEY uk_tenant_course_date (tenant_id, course_id, stat_date);
