-- EduMind V2.0.11 ai_call_log 增加 conversation_id（仅追溯，无外键，删除会话不影响审计）
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_call_log'
      AND COLUMN_NAME = 'conversation_id'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE ai_call_log ADD COLUMN conversation_id VARCHAR(64) NULL COMMENT ''关联会话ID（可选，无外键；删除会话不影响审计）'' AFTER course_id',
    'SELECT ''skip: ai_call_log.conversation_id already exists'' AS migration_note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
