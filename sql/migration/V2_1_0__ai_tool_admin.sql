-- =============================================================================
-- V2.1.0 AI 教学工具管理端字段与权限
-- 智教云 · EduMind
-- =============================================================================

ALTER TABLE ai_tool
    ADD COLUMN sort_order INT DEFAULT 0 COMMENT '展示排序（越小越靠前）' AFTER use_count;

ALTER TABLE ai_tool
    ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time;

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id)
VALUES ('system:tool:edit', 'AI教学工具编辑', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('ADMIN', 'TENANT_ADMIN')
  AND p.permission_code = 'system:tool:edit';
