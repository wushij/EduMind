-- V1.1.0 审计表增加课程上下文维度 (支持按课程维度精确统计 AI 耗用)
-- 说明：若已通过 init.sql 全量初始化，ai_call_log.course_id 已存在，本脚本会自动跳过

SET @db_name = DATABASE();

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'ai_call_log'
      AND COLUMN_NAME = 'course_id'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE ai_call_log ADD COLUMN course_id BIGINT NULL COMMENT ''关联课程ID (0或NULL表示全局/无课程上下文)'' AFTER user_id',
    'SELECT ''skip: ai_call_log.course_id already exists'' AS migration_note'
);
PREPARE stmt FROM @add_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'ai_call_log'
      AND INDEX_NAME = 'idx_call_log_course_time'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE ai_call_log ADD INDEX idx_call_log_course_time (course_id, create_time)',
    'SELECT ''skip: idx_call_log_course_time already exists'' AS migration_note'
);
PREPARE stmt FROM @add_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
