-- Agent 中心 / Tool Calling 权限（修复管理员访问 /api/ai/agents 返回 403）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(31, 'ai:tool:use', 'Agent工具调用', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code = 'ai:tool:use';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code = 'ai:tool:use';
