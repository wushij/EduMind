-- ==============================================================================
-- EduMind V2.0 学校级 AI 教学平台核心多租户与智能资产表结构
-- ==============================================================================

-- 1. 学校租户主表
CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `code` VARCHAR(64) NOT NULL COMMENT '学校唯一编码',
    `name` VARCHAR(128) NOT NULL COMMENT '学校名称',
    `logo` VARCHAR(255) DEFAULT NULL COMMENT '校徽URL',
    `domain` VARCHAR(128) DEFAULT NULL COMMENT '专属域名',
    `plan_code` VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT '套餐版本(STANDARD/PRO/FLAGSHIP)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1:正常, 0:停用, 2:欠费到期)',
    `expire_time` DATETIME DEFAULT NULL COMMENT '服务到期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校租户主表';

-- 2. 校区表
CREATE TABLE IF NOT EXISTS `sys_campus` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '校区ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `code` VARCHAR(64) NOT NULL COMMENT '校区编码',
    `name` VARCHAR(128) NOT NULL COMMENT '校区名称',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '校区地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1:启用, 0:停用)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_campus` (`tenant_id`, `code`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区信息表';

-- 3. 组织架构树 (院系/专业/班级)
CREATE TABLE IF NOT EXISTS `sys_organization` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '组织ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID',
    `org_type` VARCHAR(32) NOT NULL COMMENT '节点类型(CAMPUS/COLLEGE/MAJOR/CLASS)',
    `org_path` VARCHAR(255) NOT NULL COMMENT '层级路径',
    `name` VARCHAR(128) NOT NULL COMMENT '组织名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校组织架构树表';

