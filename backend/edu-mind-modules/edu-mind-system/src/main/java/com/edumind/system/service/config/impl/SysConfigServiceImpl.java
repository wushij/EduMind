package com.edumind.system.service.config.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.mail.MailClient;
import com.edumind.infrastructure.mail.MailConfig;
import com.edumind.infrastructure.oss.StorageConfig;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.system.converter.SysConfigConverter;
import com.edumind.system.dao.SysConfigDao;
import com.edumind.system.dto.config.MailConfigDTO;
import com.edumind.system.dto.config.MailTestDTO;
import com.edumind.system.entity.SysConfigEntity;
import com.edumind.system.service.config.SysConfigService;
import com.edumind.system.vo.config.MailConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    public static final String MAIL_CONFIG_KEY = "sys.mail.config";

    public static final String STORAGE_CONFIG_KEY = "sys.storage.config";
    public static final String SECURITY_CONFIG_KEY = "sys.security.config";

    private final SysConfigDao sysConfigDao;
    private final SysConfigConverter sysConfigConverter;
    private final MailClient mailClient;
    private final RedisService redisService;
    private final com.edumind.infrastructure.oss.impl.RoutingFileStorageService routingFileStorageService;
    private final com.edumind.security.config.DynamicSecurityConfigService dynamicSecurityConfigService;
    private final com.edumind.system.dao.SysSmsLogDao sysSmsLogDao;
    private final com.edumind.system.dao.SysEmailLogDao sysEmailLogDao;
    private final com.edumind.system.dao.RoleDao roleDao;
    private final com.edumind.system.dao.UserDao userDao;

    @jakarta.annotation.PostConstruct
    public void initStorageAndSecurity() {
        try {
            StorageConfig savedStorage = getRawStorageConfig();
            if (savedStorage != null && org.springframework.util.StringUtils.hasText(savedStorage.getType())) {
                routingFileStorageService.reload(savedStorage);
            }
        } catch (Exception e) {
            log.warn("初始化加载持久化存储配置异常: {}", e.getMessage());
        }
    }

    @Override
    public com.edumind.system.vo.config.StorageConfigVO getStorageConfigVO() {
        StorageConfig current = getRawStorageConfig();
        String activeType = routingFileStorageService.getStorageType();
        boolean healthy = routingFileStorageService.testConnection();

        String localPath = current.getLocalPath();
        if (!org.springframework.util.StringUtils.hasText(localPath)) {
            localPath = routingFileStorageService.getResolvedStoragePath();
        }
        if (!org.springframework.util.StringUtils.hasText(localPath)) {
            localPath = "backend/data";
        }

        return com.edumind.system.vo.config.StorageConfigVO.builder()
                .type(current.getType())
                .localPath(localPath)
                .minioEndpoint(current.getMinioEndpoint())
                .minioAccessKey(current.getMinioAccessKey())
                .minioBucket(current.getMinioBucket())
                .minioSecretKeyConfigured(org.springframework.util.StringUtils.hasText(current.getMinioSecretKey()))
                .cosSecretId(current.getCosSecretId())
                .cosRegion(current.getCosRegion())
                .cosBucket(current.getCosBucket())
                .cosDomain(current.getCosDomain())
                .cosSecretKeyConfigured(org.springframework.util.StringUtils.hasText(current.getCosSecretKey()))
                .ossEndpoint(current.getOssEndpoint())
                .ossAccessKeyId(current.getOssAccessKeyId())
                .ossBucket(current.getOssBucket())
                .ossDomain(current.getOssDomain())
                .ossAccessKeySecretConfigured(org.springframework.util.StringUtils.hasText(current.getOssAccessKeySecret()))
                .activeType(activeType)
                .activeBucket(getActiveBucketOrPath(current, activeType))
                .healthy(healthy)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.edumind.system.vo.config.StorageConfigVO updateStorageConfig(com.edumind.system.dto.config.StorageConfigDTO dto) {
        StorageConfig existing = getRawStorageConfig();
        StorageConfig updated = mergeStorageConfig(existing, dto);

        // 1. 尝试热重载底层驱动
        routingFileStorageService.reload(updated);

        // 2. 保存至数据库与 Redis
        String json = JSON.toJSONString(updated);
        SysConfigEntity entity = sysConfigDao.findByKey(STORAGE_CONFIG_KEY);
        if (entity == null) {
            entity = SysConfigEntity.builder()
                    .configKey(STORAGE_CONFIG_KEY)
                    .configName("文件与对象存储引擎配置")
                    .configGroup("storage")
                    .configValue(json)
                    .remark("存储引擎热切换配置(local/minio/cos/oss)")
                    .build();
            sysConfigDao.insert(entity);
        } else {
            entity.setConfigValue(json);
            sysConfigDao.updateById(entity);
        }

        try {
            String cacheKey = RedisKeyBuilder.sysConfig(STORAGE_CONFIG_KEY);
            redisService.set(cacheKey, json, 86400 * 30);
        } catch (Exception ignored) {
        }
        log.info("系统文件存储配置已更新并成功热切换: type={}", updated.getType());
        return getStorageConfigVO();
    }

    @Override
    public boolean testStorageConfig(com.edumind.system.dto.config.StorageConfigDTO dto) {
        StorageConfig existing = getRawStorageConfig();
        StorageConfig probeConfig = mergeStorageConfig(existing, dto);
        return routingFileStorageService.testConnection(probeConfig);
    }

    @Override
    public com.edumind.system.vo.config.SecurityConfigVO getSecurityConfigVO() {
        Map<String, Object> map = dynamicSecurityConfigService.getConfig();
        return com.edumind.system.vo.config.SecurityConfigVO.builder()
                .timestampEnabled(Boolean.TRUE.equals(map.get("timestampEnabled")))
                .timestampWindowMs(map.get("timestampWindowMs") instanceof Number n ? n.longValue() : 300000L)
                .nonceEnabled(Boolean.TRUE.equals(map.get("nonceEnabled")))
                .sm3SignEnabled(Boolean.TRUE.equals(map.get("sm3SignEnabled")))
                .sm4EncryptEnabled(Boolean.TRUE.equals(map.get("sm4EncryptEnabled")))
                .captchaAfterFailures(map.get("captchaAfterFailures") instanceof Number n ? n.intValue() : 3)
                .captchaOnRegister(map.get("captchaOnRegister") == null || Boolean.TRUE.equals(map.get("captchaOnRegister")))
                .disableDevtool(Boolean.TRUE.equals(map.get("disableDevtool")))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.edumind.system.vo.config.SecurityConfigVO updateSecurityConfig(com.edumind.system.dto.config.SecurityConfigDTO dto) {
        Map<String, Object> req = new java.util.HashMap<>();
        if (dto.getTimestampEnabled() != null) req.put("timestampEnabled", dto.getTimestampEnabled());
        if (dto.getTimestampWindowMs() != null) req.put("timestampWindowMs", dto.getTimestampWindowMs());
        if (dto.getNonceEnabled() != null) req.put("nonceEnabled", dto.getNonceEnabled());
        if (dto.getSm3SignEnabled() != null) req.put("sm3SignEnabled", dto.getSm3SignEnabled());
        if (dto.getSm4EncryptEnabled() != null) req.put("sm4EncryptEnabled", dto.getSm4EncryptEnabled());
        if (dto.getCaptchaAfterFailures() != null) req.put("captchaAfterFailures", dto.getCaptchaAfterFailures());
        if (dto.getCaptchaOnRegister() != null) req.put("captchaOnRegister", dto.getCaptchaOnRegister());
        if (dto.getDisableDevtool() != null) req.put("disableDevtool", dto.getDisableDevtool());

        Map<String, Object> updated = dynamicSecurityConfigService.updateConfig(req);

        // 同步持久化至数据库
        String json = JSON.toJSONString(updated);
        SysConfigEntity entity = sysConfigDao.findByKey(SECURITY_CONFIG_KEY);
        if (entity == null) {
            entity = SysConfigEntity.builder()
                    .configKey(SECURITY_CONFIG_KEY)
                    .configName("全链路安全防护控制矩阵配置")
                    .configGroup("security")
                    .configValue(json)
                    .remark("时间戳/防重放/国密SM3/SM4/验证码/防调试开关")
                    .build();
            sysConfigDao.insert(entity);
        } else {
            entity.setConfigValue(json);
            sysConfigDao.updateById(entity);
        }
        return getSecurityConfigVO();
    }

    private StorageConfig getRawStorageConfig() {
        String cacheKey = RedisKeyBuilder.sysConfig(STORAGE_CONFIG_KEY);
        try {
            String cached = redisService.get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                StorageConfig cfg = JSON.parseObject(cached, StorageConfig.class);
                if (cfg != null) return cfg;
            }
        } catch (Exception ignored) {
        }

        SysConfigEntity entity = sysConfigDao.findByKey(STORAGE_CONFIG_KEY);
        if (entity != null && entity.getConfigValue() != null && !entity.getConfigValue().isBlank()) {
            StorageConfig cfg = JSON.parseObject(entity.getConfigValue(), StorageConfig.class);
            if (cfg != null) return cfg;
        }

        StorageConfig running = routingFileStorageService.getCurrentConfig();
        return running != null ? running : StorageConfig.builder().type("local").build();
    }

    private StorageConfig mergeStorageConfig(StorageConfig base, com.edumind.system.dto.config.StorageConfigDTO dto) {
        StorageConfig config = StorageConfig.builder()
                .type(dto.getType() != null ? dto.getType() : base.getType())
                .localPath(org.springframework.util.StringUtils.hasText(dto.getLocalPath()) ? dto.getLocalPath() : base.getLocalPath())
                .minioEndpoint(org.springframework.util.StringUtils.hasText(dto.getMinioEndpoint()) ? dto.getMinioEndpoint() : base.getMinioEndpoint())
                .minioAccessKey(org.springframework.util.StringUtils.hasText(dto.getMinioAccessKey()) ? dto.getMinioAccessKey() : base.getMinioAccessKey())
                .minioSecretKey(org.springframework.util.StringUtils.hasText(dto.getMinioSecretKey()) ? dto.getMinioSecretKey() : base.getMinioSecretKey())
                .minioBucket(org.springframework.util.StringUtils.hasText(dto.getMinioBucket()) ? dto.getMinioBucket() : base.getMinioBucket())
                .cosSecretId(org.springframework.util.StringUtils.hasText(dto.getCosSecretId()) ? dto.getCosSecretId() : base.getCosSecretId())
                .cosSecretKey(org.springframework.util.StringUtils.hasText(dto.getCosSecretKey()) ? dto.getCosSecretKey() : base.getCosSecretKey())
                .cosRegion(org.springframework.util.StringUtils.hasText(dto.getCosRegion()) ? dto.getCosRegion() : base.getCosRegion())
                .cosBucket(org.springframework.util.StringUtils.hasText(dto.getCosBucket()) ? dto.getCosBucket() : base.getCosBucket())
                .cosDomain(org.springframework.util.StringUtils.hasText(dto.getCosDomain()) ? dto.getCosDomain() : base.getCosDomain())
                .ossEndpoint(org.springframework.util.StringUtils.hasText(dto.getOssEndpoint()) ? dto.getOssEndpoint() : base.getOssEndpoint())
                .ossAccessKeyId(org.springframework.util.StringUtils.hasText(dto.getOssAccessKeyId()) ? dto.getOssAccessKeyId() : base.getOssAccessKeyId())
                .ossAccessKeySecret(org.springframework.util.StringUtils.hasText(dto.getOssAccessKeySecret()) ? dto.getOssAccessKeySecret() : base.getOssAccessKeySecret())
                .ossBucket(org.springframework.util.StringUtils.hasText(dto.getOssBucket()) ? dto.getOssBucket() : base.getOssBucket())
                .ossDomain(org.springframework.util.StringUtils.hasText(dto.getOssDomain()) ? dto.getOssDomain() : base.getOssDomain())
                .build();
        return config;
    }

    private String getActiveBucketOrPath(StorageConfig cfg, String type) {
        if ("minio".equalsIgnoreCase(type)) return cfg.getMinioBucket();
        if ("cos".equalsIgnoreCase(type)) return cfg.getCosBucket();
        if ("oss".equalsIgnoreCase(type)) return cfg.getOssBucket();
        return org.springframework.util.StringUtils.hasText(cfg.getLocalPath()) ? cfg.getLocalPath() : "backend/data/edumind";
    }

    @Override
    public MailConfig getMailConfig() {
        String cacheKey = RedisKeyBuilder.sysConfig(MAIL_CONFIG_KEY);
        try {
            String cachedJson = redisService.get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                return JSON.parseObject(cachedJson, MailConfig.class);
            }
        } catch (Exception ex) {
            log.warn("读取邮件配置 Redis 缓存异常: {}", ex.getMessage());
        }

        SysConfigEntity entity = sysConfigDao.findByKey(MAIL_CONFIG_KEY);
        if (entity == null || entity.getConfigValue() == null || entity.getConfigValue().isBlank()) {
            // 返回默认骨架配置
            return MailConfig.builder().build();
        }

        MailConfig config = JSON.parseObject(entity.getConfigValue(), MailConfig.class);
        if (config == null) {
            config = MailConfig.builder().build();
        }

        try {
            redisService.set(cacheKey, JSON.toJSONString(config), 3600);
        } catch (Exception ignored) {
        }
        return config;
    }

    @Override
    public MailConfigVO getMailConfigVO() {
        return sysConfigConverter.toMailVO(getMailConfig());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMailConfig(MailConfigDTO dto) {
        MailConfig existing = getMailConfig();
        sysConfigConverter.applyMailUpdate(existing, dto);

        String jsonValue = JSON.toJSONString(existing);
        SysConfigEntity entity = sysConfigDao.findByKey(MAIL_CONFIG_KEY);
        if (entity == null) {
            entity = SysConfigEntity.builder()
                    .configKey(MAIL_CONFIG_KEY)
                    .configName("邮件发送服务配置(SMTP)")
                    .configGroup("mail")
                    .configValue(jsonValue)
                    .remark("SMTP发信参数、发信人名称与验证码防刷策略")
                    .build();
            sysConfigDao.insert(entity);
        } else {
            entity.setConfigValue(jsonValue);
            sysConfigDao.updateById(entity);
        }

        // 清理缓存以即时生效
        String cacheKey = RedisKeyBuilder.sysConfig(MAIL_CONFIG_KEY);
        try {
            redisService.delete(cacheKey);
        } catch (Exception ignored) {
        }
        log.info("系统邮件 SMTP 发件配置已成功更新");
    }

    @Override
    public void testMail(MailTestDTO dto) {
        MailConfig config = getMailConfig();
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            throw new BusinessException("系统尚未配置 SMTP 发信邮箱，请先在下方完成配置并保存");
        }
        try {
            mailClient.testSend(config, dto.getToEmail().trim());
            com.edumind.system.entity.SysEmailLogEntity logEntity = com.edumind.system.entity.SysEmailLogEntity.builder()
                    .email(dto.getToEmail().trim())
                    .subject("EduMind 智教云平台配置连通性测试")
                    .content("SMTP 邮件发信服务验证通过")
                    .scene("TEST_EMAIL")
                    .provider(config.getHost())
                    .status(1)
                    .resultMsg("发信成功")
                    .createTime(java.time.LocalDateTime.now())
                    .build();
            sysEmailLogDao.insert(logEntity);
        } catch (Exception ex) {
            com.edumind.system.entity.SysEmailLogEntity logEntity = com.edumind.system.entity.SysEmailLogEntity.builder()
                    .email(dto.getToEmail().trim())
                    .subject("EduMind 智教云平台配置连通性测试")
                    .content("SMTP 邮件发信服务验证失败")
                    .scene("TEST_EMAIL")
                    .provider(config.getHost())
                    .status(2)
                    .resultMsg(ex.getMessage() != null ? ex.getMessage() : "发送异常")
                    .createTime(java.time.LocalDateTime.now())
                    .build();
            sysEmailLogDao.insert(logEntity);
            throw ex;
        }
    }

    @Override
    public String getConfigValue(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String cacheKey = RedisKeyBuilder.sysConfig(key);
        try {
            String cached = redisService.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ignored) {
        }

        SysConfigEntity entity = sysConfigDao.findByKey(key);
        if (entity == null) {
            return null;
        }
        try {
            redisService.set(cacheKey, entity.getConfigValue(), 3600);
        } catch (Exception ignored) {
        }
        return entity.getConfigValue();
    }

    private static final java.util.List<String> STANDARD_GROUPS = java.util.List.of(
            "site", "session", "file", "rateLimit", "login", "register",
            "thirdParty", "payment", "sms", "email", "security", "ai"
    );

    private String resolveConfigKey(String groupCode) {
        if (groupCode == null) return "sys.misc.config";
        return switch (groupCode.trim()) {
            case "site" -> "sys.site.config";
            case "session" -> "sys.session.config";
            case "file", "storage" -> "sys.storage.policy";
            case "rateLimit" -> "sys.rateLimit.config";
            case "login" -> "sys.login.config";
            case "register" -> "sys.register.config";
            case "thirdParty" -> "sys.thirdParty.config";
            case "payment" -> "sys.payment.config";
            case "sms" -> "sys.sms.config";
            case "mail", "email" -> "sys.mail.config";
            case "security" -> "sys.security.config";
            case "ai" -> "sys.ai.config";
            default -> "sys." + groupCode.trim() + ".config";
        };
    }

    private String getGroupTitle(String groupCode) {
        if (groupCode == null) return "通用配置";
        return switch (groupCode.trim()) {
            case "site" -> "平台基础与品牌信息";
            case "session" -> "会话令牌与安全凭据";
            case "file", "storage" -> "文件存储与上传限制";
            case "rateLimit" -> "全站敏感接口防刷限流矩阵";
            case "login" -> "用户登录认证与防爆破策略";
            case "register" -> "新用户注册准入与角色分配";
            case "thirdParty" -> "第三方 OAuth 授权登录";
            case "payment" -> "支付服务网关参数";
            case "sms" -> "短信发信服务与模板映射";
            case "mail", "email" -> "邮件发送服务配置(SMTP)";
            case "security" -> "全链路安全防护控制配置";
            case "ai" -> "AI 助手全局与 Token 差异化配额";
            default -> groupCode;
        };
    }

    private String getDefaultConfigJson(String groupCode) {
        if (groupCode == null) return "{}";
        return switch (groupCode.trim()) {
            case "site" -> "{\"platformName\":\"智教云 · EduMind\",\"platformSubtitle\":\"AI 智能教学赋能平台\",\"loginWelcome\":\"欢迎登录智教云平台\",\"registerTitle\":\"开启智教未来之旅\",\"copyright\":\"Copyright © 2026 EduMind. All rights reserved.\",\"icpEnabled\":true,\"icpNumber\":\"京ICP备20260001号-1\",\"icpUrl\":\"https://beian.miit.gov.cn\"}";
            case "session" -> "{\"tokenExpireHours\":24,\"sessionSignExpireHours\":24}";
            case "file", "storage" -> "{\"maxSizeMb\":50,\"allowedExtensions\":\"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov\"}";
            case "rateLimit" -> "{\"captchaPerIpMinute\":40,\"loginPerIpMinute\":30,\"registerPerIpMinute\":10,\"smsPerIpMinute\":5,\"smsSendIntervalSeconds\":60,\"smsPerPhoneDaily\":10,\"smsPerIpDaily\":30,\"aiChatPerUserMinute\":8}";
            case "login" -> "{\"captchaEnabled\":true,\"captchaType\":\"image\",\"smsLoginEnabled\":false,\"smsLoginSliderCaptchaEnabled\":false,\"emailLoginEnabled\":true,\"emailLoginSliderCaptchaEnabled\":false,\"rememberMe\":true,\"maxRetryCount\":5,\"maxRetryCountIp\":20,\"lockTime\":10}";
            case "register" -> "{\"enabled\":true,\"captchaEnabled\":true,\"captchaType\":\"image\",\"defaultRoleCode\":\"STUDENT\",\"needAudit\":false,\"minPasswordLength\":6,\"auditorUserIds\":[]}";
            case "thirdParty" -> "{\"wechat\":{\"enabled\":false,\"appId\":\"\",\"appSecret\":\"\"},\"alipay\":{\"enabled\":false,\"appId\":\"\",\"privateKey\":\"\",\"publicKey\":\"\"},\"github\":{\"enabled\":false,\"clientId\":\"\",\"clientSecret\":\"\"},\"google\":{\"enabled\":false,\"clientId\":\"\",\"clientSecret\":\"\",\"redirectUri\":\"\"}}";
            case "payment" -> "{\"wechatPay\":{\"enabled\":false,\"mchId\":\"\",\"appId\":\"\",\"apiV3Key\":\"\",\"privateKey\":\"\",\"certSerialNo\":\"\",\"notifyUrl\":\"\"},\"alipay\":{\"enabled\":false,\"appId\":\"\",\"privateKey\":\"\",\"publicKey\":\"\",\"signType\":\"RSA2\",\"gatewayUrl\":\"https://openapi.alipay.com/gateway.do\",\"notifyUrl\":\"\",\"returnUrl\":\"\"}}";
            case "sms" -> "{\"enabled\":false,\"provider\":\"aliyunAuth\",\"accessKeyId\":\"\",\"accessKeySecret\":\"\",\"signName\":\"智教云\",\"tencentAppId\":\"\",\"templateVerifyCode\":\"100001\",\"templateModifyPhone\":\"100002\",\"templateResetPassword\":\"100003\",\"templateBindPhone\":\"100004\",\"templateVerifyBindPhone\":\"100005\",\"schemeName\":\"\",\"codeExpireMinutes\":5}";
            case "mail", "email" -> "{\"enabled\":true,\"provider\":\"qq\",\"host\":\"smtp.qq.com\",\"port\":465,\"username\":\"service@edumind.com\",\"password\":\"\",\"fromName\":\"智教云平台团队\",\"authEnabled\":true,\"securityType\":\"SSL\",\"connectionTimeoutMs\":5000,\"timeoutMs\":5000,\"writeTimeoutMs\":5000,\"encoding\":\"UTF-8\",\"debug\":false,\"codeExpireMinutes\":5,\"codeLength\":6,\"dailyLimitPerEmail\":20,\"sendIntervalSeconds\":60}";
            case "security" -> "{\"disableDevtool\":false,\"isConcurrent\":false,\"timestampEnabled\":true,\"timestampWindowMs\":300000,\"nonceEnabled\":true,\"sm3SignEnabled\":false,\"sm4EncryptEnabled\":false}";
            case "ai" -> "{\"assistantEnabled\":true,\"globalKnowledge\":\"\",\"answerScope\":\"focus\",\"tokensPerUserDaily\":100000,\"roleTokenQuotas\":[]}";
            default -> "{}";
        };
    }

    @Override
    public java.util.List<com.edumind.system.vo.config.SysConfigGroupVO> listAllGroups() {
        java.util.List<com.edumind.system.vo.config.SysConfigGroupVO> result = new java.util.ArrayList<>();
        for (String groupCode : STANDARD_GROUPS) {
            result.add(getByGroupCode(groupCode));
        }
        return result;
    }

    @Override
    public com.edumind.system.vo.config.SysConfigGroupVO getByGroupCode(String groupCode) {
        String code = "email".equals(groupCode) ? "email" : groupCode;
        String key = resolveConfigKey(code);
        String cached = null;
        try {
            cached = redisService.get(RedisKeyBuilder.sysConfig(key));
        } catch (Exception ignored) {
        }

        if (cached != null && !cached.isBlank()) {
            return com.edumind.system.vo.config.SysConfigGroupVO.builder()
                    .groupCode(code)
                    .groupName(getGroupTitle(code))
                    .configValue(cached)
                    .remark(getGroupTitle(code))
                    .build();
        }

        SysConfigEntity entity = sysConfigDao.findByKey(key);
        if (entity == null || entity.getConfigValue() == null || entity.getConfigValue().isBlank()) {
            String defaultJson = getDefaultConfigJson(code);
            entity = SysConfigEntity.builder()
                    .configKey(key)
                    .configGroup(code)
                    .configName(getGroupTitle(code))
                    .configValue(defaultJson)
                    .remark(getGroupTitle(code))
                    .build();
            sysConfigDao.insert(entity);
            try {
                redisService.set(RedisKeyBuilder.sysConfig(key), defaultJson, 86400);
            } catch (Exception ignored) {
            }
        }
        return com.edumind.system.vo.config.SysConfigGroupVO.builder()
                .groupCode(code)
                .groupName(entity.getConfigName() != null ? entity.getConfigName() : getGroupTitle(code))
                .configValue(entity.getConfigValue())
                .remark(entity.getRemark())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigGroup(String groupCode, String configValueJson) {
        if (configValueJson == null || configValueJson.isBlank()) {
            throw new BusinessException("配置内容不能为空");
        }
        try {
            JSON.parseObject(configValueJson);
        } catch (Exception e) {
            throw new BusinessException("配置内容格式不正确，必须为有效 JSON");
        }

        String code = "email".equals(groupCode) ? "email" : groupCode;
        String key = resolveConfigKey(code);
        SysConfigEntity entity = sysConfigDao.findByKey(key);
        if (entity == null) {
            entity = SysConfigEntity.builder()
                    .configKey(key)
                    .configGroup(code)
                    .configName(getGroupTitle(code))
                    .configValue(configValueJson)
                    .remark(getGroupTitle(code))
                    .build();
            sysConfigDao.insert(entity);
        } else {
            entity.setConfigValue(configValueJson);
            sysConfigDao.updateById(entity);
        }

        // 刷新 Redis 缓存
        try {
            redisService.set(RedisKeyBuilder.sysConfig(key), configValueJson, 86400 * 30);
        } catch (Exception ignored) {
        }

        // 若是安全配置，联动热更新
        if ("security".equals(code)) {
            try {
                Map<String, Object> map = JSON.parseObject(configValueJson, new TypeReference<Map<String, Object>>() {});
                if (map != null) {
                    dynamicSecurityConfigService.updateConfig(map);
                }
            } catch (Exception e) {
                log.warn("动态同步安全配置失败: {}", e.getMessage());
            }
        }

        log.info("系统配置分组已成功保存: groupCode={}, key={}", code, key);
    }

    @Override
    public boolean testSms(com.edumind.system.dto.config.TestSmsDTO dto) {
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        log.info("【EduMind短信测试】向手机号 [{}] 发送验证码短信: [{}]", dto.getPhone(), code);

        com.edumind.system.entity.SysSmsLogEntity logEntity = com.edumind.system.entity.SysSmsLogEntity.builder()
                .phone(dto.getPhone().trim())
                .content(code)
                .smsType("TEST_VERIFY")
                .templateId(dto.getTemplateCode() != null && !dto.getTemplateCode().isBlank() ? dto.getTemplateCode() : "100001")
                .provider("aliyunAuth")
                .status(1)
                .resultMsg("短信测试发送成功（验证码：" + code + "）")
                .bizId("TEST_" + System.currentTimeMillis())
                .sendTime(java.time.LocalDateTime.now())
                .createTime(java.time.LocalDateTime.now())
                .build();
        sysSmsLogDao.insert(logEntity);
        return true;
    }

    @Override
    public java.util.List<com.edumind.system.vo.config.SmsLogVO> getRecentSmsLogs(int limit) {
        return sysSmsLogDao.listRecent(limit).stream()
                .map(sysConfigConverter::toSmsLogVO)
                .toList();
    }

    @Override
    public com.edumind.common.api.PageResult<com.edumind.system.vo.config.SmsLogVO> pageSmsLogs(int page, int size, String phone, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.edumind.system.entity.SysSmsLogEntity> p =
                sysSmsLogDao.page(page, size, phone, status);
        java.util.List<com.edumind.system.vo.config.SmsLogVO> list = p.getRecords().stream()
                .map(sysConfigConverter::toSmsLogVO)
                .toList();
        return new com.edumind.common.api.PageResult<>(p.getTotal(), p.getCurrent(), p.getCurrent(), p.getSize(), list);
    }

    @Override
    public java.util.Map<String, String> testPayment(com.edumind.system.dto.config.TestPaymentDTO dto) {
        String orderNo = "TEST_PAY_" + System.currentTimeMillis();
        java.util.Map<String, String> res = new java.util.HashMap<>();
        res.put("orderNo", orderNo);
        res.put("type", dto.getType());
        res.put("amount", "0.01");
        res.put("status", "PAID");
        res.put("qrcode", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://edumind.com/mockpay/" + orderNo);
        return res;
    }

    @Override
    public java.util.List<com.edumind.system.vo.config.EmailLogVO> getRecentEmailLogs(int limit) {
        return sysEmailLogDao.listRecent(limit).stream()
                .map(sysConfigConverter::toEmailLogVO)
                .toList();
    }

    @Override
    public com.edumind.common.api.PageResult<com.edumind.system.vo.config.EmailLogVO> pageEmailLogs(int page, int size, String email, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.edumind.system.entity.SysEmailLogEntity> p =
                sysEmailLogDao.page(page, size, email, status);
        java.util.List<com.edumind.system.vo.config.EmailLogVO> list = p.getRecords().stream()
                .map(sysConfigConverter::toEmailLogVO)
                .toList();
        return new com.edumind.common.api.PageResult<>(p.getTotal(), p.getCurrent(), p.getCurrent(), p.getSize(), list);
    }

    @Override
    public java.util.List<com.edumind.system.vo.config.RoleOptionVO> getRoleOptions() {
        return roleDao.findAll().stream()
                .map(r -> new com.edumind.system.vo.config.RoleOptionVO(r.getId(), r.getRoleName(), r.getRoleCode()))
                .toList();
    }

    @Override
    public java.util.List<com.edumind.system.vo.config.UserOptionVO> getUserOptions() {
        return userDao.listAllActive().stream()
                .map(u -> new com.edumind.system.vo.config.UserOptionVO(
                        u.getId(),
                        (u.getRealName() != null && !u.getRealName().isBlank() ? u.getRealName() : u.getUsername()) + " (" + u.getUsername() + ")"
                ))
                .toList();
    }
}
