-- =============================================================================
-- V2.0.0 ~ V2.0.6 多租户核心与通知广播（合并迁移）
-- 智教云 · EduMind
--
-- 合并来源：
--   · V2_0_0__multi_tenant_core.sql
--   · V2_0_1__system_config_expansion.sql
--   · V2_0_2__legacy_core_tenant_id.sql
--   · V2_0_3__notification_ref_id.sql
--   · V2_0_4__notification_broadcast.sql
--   · V2_0_5__notification_broadcast_permissions.sql
--   · V2_0_6__system_and_ai_compute_permissions.sql
--
-- 执行：mysql -u root -p edumind < sql/migration/V2_0_0__multi_tenant_and_notification.sql
-- 全新建库请直接执行 sql/init.sql，无需再跑本脚本。
-- =============================================================================

USE edumind;

-- -----------------------------------------------------------------------------
-- [V2_0_0__multi_tenant_core.sql]
-- -----------------------------------------------------------------------------
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
    `consent_status` TINYINT NOT NULL DEFAULT 0 COMMENT '用户授权状态(1:同意, 0:未授权/已撤回)',
    `retention_days` INT NOT NULL DEFAULT 180 COMMENT '记忆留存周期(天)',
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user_course` (`tenant_id`, `user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI长期记忆命名空间表';

-- 9. 长期记忆条目表
CREATE TABLE IF NOT EXISTS `ai_memory_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `namespace_id` BIGINT NOT NULL,
    `memory_type` VARCHAR(32) NOT NULL DEFAULT 'PREFERENCE' COMMENT '记忆类型(PREFERENCE/PROFILE/EPISODIC/FEEDBACK)',
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
    `algorithm` VARCHAR(32) NOT NULL DEFAULT 'SM4_GCM',
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


-- -----------------------------------------------------------------------------
-- [V2_0_1__system_config_expansion.sql]
-- -----------------------------------------------------------------------------
-- ==============================================================================
-- EduMind V2.0.1 系统全局参数配置全面扩充与审计日志表
-- 对齐 12 大系统配置分组：site, session, storage, rateLimit, login, register,
-- thirdParty, payment, sms, mail, security, ai
-- ==============================================================================

-- 1. 短信发送日志审计表
CREATE TABLE IF NOT EXISTS `sys_sms_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `phone` VARCHAR(32) NOT NULL COMMENT '接收手机号',
    `content` VARCHAR(128) DEFAULT NULL COMMENT '验证码或短信内容摘要',
    `sms_type` VARCHAR(64) DEFAULT 'VERIFY_CODE' COMMENT '短信业务类型',
    `template_id` VARCHAR(64) DEFAULT NULL COMMENT '短信模板ID/CODE',
    `template_params` VARCHAR(512) DEFAULT NULL COMMENT '模板参数(JSON)',
    `provider` VARCHAR(32) NOT NULL DEFAULT 'aliyunAuth' COMMENT '服务商(aliyunAuth/tencent)',
    `status` INT NOT NULL DEFAULT 1 COMMENT '发送状态(0-发送中 1-成功 2-失败)',
    `result_msg` VARCHAR(512) DEFAULT NULL COMMENT '回执或错误原因明细',
    `biz_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方回执业务ID',
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `user_id` BIGINT DEFAULT NULL COMMENT '触发用户ID',
    `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '关联业务模块',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '调用方客户端IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发信记录与审计表';

