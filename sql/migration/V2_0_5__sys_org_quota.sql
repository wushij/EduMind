-- =============================================================================
-- V2.0.5 组织与院系级算力配额切分与持久化表 (sys_org_quota)
-- 智教云 · EduMind
-- =============================================================================

CREATE TABLE IF NOT EXISTS `sys_org_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `org_id` BIGINT NOT NULL COMMENT '组织节点ID(关联sys_organization.id)',
    `quota_type` VARCHAR(32) NOT NULL DEFAULT 'TOKEN' COMMENT '配额类型(TOKEN/STORAGE/SEATS)',
    `limit_value` BIGINT NOT NULL DEFAULT 10000000 COMMENT '配额分配上限(Tokens/MB/席位)',
    `used_value` BIGINT NOT NULL DEFAULT 0 COMMENT '当前已使用量',
    `warning_threshold` INT NOT NULL DEFAULT 85 COMMENT '预警水位线百分比',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_org_quota` (`tenant_id`, `org_id`, `quota_type`),
    KEY `idx_tenant_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织院系算力配额分配表';

-- 1. 高中数学教研组 (org_id=2)
INSERT IGNORE INTO `sys_org_quota` (`tenant_id`, `org_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`) VALUES
(1, 2, 'TOKEN', 15000000, 4528000, 85),
(1, 2, 'STORAGE', 150, 42, 85),
(1, 2, 'SEATS', 600, 180, 85);

-- 2. 计算机与信息工程组 (org_id=3)
INSERT IGNORE INTO `sys_org_quota` (`tenant_id`, `org_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`) VALUES
(1, 3, 'TOKEN', 18000000, 5124000, 85),
(1, 3, 'STORAGE', 200, 56, 85),
(1, 3, 'SEATS', 800, 210, 85);

-- 3. 高三(1)班 [理科实验班] (org_id=4)
INSERT IGNORE INTO `sys_org_quota` (`tenant_id`, `org_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`) VALUES
(1, 4, 'TOKEN', 5000000, 1820000, 85),
(1, 4, 'STORAGE', 50, 12, 85),
(1, 4, 'SEATS', 200, 48, 85);

-- 4. 高三(2)班 [数学拔尖班] (org_id=5)
INSERT IGNORE INTO `sys_org_quota` (`tenant_id`, `org_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`) VALUES
(1, 5, 'TOKEN', 5000000, 980000, 85),
(1, 5, 'STORAGE', 50, 8, 85),
(1, 5, 'SEATS', 200, 35, 85);

-- 5. 高二(1)班 [创客先锋班] (org_id=6)
INSERT IGNORE INTO `sys_org_quota` (`tenant_id`, `org_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`) VALUES
(1, 6, 'TOKEN', 4000000, 405000, 85),
(1, 6, 'STORAGE', 30, 6, 85),
(1, 6, 'SEATS', 150, 25, 85);

