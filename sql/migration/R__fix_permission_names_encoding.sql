-- =============================================================================
-- 修复 sys_permission 权限名称乱码（幂等，可重复执行）
-- 原因：部分 migration 在 Windows 下未指定 utf8mb4 导入，导致中文损坏
-- =============================================================================

USE edumind;

SET NAMES utf8mb4;

UPDATE sys_permission SET permission_name = '广播推送查看' WHERE permission_code = 'notice:broadcast:view';
UPDATE sys_permission SET permission_name = '广播推送发送' WHERE permission_code = 'notice:broadcast:send';
UPDATE sys_permission SET permission_name = '长期记忆查看' WHERE permission_code = 'ai:memory:view';
UPDATE sys_permission SET permission_name = '长期记忆管理' WHERE permission_code = 'ai:memory:manage';
