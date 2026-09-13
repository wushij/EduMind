-- =============================================================================
-- V2.0.6 系统管理与 AI 智算中心权限数据补充
-- 智教云 · EduMind
-- =============================================================================

USE edumind;

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(41, 'system:menu:view',        '菜单管理查看', 0),
(42, 'system:menu:edit',        '菜单管理编辑', 0),
(43, 'system:permission:view',  '权限分配矩阵查看', 0),
(44, 'system:tenant:view',      '租户与校区查看', 0),
(45, 'system:tenant:edit',      '租户与校区编辑', 0),
(46, 'system:org:view',         '组织架构查看', 0),
(47, 'system:config:view',      '系统全局配置查看', 0),
(48, 'system:config:edit',      '系统全局配置编辑', 0),
(49, 'system:model:view',       'AI模型接入调度', 0),
(50, 'system:model:edit',       'AI模型接入配置', 0),
(51, 'system:tool:view',        'AI工具调度查看', 0),
(52, 'system:gateway:view',     'AI网关监控查看', 0);

-- 管理员角色赋权
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 41 AND 52;
