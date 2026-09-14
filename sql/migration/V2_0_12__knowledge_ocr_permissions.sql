-- =============================================================================
-- V2.0.12 知识库 OCR 识别任务与校对使用权限 (Gate I6)
-- 智教云 · EduMind
-- =============================================================================

USE edumind;

-- 1. 插入 OCR 使用权限点
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id)
VALUES ('knowledge:ocr:use', 'OCR识别与校对使用', 0);

-- 2. 赋予管理员、教师与校级租户管理员角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code IN ('ADMIN', 'TEACHER', 'TENANT_ADMIN')
  AND p.permission_code = 'knowledge:ocr:use';