-- 4. 学年学期配置表
CREATE TABLE IF NOT EXISTS `sys_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学期ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `school_year` VARCHAR(32) NOT NULL COMMENT '学年(如 2026-2027)',
    `term_code` VARCHAR(32) NOT NULL COMMENT '学期代号(FALL/SPRING)',
    `name` VARCHAR(64) NOT NULL COMMENT '学期全称',
    `start_date` DATE NOT NULL COMMENT '开学日期',
    `end_date` DATE NOT NULL COMMENT '结课日期',
    `is_current` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为当前活跃学期',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_term` (`tenant_id`, `school_year`, `term_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学年学期配置表';

-- 5. 用户与租户成员关联表
CREATE TABLE IF NOT EXISTS `sys_tenant_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `member_no` VARCHAR(64) DEFAULT NULL COMMENT '工号/学号',
    `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '成员状态(1:有效, 0:停用)',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认登录租户',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户租户成员表';

-- 6. 成员组织关系表
CREATE TABLE IF NOT EXISTS `sys_member_org` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `organization_id` BIGINT NOT NULL,
    `role_type` VARCHAR(32) NOT NULL DEFAULT 'STUDENT' COMMENT '身份(HEAD_TEACHER/TEACHER/STUDENT)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_org` (`member_id`, `organization_id`, `role_type`),
    KEY `idx_tenant_org` (`tenant_id`, `organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员组织分配关系表';

-- 7. 租户资源配额表
CREATE TABLE IF NOT EXISTS `sys_tenant_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `quota_type` VARCHAR(32) NOT NULL COMMENT '配额类型(TOKEN/STORAGE/QPS/SEATS)',
    `limit_value` BIGINT NOT NULL COMMENT '上限总额度',
    `used_value` BIGINT NOT NULL DEFAULT 0 COMMENT '当前已使用量',
    `warning_threshold` INT NOT NULL DEFAULT 85 COMMENT '告警阈值百分比',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_quota` (`tenant_id`, `quota_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户资源配额表';

-- 8. 长期记忆命名空间表
CREATE TABLE IF NOT EXISTS `ai_memory_namespace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(NULL代表个人全局)',
    `scope` VARCHAR(32) NOT NULL DEFAULT 'COURSE' COMMENT '作用域(GLOBAL/COURSE)',
    `consent_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户授权状态(1:同意, 0:已撤回)',
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user_course` (`tenant_id`, `user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI长期记忆命名空间表';

-- 9. 长期记忆条目表
CREATE TABLE IF NOT EXISTS `ai_memory_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `namespace_id` BIGINT NOT NULL,
    `summary` VARCHAR(512) NOT NULL COMMENT '记忆摘要内容(明文脱敏)',
    `content_ciphertext` TEXT DEFAULT NULL COMMENT 'SM4加密敏感事实材料',
    `sensitivity_level` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '敏感级别(NORMAL/ACADEMIC/HIGH_RISK)',
    `vector_ref` VARCHAR(128) DEFAULT NULL COMMENT '向量数据库ID引用',
    `expire_time` DATETIME DEFAULT NULL COMMENT '生命周期过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_namespace` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI长期记忆条目明细表';

-- 10. 长期记忆反馈与纠错表
CREATE TABLE IF NOT EXISTS `ai_memory_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `memory_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `feedback_action` VARCHAR(32) NOT NULL COMMENT '操作(FORGET/MODIFY)',
    `correct_content` TEXT DEFAULT NULL,
    `reason` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_memory` (`memory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI长期记忆反馈与纠错记录';

-- 11. 多模态 OCR 任务主表
CREATE TABLE IF NOT EXISTS `knowledge_ocr_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `document_id` BIGINT NOT NULL COMMENT '关联文档ID',
    `engine` VARCHAR(32) NOT NULL DEFAULT 'PADDLE_OCR' COMMENT 'OCR引擎适配器',
    `total_pages` INT NOT NULL DEFAULT 0,
    `processed_pages` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/PROOFREADING/COMPLETED/FAILED)',
    `error_msg` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_doc` (`tenant_id`, `document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR识别任务主表';

-- 12. 多模态 OCR 逐页识别与校对表
CREATE TABLE IF NOT EXISTS `knowledge_ocr_page` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `page_no` INT NOT NULL,
    `raw_text` LONGTEXT DEFAULT NULL COMMENT '识别出的原始文本',
    `proofread_text` LONGTEXT DEFAULT NULL COMMENT '人工校对后的文本',
    `blocks_json` JSON DEFAULT NULL COMMENT '带坐标的识别块JSON',
    `confidence_score` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
    `proofread_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认校对',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_page` (`task_id`, `page_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR逐页识别与校对记录';

-- 13. 异步导出任务表
CREATE TABLE IF NOT EXISTS `export_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型(EXAM_PAPER/TEACHING_REPORT)',
    `biz_id` BIGINT NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态(PROCESSING/SUCCESS/FAILED)',
    `file_url` VARCHAR(512) DEFAULT NULL COMMENT '临时预签名下载地址',
    `expire_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步打印导出任务表';

-- 14. 国密密钥版本元数据表
CREATE TABLE IF NOT EXISTS `security_key_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `key_alias` VARCHAR(64) NOT NULL COMMENT '密钥别名',
    `key_version` INT NOT NULL DEFAULT 1,
    `algorithm` VARCHAR(32) NOT NULL DEFAULT 'SM4_CBC',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/ROTATING/DEPRECATED)',
    `activated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_key` (`tenant_id`, `key_alias`, `key_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国密密钥版本元数据';

-- 15. 教学干预建议决策表
CREATE TABLE IF NOT EXISTS `teaching_intervention` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `trigger_type` VARCHAR(32) NOT NULL COMMENT '触发类型(EXAM_WEAK/ACTIVITY_DROP)',
    `proposal_json` JSON NOT NULL COMMENT '建议内容明细',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/APPROVED/DISPATCHED/REVOKED)',
    `approved_by` BIGINT DEFAULT NULL COMMENT '审批教师ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_course` (`tenant_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学干预建议决策表';

-- 插入默认租户初始数据
INSERT IGNORE INTO `sys_tenant` (`id`, `code`, `name`, `logo`, `domain`, `plan_code`, `status`, `expire_time`)
VALUES
(1, 'ECNU_ATTACHED', '华东师范大学附属实验学校', '/assets/logo.png', 'ecnu.edumind.com', 'FLAGSHIP', 1, '2030-12-31 23:59:59'),
(2, 'FUDAN_DEMO', '复旦实验示范中学', '/assets/logo.png', 'fudan.edumind.com', 'PRO', 1, '2029-12-31 23:59:59');

-- 插入校区初始数据
INSERT IGNORE INTO `sys_campus` (`id`, `tenant_id`, `code`, `name`, `address`, `status`)
VALUES
(1, 1, 'MAIN', '主校区(普陀本部)', '上海市普陀区中山北路3663号', 1),
(2, 1, 'EAST', '闵行新校区', '上海市闵行区东川路500号', 1),
(3, 2, 'MAIN', '江湾主校区', '上海市杨浦区淞沪路2005号', 1);

-- 插入租户组织树
INSERT IGNORE INTO `sys_organization` (`id`, `tenant_id`, `parent_id`, `org_type`, `org_path`, `name`, `sort_order`)
VALUES
(1, 1, 0, 'CAMPUS', '1', '主校区(普陀本部)', 1),
(2, 1, 1, 'COLLEGE', '1/2', '高中数学教研组', 1),
(3, 1, 1, 'COLLEGE', '1/3', '计算机与信息工程组', 2),
(4, 1, 2, 'CLASS', '1/2/4', '高三(1)班 [理科实验班]', 1),
(5, 1, 2, 'CLASS', '1/2/5', '高三(2)班 [数学拔尖班]', 2),
(6, 1, 3, 'CLASS', '1/3/6', '高二(1)班 [创客先锋班]', 1);

-- 插入学期数据
INSERT IGNORE INTO `sys_term` (`id`, `tenant_id`, `school_year`, `term_code`, `name`, `start_date`, `end_date`, `is_current`)
VALUES
(1, 1, '2026-2027', 'FALL', '2026-2027学年秋季学期', '2026-09-01', '2027-01-20', 1),
(2, 1, '2026-2027', 'SPRING', '2026-2027学年春季学期', '2027-02-20', '2027-07-10', 0);

-- 插入默认租户配额
INSERT IGNORE INTO `sys_tenant_quota` (`tenant_id`, `quota_type`, `limit_value`, `used_value`, `warning_threshold`)
VALUES
(1, 'TOKEN', 50000000, 12850000, 85),
(1, 'STORAGE', 500, 128, 80),
(1, 'QPS', 200, 45, 90),
(1, 'SEATS', 2000, 1240, 85),
(2, 'TOKEN', 20000000, 3400000, 85),
(2, 'STORAGE', 200, 45, 80),
(2, 'QPS', 100, 20, 90),
(2, 'SEATS', 800, 320, 85);

-- 插入超级管理员/教师的租户绑定关系
INSERT IGNORE INTO `sys_tenant_member` (`tenant_id`, `user_id`, `member_no`, `real_name`, `status`, `is_default`)
VALUES
(1, 1, 'ADMIN-001', '系统管理员', 1, 1),
(1, 2, 'T2026001', '骨干教师张教授', 1, 1),
(1, 3, 'S2026001', '统招学生李思源', 1, 1),
(2, 1, 'DELEGATE-001', '系统管理员(代管)', 1, 0);
