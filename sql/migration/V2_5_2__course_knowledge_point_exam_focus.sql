-- -----------------------------------------------------------------------------
-- V2.5.2: 课程知识点考查重点字段
-- -----------------------------------------------------------------------------

USE edumind;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course_knowledge_point'
      AND COLUMN_NAME = 'exam_focus'
);

SET @ddl = IF(
    @col_exists = 0,
    'ALTER TABLE course_knowledge_point ADD COLUMN exam_focus VARCHAR(256) NULL COMMENT ''考查易错/重点'' AFTER importance',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
