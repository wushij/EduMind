-- 消息广播推送权限（管理员）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, permission_type) VALUES
(39, 'notice:broadcast:view', '广播推送查看', 0),
(40, 'notice:broadcast:send', '广播推送发送', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code IN ('notice:broadcast:view', 'notice:broadcast:send');
