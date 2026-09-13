-- ==============================================================================
-- EduMind V2.0.2 核心业务表租户隔离列扩展 (course, knowledge_base, ai_conversation, ai_call_log, edu_question)
-- 保证历史表结构与 MyBatis-Plus 多租户拦截器名单对齐并回填默认租户 (tenant_id = 1)
-- ==============================================================================

-- 1. course 课程表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `course` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. knowledge_base 知识库表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_base' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `knowledge_base` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. ai_conversation AI对话会话表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `ai_conversation` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. ai_call_log AI调用审计日志表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `ai_call_log` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. edu_question 题库题目表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_question' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `edu_question` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 统一回填存量数据的默认租户ID
UPDATE `course` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `knowledge_base` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `ai_conversation` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `ai_call_log` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `edu_question` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