-- 2. 邮件发送日志审计表
CREATE TABLE IF NOT EXISTS `sys_email_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `email` VARCHAR(128) NOT NULL COMMENT '接收邮箱',
    `subject` VARCHAR(256) DEFAULT NULL COMMENT '邮件主题',
    `content` VARCHAR(256) DEFAULT NULL COMMENT '验证码或邮件摘要',
    `scene` VARCHAR(64) DEFAULT 'VERIFY_CODE' COMMENT '邮件应用场景',
    `provider` VARCHAR(32) DEFAULT 'custom' COMMENT '发件服务类型',
    `status` INT NOT NULL DEFAULT 1 COMMENT '发送状态(1-成功 2-失败)',
    `result_msg` VARCHAR(512) DEFAULT NULL COMMENT '回执信息或失败异常',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '调用方客户端IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_email` (`email`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发信记录与审计表';

-- 3. 初始注入系统配置分组记录 (12大业务分组)
INSERT IGNORE INTO `sys_config` (`config_key`, `config_name`, `config_group`, `config_value`, `remark`)
VALUES
('sys.site.config', '平台基础与品牌信息', 'site',
 '{"platformName":"智教云 · EduMind","platformSubtitle":"AI 智能教学赋能平台","loginWelcome":"欢迎登录智教云平台","registerTitle":"开启智教未来之旅","copyright":"Copyright © 2026 EduMind. All rights reserved.","icpEnabled":true,"icpNumber":"京ICP备20260001号-1","icpUrl":"https://beian.miit.gov.cn"}',
 '系统网站名称、副标题、版权与工信部ICP备案信息'),

('sys.session.config', '会话令牌与安全凭据', 'session',
 '{"tokenExpireHours":24,"sessionSignExpireHours":24}',
 'Sa-Token 令牌时效与临时签名/传输密钥生命周期配置'),

('sys.storage.policy', '文件上传限制与扩展名白名单', 'storage',
 '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}',
 '全平台单文件最大体积与允许上传格式白名单'),

('sys.rateLimit.config', '全站敏感接口防刷限流矩阵', 'rateLimit',
 '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10,"smsPerIpMinute":5,"smsSendIntervalSeconds":60,"smsPerPhoneDaily":10,"smsPerIpDaily":30,"aiChatPerUserMinute":8}',
 '登录、注册、人机校验、短信发送与AI对话速率限制'),

('sys.login.config', '用户登录认证与防爆破策略', 'login',
 '{"captchaEnabled":true,"captchaType":"image","smsLoginEnabled":false,"smsLoginSliderCaptchaEnabled":false,"emailLoginEnabled":true,"emailLoginSliderCaptchaEnabled":false,"rememberMe":true,"maxRetryCount":5,"maxRetryCountIp":20,"lockTime":10}',
 '登录人机校验、短信/邮箱验证码登录开关、账户防撞库锁定'),

('sys.register.config', '新用户注册准入与角色分配', 'register',
 '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"STUDENT","needAudit":false,"minPasswordLength":6,"auditorUserIds":[]}',
 '自主注册总开关、人机校验、密码强度、新账号默认角色及审核流'),

('sys.thirdParty.config', '第三方 OAuth 授权登录', 'thirdParty',
 '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}',
 '微信开放平台、支付宝、GitHub、Google OAuth 登录凭据'),

('sys.payment.config', '支付服务网关参数', 'payment',
 '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}',
 '微信支付 APIv3 与支付宝官方支付网关配置'),

('sys.sms.config', '短信发信服务与模板映射', 'sms',
 '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"智教云","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}',
 '阿里云认证/腾讯云短信服务商密钥与业务短信模板映射'),

('sys.ai.config', 'AI 助手全局与 Token 差异化配额', 'ai',
 '{"assistantEnabled":true,"globalKnowledge":"","answerScope":"focus","tokensPerUserDaily":100000,"roleTokenQuotas":[]}',
 '悬浮AI教学助手开关、回答边界及全员/各角色每日Token配额矩阵');


-- -----------------------------------------------------------------------------
-- [V2_0_2__legacy_core_tenant_id.sql]
-- -----------------------------------------------------------------------------
-- ==============================================================================
-- EduMind V2.0.2 核心业务表租户隔离列扩展 (course, knowledge_base, ai_conversation, ai_call_log, edu_question)
-- 保证历史表结构与 MyBatis-Plus 多租户拦截器名单对齐并回填默认租户 (tenant_id = 1)
-- ==============================================================================

-- 1. course 课程表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `course` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. knowledge_base 知识库表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_base' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `knowledge_base` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. ai_conversation AI对话会话表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `ai_conversation` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. ai_call_log AI调用审计日志表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `ai_call_log` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. edu_question 题库题目表
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_question' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `edu_question` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT \'租户ID\' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 统一回填存量数据的默认租户ID
UPDATE `course` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `knowledge_base` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `ai_conversation` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `ai_call_log` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;
UPDATE `edu_question` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0;


-- -----------------------------------------------------------------------------
-- [V2_0_3__notification_ref_id.sql]
-- -----------------------------------------------------------------------------
-- 消息通知：增加关联业务 ID，支持点击跳转
ALTER TABLE sys_notification
    ADD COLUMN ref_id BIGINT DEFAULT NULL COMMENT '关联业务ID' AFTER type;

ALTER TABLE sys_notification
    ADD KEY idx_user_read (user_id, is_read);


-- -----------------------------------------------------------------------------
-- [V2_0_4__notification_broadcast.sql]
-- -----------------------------------------------------------------------------
-- 消息通知：优先级 + 管理员广播任务表
ALTER TABLE sys_notification
    ADD COLUMN priority TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1强弹窗 2跑马灯' AFTER ref_id;

CREATE TABLE IF NOT EXISTS sys_notification_broadcast (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    content         TEXT         NOT NULL,
    target_type     VARCHAR(16)  NOT NULL DEFAULT 'all' COMMENT 'all/role',
    target_payload  VARCHAR(128) DEFAULT NULL COMMENT '角色编码 ADMIN/TEACHER/STUDENT',
    notify_type     VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM',
    priority        TINYINT      NOT NULL DEFAULT 0 COMMENT '0普通 1强弹窗 2跑马灯',
    sender_id       BIGINT       NOT NULL,
    sender_name     VARCHAR(64)  DEFAULT '',
    total_count     INT          NOT NULL DEFAULT 0,
    read_count      INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息广播任务表';


-- -----------------------------------------------------------------------------
-- [V2_0_5__notification_broadcast_permissions.sql]
-- -----------------------------------------------------------------------------
-- 消息广播推送权限（管理员）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, permission_type) VALUES
(39, 'notice:broadcast:view', '广播推送查看', 0),
(40, 'notice:broadcast:send', '广播推送发送', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code IN ('notice:broadcast:view', 'notice:broadcast:send');


-- -----------------------------------------------------------------------------
-- [V2_0_6__system_and_ai_compute_permissions.sql]
-- -----------------------------------------------------------------------------
-- =============================================================================
-- V2.0.6 系统管理与 AI 智算中心权限数据补充
-- 智教云 · EduMind
-- =============================================================================

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


