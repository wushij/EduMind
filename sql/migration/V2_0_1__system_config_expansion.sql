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
