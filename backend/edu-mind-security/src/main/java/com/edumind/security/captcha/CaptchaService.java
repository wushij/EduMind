package com.edumind.security.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.edumind.common.constant.RedisConstant;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.RedisSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private record LocalCacheEntry(String value, long expireAt) {
    }

    private final RedisService redisService;
    private final RedisSupport redisSupport;
    private final Map<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    public CaptchaVO create() {
        String id = IdUtil.simpleUUID();
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 50);
        lineCaptcha.setBackground(Color.WHITE);
        String code = lineCaptcha.getCode();
        saveCode(id, code.toLowerCase());
        return CaptchaVO.builder()
                .id(id)
                .img(lineCaptcha.getImageBase64())
                .build();
    }

    public void verify(String id, String answer) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(answer)) {
            throw new BusinessException("请输入验证码");
        }
        String expected = getAndRemoveCode(id);
        if (expected == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }
        if (!expected.trim().equalsIgnoreCase(answer.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    private void saveCode(String id, String code) {
        String key = RedisKeyBuilder.captcha(id);
        if (redisSupport.useRedisOrFallback()) {
            try {
                redisService.set(key, code, RedisConstant.CAPTCHA_TTL_SECONDS);
                return;
            } catch (Exception ex) {
                if (redisSupport.requireRedis()) {
                    throw new BusinessException("验证码服务不可用");
                }
                log.warn("Redis 写入验证码异常，已切换为本地缓存: {}", ex.getMessage());
            }
        }
        localCache.put(key, new LocalCacheEntry(code, System.currentTimeMillis() + RedisConstant.CAPTCHA_TTL_SECONDS * 1000));
    }

    private String getAndRemoveCode(String id) {
        String key = RedisKeyBuilder.captcha(id);
        if (redisSupport.useRedisOrFallback()) {
            try {
                String val = redisService.get(key);
                redisService.delete(key);
                if (val != null) {
                    return val;
                }
            } catch (Exception ex) {
                if (redisSupport.requireRedis()) {
                    throw new BusinessException("验证码服务不可用");
                }
                log.warn("Redis 获取验证码异常，尝试从本地缓存读取: {}", ex.getMessage());
            }
        }
        LocalCacheEntry entry = localCache.remove(key);
        if (entry != null && System.currentTimeMillis() <= entry.expireAt()) {
            return entry.value();
        }
        return null;
    }
}
