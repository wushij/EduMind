-- =============================================================================
-- 组织成员分配演示种子（可重复执行）
-- member_id 对应 sys_tenant_member.id（非 user_id）
-- =============================================================================

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
