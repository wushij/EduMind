-- =============================================================================
-- EduMind V2.0.10 · 通知广播多租户隔离与权限增强 (Gate I5)
-- =============================================================================

-- 1. sys_notification_broadcast 增加 tenant_id 列与索引（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = DATABASE() AND table_name = 'sys_notification_broadcast' AND column_name = 'tenant_id');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE sys_notification_broadcast ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER id, ADD KEY idx_tenant_id (tenant_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. sys_notification 增加 tenant_id 列与索引（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = DATABASE() AND table_name = 'sys_notification' AND column_name = 'tenant_id');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE sys_notification ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER id, ADD KEY idx_tenant_id (tenant_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 历史数据兜底回填 tenant_id = 1
UPDATE sys_notification_broadcast SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_notification SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- 4. 补充 sys_permission 广播权限点（如果未植入）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(39, 'notice:broadcast:view', '广播推送查看', 0),
(40, 'notice:broadcast:send', '广播推送发送', 0);

-- 5. 为超级管理员 (ADMIN, role_id=1) 绑定广播权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code IN ('notice:broadcast:view', 'notice:broadcast:send');

-- 6. 为校级租户管理员 (TENANT_ADMIN, role_id=4) 绑定广播查看与发送权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE permission_code IN ('notice:broadcast:view', 'notice:broadcast:send');
