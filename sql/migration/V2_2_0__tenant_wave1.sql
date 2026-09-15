-- =============================================================================
-- V2.2.0 GA Wave1 · 高泄漏风险子表 tenant_id 迁移（19 张）
-- 智教云 · EduMind
--
-- 执行：mysql -u root -p edumind < sql/migration/V2_2_0__tenant_wave1.sql
-- 验证：mysql -u root -p edumind < scripts/sql/ga/verify_wave1.sql
-- 回滚：mysql -u root -p edumind < sql/rollback/V2_2_rollback_wave1.sql
-- =============================================================================

USE edumind;

-- ---------------------------------------------------------------------------
-- 1. Expand：ADD tenant_id NULL + 索引
-- ---------------------------------------------------------------------------

-- course 子表
ALTER TABLE course_chapter ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE course_member ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE course_resource ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE course_knowledge_point ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);

-- knowledge 子表
ALTER TABLE knowledge_document ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_document_text ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_document_chunk ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_index_task ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_chunk_index ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE knowledge_ocr_page ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);

-- exam / teaching 子表
ALTER TABLE teaching_exam ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE exam_question ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE assignment ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE assignment_submission ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE submission_answer ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);

-- ai 子表
ALTER TABLE ai_message ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE ai_memory_item ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE ai_memory_feedback ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE agent_run ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER id, ADD KEY idx_tenant_id (tenant_id);

-- ---------------------------------------------------------------------------
-- 2. Backfill：JOIN 父表推导
-- ---------------------------------------------------------------------------

UPDATE course_chapter cc INNER JOIN course c ON cc.course_id = c.id SET cc.tenant_id = c.tenant_id WHERE cc.tenant_id IS NULL;
UPDATE course_member cm INNER JOIN course c ON cm.course_id = c.id SET cm.tenant_id = c.tenant_id WHERE cm.tenant_id IS NULL;
UPDATE course_resource cr INNER JOIN course c ON cr.course_id = c.id SET cr.tenant_id = c.tenant_id WHERE cr.tenant_id IS NULL;
UPDATE course_knowledge_point ckp INNER JOIN course c ON ckp.course_id = c.id SET ckp.tenant_id = c.tenant_id WHERE ckp.tenant_id IS NULL;

UPDATE knowledge_document kd INNER JOIN knowledge_base kb ON kd.knowledge_base_id = kb.id SET kd.tenant_id = kb.tenant_id WHERE kd.tenant_id IS NULL;
UPDATE knowledge_document_text kdt INNER JOIN knowledge_document kd ON kdt.document_id = kd.id SET kdt.tenant_id = kd.tenant_id WHERE kdt.tenant_id IS NULL;
UPDATE knowledge_document_chunk kdc INNER JOIN knowledge_base kb ON kdc.knowledge_base_id = kb.id SET kdc.tenant_id = kb.tenant_id WHERE kdc.tenant_id IS NULL;
UPDATE knowledge_index_task kit INNER JOIN knowledge_base kb ON kit.knowledge_base_id = kb.id SET kit.tenant_id = kb.tenant_id WHERE kit.tenant_id IS NULL;
UPDATE knowledge_chunk_index kci INNER JOIN knowledge_base kb ON kci.knowledge_base_id = kb.id SET kci.tenant_id = kb.tenant_id WHERE kci.tenant_id IS NULL;
UPDATE knowledge_ocr_page kop INNER JOIN knowledge_ocr_task kot ON kop.task_id = kot.id SET kop.tenant_id = kot.tenant_id WHERE kop.tenant_id IS NULL;

UPDATE teaching_exam te INNER JOIN course c ON te.course_id = c.id SET te.tenant_id = c.tenant_id WHERE te.tenant_id IS NULL;
UPDATE exam_question eq INNER JOIN teaching_exam te ON eq.exam_id = te.id SET eq.tenant_id = te.tenant_id WHERE eq.tenant_id IS NULL;
UPDATE assignment a INNER JOIN course c ON a.course_id = c.id SET a.tenant_id = c.tenant_id WHERE a.tenant_id IS NULL;
UPDATE assignment_submission asub INNER JOIN assignment a ON asub.assignment_id = a.id SET asub.tenant_id = a.tenant_id WHERE asub.tenant_id IS NULL;
UPDATE submission_answer sa INNER JOIN assignment_submission asub ON sa.submission_id = asub.id SET sa.tenant_id = asub.tenant_id WHERE sa.tenant_id IS NULL;

UPDATE ai_message am INNER JOIN ai_conversation ac ON am.conversation_id = ac.id SET am.tenant_id = ac.tenant_id WHERE am.tenant_id IS NULL;
UPDATE ai_memory_item ami INNER JOIN ai_memory_namespace amn ON ami.namespace_id = amn.id SET ami.tenant_id = amn.tenant_id WHERE ami.tenant_id IS NULL;
UPDATE ai_memory_feedback amf INNER JOIN ai_memory_item ami ON amf.memory_id = ami.id SET amf.tenant_id = ami.tenant_id WHERE amf.tenant_id IS NULL;
UPDATE agent_run ar LEFT JOIN course c ON ar.course_id = c.id SET ar.tenant_id = COALESCE(c.tenant_id, 1) WHERE ar.tenant_id IS NULL;

-- 兜底默认租户
UPDATE course_chapter SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE course_member SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE course_resource SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE course_knowledge_point SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_document SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_document_text SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_document_chunk SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_index_task SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_chunk_index SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE knowledge_ocr_page SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE teaching_exam SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE exam_question SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE assignment SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE assignment_submission SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE submission_answer SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE ai_message SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE ai_memory_item SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE ai_memory_feedback SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE agent_run SET tenant_id = 1 WHERE tenant_id IS NULL;

-- ---------------------------------------------------------------------------
-- 3. Enforce：NOT NULL + 复合唯一键
-- ---------------------------------------------------------------------------

ALTER TABLE course_chapter MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE course_member MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE course_resource MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE course_knowledge_point MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_document MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_document_text MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_document_chunk MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_index_task MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_chunk_index MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE knowledge_ocr_page MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE teaching_exam MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE exam_question MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE assignment MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE assignment_submission MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE submission_answer MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE ai_message MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE ai_memory_item MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE ai_memory_feedback MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE agent_run MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';

-- 复合唯一键（course_member / assignment_submission）
ALTER TABLE course_member DROP INDEX uk_course_user;
ALTER TABLE course_member ADD UNIQUE KEY uk_tenant_course_user (tenant_id, course_id, user_id);

ALTER TABLE assignment_submission DROP INDEX uk_assignment_student;
ALTER TABLE assignment_submission ADD UNIQUE KEY uk_tenant_assignment_student (tenant_id, assignment_id, student_id);
