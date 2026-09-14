-- =============================================================================
-- V2.0.17 试卷排版导出异步任务与下载鉴权 (Gate I7)
-- 智教云 · EduMind
-- 可重复执行：列/权限已存在时自动跳过
-- =============================================================================

USE edumind;

-- 1. 扩展 export_task 异步任务与鉴权字段
ALTER TABLE export_task
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT '状态(PENDING/PROCESSING/SUCCESS/FAILED)';

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'export_task' AND column_name = 'error_msg'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE export_task ADD COLUMN error_msg VARCHAR(512) DEFAULT NULL COMMENT ''失败原因'' AFTER status',
    'SELECT ''skip: export_task.error_msg'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'export_task' AND column_name = 'download_token'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE export_task ADD COLUMN download_token VARCHAR(64) DEFAULT NULL COMMENT ''下载鉴权令牌'' AFTER file_url',
    'SELECT ''skip: export_task.download_token'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'export_task' AND column_name = 'object_key'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE export_task ADD COLUMN object_key VARCHAR(512) DEFAULT NULL COMMENT ''OSS ObjectKey'' AFTER download_token',
    'SELECT ''skip: export_task.object_key'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'export_task' AND column_name = 'export_params'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE export_task ADD COLUMN export_params JSON DEFAULT NULL COMMENT ''导出排版参数快照(Beta)'' AFTER object_key',
    'SELECT ''skip: export_task.export_params'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 插入试卷导出权限点（按 code 幂等，避免 id 冲突）
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id)
VALUES ('exam:export', '试卷排版导出', 0);

-- 若历史库 id=56 已被占用，按 code 补齐权限记录
UPDATE sys_permission
SET permission_name = '试卷排版导出', parent_id = 0
WHERE permission_code = 'exam:export';

-- 3. 赋予管理员、教师与校级租户管理员角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
INNER JOIN sys_permission p ON p.permission_code = 'exam:export'
WHERE r.role_code IN ('ADMIN', 'TEACHER', 'TENANT_ADMIN');
