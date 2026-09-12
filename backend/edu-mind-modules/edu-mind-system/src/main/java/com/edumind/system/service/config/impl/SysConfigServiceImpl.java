package com.edumind.system.service.config.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.mail.MailClient;
import com.edumind.infrastructure.mail.MailConfig;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    public static final String MAIL_CONFIG_KEY = "sys.mail.config";

    private final SysConfigDao sysConfigDao;
    private final SysConfigConverter sysConfigConverter;
    private final MailClient mailClient;
    private final RedisService redisService;

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
