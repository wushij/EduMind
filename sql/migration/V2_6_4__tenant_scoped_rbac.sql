-- =============================================================================
-- V2.6.4 租户内 RBAC 收敛（Gate：切换租户后权限必须真实变化，禁止跨租户越权）
-- -----------------------------------------------------------------------------
-- 背景：
--   原 sys_user_role 无租户维度，用户角色是全局的。导致两类严重缺陷：
--     1. 切换租户后权限集完全不变（菜单/权限不随租户变化）；
--     2. 在 A 校被授予 TENANT_ADMIN 的用户，切到 B 校后仍被判定为「校级管理员」，
--        从而拿到 B 校 allTenant 数据范围与配额管理权（跨租户越权）。
--
-- 方案：
--   sys_user_role 增加 tenant_id：
--     tenant_id = 0   → 平台级角色，对所有租户生效（ADMIN / PLATFORM_ADMIN / ROLE_ADMIN）
--     tenant_id > 0   → 仅在对应租户内生效（TENANT_ADMIN / ORG_ADMIN / TEACHER / STUDENT ...）
--   存量数据按「用户实际加入的租户成员关系」展开为多条，保证能力不丢失、边界不越界。
--   无任何租户成员关系的账号（如内置 admin）保留 tenant_id = 0，语义为平台级。
--
-- 幂等性：脚本可重复执行（列/索引存在则跳过；回填均带存在性判断）。
--
-- 【执行说明 · 重要】
--   1. CLI：mysql -uroot -p edumind < sql/migration/V2_6_4__tenant_scoped_rbac.sql
--      图形客户端（Navicat / Workbench）：请先「选中 edumind 库」再运行本文件；
--      本脚本已含 USE edumind 兜底，但部分客户端批量执行时会为每条语句新建连接，
--      导致 USE 只在第一条语句生效、后续语句「没有默认库」。
--      若遇到 ERROR 1046 (3D000) No database selected，即为此原因 ——
--      解决办法：先双击选中 edumind 库（使其成为当前库）后再执行，或改用 CLI 方式。
--   2. 本脚本刻意**不使用**「多表别名 DELETE/UPDATE」（如 DELETE ur FROM t ur JOIN ...）：
--      该语法在缺少默认库时必然报 1046。回填步骤统一改写为「单表 + 子查询」形式。
--   3. 注意：sys_user_role 刻意不纳入 MyBatis-Plus 租户拦截器白名单——
--      平台级行(tenant_id = 0)必须对所有租户可见，故租户过滤在 DAO 层显式完成。
-- =============================================================================

USE edumind;

-- ---------------------------------------------------------------------------
-- 1. Expand：新增 tenant_id 列（默认 0 = 平台级，保持向后兼容）
-- ---------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `sys_user_role` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT ''租户ID(0=平台级/全租户生效)'' AFTER `role_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 1.1 唯一键升级：uk_user_role(user_id, role_id) → (user_id, role_id, tenant_id)
--     否则同一用户无法在不同租户下持有相同角色（会被唯一键拦截）。
--     校验当前索引列数是否为 2，是则升级；拆成 DROP + ADD 两条语句更稳妥。
-- ---------------------------------------------------------------------------
SET @uk_cols = (SELECT COUNT(DISTINCT COLUMN_NAME) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND INDEX_NAME = 'uk_user_role');
SET @sql = IF(@uk_cols = 2,
    'ALTER TABLE `sys_user_role` DROP INDEX `uk_user_role`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk_cols = (SELECT COUNT(DISTINCT COLUMN_NAME) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND INDEX_NAME = 'uk_user_role');
SET @sql = IF(@uk_cols = 0,
    'ALTER TABLE `sys_user_role` ADD UNIQUE KEY `uk_user_role` (`user_id`, `role_id`, `tenant_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Expand：租户维度联合索引
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND INDEX_NAME = 'idx_ur_tenant_user');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `sys_user_role` ADD KEY `idx_ur_tenant_user` (`tenant_id`, `user_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. Backfill-A：把「非平台级角色」按用户真实加入的租户逐一展开为租户内角色行
--    仅对仍为 0（尚未展开）的行生效，保证可重复执行不产生重复数据
-- ---------------------------------------------------------------------------
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT DISTINCT ur.user_id, ur.role_id, tm.tenant_id
FROM sys_user_role ur
JOIN sys_role r  ON r.id = ur.role_id
JOIN sys_tenant_member tm ON tm.user_id = ur.user_id AND tm.status = 1 AND tm.tenant_id > 0
WHERE ur.tenant_id = 0
  AND r.role_code NOT IN ('ADMIN', 'ROLE_ADMIN', 'PLATFORM_ADMIN')
  AND NOT EXISTS (
        SELECT 1 FROM (SELECT user_id, role_id, tenant_id FROM sys_user_role) x
        WHERE x.user_id = ur.user_id AND x.role_id = ur.role_id AND x.tenant_id = tm.tenant_id
      );

-- ---------------------------------------------------------------------------
-- 4. Backfill-B：清理已完成租户化展开的「占位平台行」
--    单表 + 子查询写法（禁止多表别名 DELETE：缺默认库时会直接报 ERROR 1046）
--    仅删除：非平台级角色 + 用户确实已有租户成员关系（即上面已展开过的行）
-- ---------------------------------------------------------------------------
DELETE FROM sys_user_role
WHERE tenant_id = 0
  AND role_id IN (SELECT id FROM sys_role
                  WHERE role_code NOT IN ('ADMIN', 'ROLE_ADMIN', 'PLATFORM_ADMIN'))
  AND user_id IN (SELECT user_id FROM sys_tenant_member
                  WHERE status = 1 AND tenant_id > 0);

-- ---------------------------------------------------------------------------
-- 5. 兜底：平台级角色显式归零（防止历史脏数据把 ADMIN 写成某个租户）
-- ---------------------------------------------------------------------------
UPDATE sys_user_role
SET tenant_id = 0
WHERE tenant_id <> 0
  AND role_id IN (SELECT id FROM sys_role
                  WHERE role_code IN ('ADMIN', 'ROLE_ADMIN', 'PLATFORM_ADMIN'));

-- ---------------------------------------------------------------------------
-- 6. 校验输出（人工核对）
-- ---------------------------------------------------------------------------
SELECT '=== sys_user_role 租户化结果 ===' AS NOTICE;
SELECT r.role_code, ur.tenant_id, COUNT(*) AS cnt
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id
GROUP BY r.role_code, ur.tenant_id
ORDER BY ur.tenant_id, r.role_code;
