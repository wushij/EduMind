package com.edumind.system.service.config.impl;

import com.alibaba.fastjson2.JSON;
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
        mailClient.testSend(config, dto.getToEmail().trim());
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
}
