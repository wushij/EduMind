-- 导出任务真实进度字段（0-100）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'export_task' AND column_name = 'progress'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE export_task ADD COLUMN progress TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''任务进度0-100'' AFTER status',
    'SELECT ''skip: export_task.progress'' AS migration_note'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
