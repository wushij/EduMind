-- =============================================================================
-- 旧库升级补丁种子（幂等，可重复执行）
-- 适用：V2.0.2 之前建的库、或未跑完整 init 的增量升级环境
-- 全新空库请直接执行 sql/init.sql，通常无需本脚本
-- =============================================================================

USE edumind;

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 租户/院系管理员角色与组织权限（原 R__seed_tenant_roles）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(4, 'TENANT_ADMIN', '租户管理员', '校级租户管理员：管理本校全部组织、课程与配额'),
(5, 'ORG_ADMIN',    '院系管理员', '院系/年级管理员：管理所属院系子树及辖下课程与师生');

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(32, 'system:organization:list',     '组织列表', 0),
(33, 'system:organization:view',     '组织查看', 0),
(34, 'system:organization:create',   '组织创建', 0),
(35, 'system:organization:edit',     '组织编辑', 0),
(36, 'system:organization:update',   '组织更新', 0),
(37, 'system:organization:delete',   '组织删除', 0),
(38, 'system:organization:assign',   '组织成员分配与解绑', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE permission_code IN (
    'system:organization:list', 'system:organization:view', 'system:organization:create',
    'system:organization:edit', 'system:organization:update', 'system:organization:delete', 'system:organization:assign'
);

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

-- -----------------------------------------------------------------------------
-- 2. 组织成员分配演示（原 R__seed_member_org，依赖 V2 多租户表）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_tenant_member (tenant_id, user_id, member_no, real_name, status, is_default) VALUES
(1, 4, 'S2026002', '统招学生王浩然', 1, 0);

INSERT IGNORE INTO sys_member_org (tenant_id, member_id, organization_id, role_type)
SELECT 1, tm.id, 4, 'HEAD_TEACHER'
FROM sys_tenant_member tm
WHERE tm.tenant_id = 1 AND tm.user_id = 2
LIMIT 1;

INSERT IGNORE INTO sys_member_org (tenant_id, member_id, organization_id, role_type)
SELECT 1, tm.id, 4, 'STUDENT'
FROM sys_tenant_member tm
WHERE tm.tenant_id = 1 AND tm.user_id = 3
LIMIT 1;

INSERT IGNORE INTO sys_member_org (tenant_id, member_id, organization_id, role_type)
SELECT 1, tm.id, 4, 'STUDENT'
FROM sys_tenant_member tm
WHERE tm.tenant_id = 1 AND tm.user_id = 4
LIMIT 1;

INSERT IGNORE INTO sys_member_org (tenant_id, member_id, organization_id, role_type)
SELECT 1, tm.id, 5, 'STUDENT'
FROM sys_tenant_member tm
WHERE tm.tenant_id = 1 AND tm.user_id = 3
LIMIT 1;

INSERT IGNORE INTO sys_member_org (tenant_id, member_id, organization_id, role_type)
SELECT 1, tm.id, 2, 'TEACHER'
FROM sys_tenant_member tm
WHERE tm.tenant_id = 1 AND tm.user_id = 2
LIMIT 1;

-- -----------------------------------------------------------------------------
-- 3. 修复权限名称乱码（原 R__fix_permission_names_encoding）
-- -----------------------------------------------------------------------------
UPDATE sys_permission SET permission_name = '广播推送查看' WHERE permission_code = 'notice:broadcast:view';
UPDATE sys_permission SET permission_name = '广播推送发送' WHERE permission_code = 'notice:broadcast:send';
UPDATE sys_permission SET permission_name = '长期记忆查看' WHERE permission_code = 'ai:memory:view';
UPDATE sys_permission SET permission_name = '长期记忆管理' WHERE permission_code = 'ai:memory:manage';

-- -----------------------------------------------------------------------------
-- 4. 清除演示账号旧种子联系方式（原 R__clear_default_user_contact）
-- -----------------------------------------------------------------------------
UPDATE sys_user
SET email = NULL,
    phone = NULL
WHERE username IN ('admin', 'teacher', 'student', 'student2');
