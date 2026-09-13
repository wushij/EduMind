-- =============================================================================
-- V2.0.7 AI 长期记忆生命周期与权限补充
-- 智教云 · EduMind (支持多次重复执行)
-- =============================================================================

USE edumind;

-- 1. 修改默认授权状态为 0 (未授权)
ALTER TABLE `ai_memory_namespace`
    MODIFY COLUMN `consent_status` TINYINT NOT NULL DEFAULT 0 COMMENT '用户授权状态(1:同意, 0:未授权/已撤回)';

-- 2. 幂等新增 retention_days 与 memory_type 字段 (若已存在则安全跳过)
DROP PROCEDURE IF EXISTS `_temp_add_memory_columns`;
DELIMITER $$
CREATE PROCEDURE `_temp_add_memory_columns`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_memory_namespace' AND column_name = 'retention_days'
    ) THEN
        ALTER TABLE `ai_memory_namespace` ADD COLUMN `retention_days` INT NOT NULL DEFAULT 180 COMMENT '记忆留存周期(天)' AFTER `consent_status`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_memory_item' AND column_name = 'memory_type'
    ) THEN
        ALTER TABLE `ai_memory_item` ADD COLUMN `memory_type` VARCHAR(32) NOT NULL DEFAULT 'PREFERENCE' COMMENT '记忆类型(PREFERENCE/PROFILE/EPISODIC/FEEDBACK)' AFTER `namespace_id`;
    END IF;
END$$
DELIMITER ;
CALL `_temp_add_memory_columns`();
DROP PROCEDURE IF EXISTS `_temp_add_memory_columns`;

-- 3. 补充长期记忆查看与管理权限点
INSERT IGNORE INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `parent_id`) VALUES
(53, 'ai:memory:view',   '长期记忆查看', 0),
(54, 'ai:memory:manage', '长期记忆管理', 0);

-- 超级管理员 (1) 自动继承记忆权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `id` IN (53, 54);

-- 教师 (2) 自动赋权
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission` WHERE `id` IN (53, 54);

-- 学生 (3) 自动赋权以支持个人长期记忆查看与知情管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, `id` FROM `sys_permission` WHERE `id` IN (53, 54);

-- 租户管理员 (4) 与 院系管理员 (5) 赋权
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 4, `id` FROM `sys_permission` WHERE `id` IN (53, 54);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 5, `id` FROM `sys_permission` WHERE `id` IN (53, 54);
