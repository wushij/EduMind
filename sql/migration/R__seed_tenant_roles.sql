-- =============================================================================
-- EduMind V2.0 · 租户管理员与院系管理员角色及权限扩展种子 (可重复执行)
-- 全新空库请直接执行 init.sql；已有旧库升级时单独执行本脚本
-- =============================================================================

-- 1. 扩充角色定义 (TENANT_ADMIN / ORG_ADMIN)
INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(4, 'TENANT_ADMIN', '租户管理员', '校级租户管理员：管理本校全部组织、课程与配额'),
(5, 'ORG_ADMIN',    '院系管理员', '院系/年级管理员：管理所属院系子树及辖下课程与师生');

-- 2. 补充组织管理权限点（配额权限复用已有 system:quota:view / system:quota:edit）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(32, 'system:organization:list',     '组织列表', 0),
(33, 'system:organization:view',     '组织查看', 0),
(34, 'system:organization:create',   '组织创建', 0),
(35, 'system:organization:edit',     '组织编辑', 0),
(36, 'system:organization:update',   '组织更新', 0),
(37, 'system:organization:delete',   '组织删除', 0),
(38, 'system:organization:assign',   '组织成员分配与解绑', 0);

-- 3. 超级管理员 (ADMIN) 自动继承新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE permission_code IN (
    'system:organization:list', 'system:organization:view', 'system:organization:create',
    'system:organization:edit', 'system:organization:update', 'system:organization:delete', 'system:organization:assign'
);

-- 4. 租户管理员 (TENANT_ADMIN) 绑定组织、课程、配额等校级权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission
WHERE permission_code IN (
    'system:user:view', 'system:user:edit',
    'system:organization:list', 'system:organization:view', 'system:organization:create',
    'system:organization:edit', 'system:organization:update', 'system:organization:delete', 'system:organization:assign',
    'system:quota:view', 'system:quota:edit',
    'course:view', 'course:create', 'course:edit',
    'question:view', 'question:edit',
    'exam:view', 'exam:edit',
    'knowledge:view', 'knowledge:edit',
    'analytics:view', 'resource:view', 'resource:upload', 'notice:view', 'ai:chat',
    'ai:memory:view', 'ai:memory:manage',
    'notice:broadcast:view', 'notice:broadcast:send'
);

-- 5. 院系管理员 (ORG_ADMIN) 绑定辖下组织与成员管理权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 5, id FROM sys_permission
WHERE permission_code IN (
    'system:organization:list', 'system:organization:view', 'system:organization:assign',
    'course:view', 'course:create', 'course:edit',
    'question:view', 'question:edit',
    'exam:view', 'exam:edit',
    'knowledge:view', 'analytics:view', 'resource:view', 'ai:chat',
    'ai:memory:view', 'ai:memory:manage'
);
