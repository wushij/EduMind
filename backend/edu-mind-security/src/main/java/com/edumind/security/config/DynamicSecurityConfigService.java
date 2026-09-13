package com.edumind.security.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时动态全链路安全配置中枢 (支持系统管理后台热开关)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicSecurityConfigService {

    public static final String SECURITY_CONFIG_KEY = "sys.security.config";

    private final SecurityProperties properties;
    private final RedisService redisService;

    private volatile Map<String, Object> cachedConfig;

    @PostConstruct
    public void init() {
        this.cachedConfig = loadConfigFromRedisOrDefault();
    }

    public Map<String, Object> getConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfigFromRedisOrDefault();
        }
        return new LinkedHashMap<>(cachedConfig);
    }

    public synchronized Map<String, Object> updateConfig(Map<String, Object> req) {
        Map<String, Object> current = getConfig();
        if (req != null) {
            if (req.containsKey("timestampEnabled")) {
                current.put("timestampEnabled", Boolean.TRUE.equals(req.get("timestampEnabled")));
            }
            if (req.containsKey("nonceEnabled")) {
                current.put("nonceEnabled", Boolean.TRUE.equals(req.get("nonceEnabled")));
            }
            if (req.containsKey("sm3SignEnabled")) {
                current.put("sm3SignEnabled", Boolean.TRUE.equals(req.get("sm3SignEnabled")));
            }
            if (req.containsKey("sm4EncryptEnabled")) {
                current.put("sm4EncryptEnabled", Boolean.TRUE.equals(req.get("sm4EncryptEnabled")));
            }
            if (req.containsKey("disableDevtool")) {
                current.put("disableDevtool", Boolean.TRUE.equals(req.get("disableDevtool")));
            }
            if (req.containsKey("captchaAfterFailures")) {
                try {
                    current.put("captchaAfterFailures", Integer.parseInt(String.valueOf(req.get("captchaAfterFailures"))));
                } catch (Exception ignored) {}
            }
            if (req.containsKey("captchaOnRegister")) {
                current.put("captchaOnRegister", Boolean.TRUE.equals(req.get("captchaOnRegister")));
            }
            if (req.containsKey("timestampWindowMs")) {
                try {
                    current.put("timestampWindowMs", Long.parseLong(String.valueOf(req.get("timestampWindowMs"))));
                } catch (Exception ignored) {}
            }
        }

        this.cachedConfig = current;
        try {
            String cacheKey = RedisKeyBuilder.sysConfig(SECURITY_CONFIG_KEY);
            redisService.set(cacheKey, JSON.toJSONString(current), 86400 * 30);
            log.info("API 全链路安全配置已热应用更新: {}", current);
        } catch (Exception e) {
            log.warn("保存安全配置到 Redis 异常: {}", e.getMessage());
        }
        return current;
    }

    public boolean isTimestampEnabled() {
        return Boolean.TRUE.equals(getConfig().get("timestampEnabled"));
    }

    public long getTimestampWindowMs() {
        Object val = getConfig().get("timestampWindowMs");
        if (val instanceof Number num) return num.longValue();
        if (val != null) {
            try { return Long.parseLong(String.valueOf(val)); } catch (Exception ignored) {}
        }
        return properties.getTimestampSkewMs() > 0 ? properties.getTimestampSkewMs() : 300000L;
    }

    public boolean isNonceEnabled() {
        return Boolean.TRUE.equals(getConfig().get("nonceEnabled"));
    }

    public boolean isSm3SignEnabled() {
        return Boolean.TRUE.equals(getConfig().get("sm3SignEnabled")) || properties.isSmEnabled();
    }

    public boolean isSm4EncryptEnabled() {
        return Boolean.TRUE.equals(getConfig().get("sm4EncryptEnabled"));
    }

    public boolean isDisableDevtool() {
        return Boolean.TRUE.equals(getConfig().get("disableDevtool"));
    }

    public int getCaptchaAfterFailures() {
        Object val = getConfig().get("captchaAfterFailures");
        if (val instanceof Number num) return num.intValue();
        if (val != null) {
            try { return Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
        }
        return 3;
    }

    public boolean isCaptchaOnRegister() {
        Object val = getConfig().get("captchaOnRegister");
        return val != null ? Boolean.TRUE.equals(val) : true;
    }

    private Map<String, Object> loadConfigFromRedisOrDefault() {
        String cacheKey = RedisKeyBuilder.sysConfig(SECURITY_CONFIG_KEY);
        try {
            String json = redisService.get(cacheKey);
            if (json != null && !json.isBlank()) {
                Map<String, Object> map = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
                if (map != null) {
                    return map;
                }
            }
        } catch (Exception e) {
            log.warn("从 Redis 读取安全配置失败，回退默认值: {}", e.getMessage());
        }
        return getDefaultConfig();
    }

    private Map<String, Object> getDefaultConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestampEnabled", false);
        map.put("timestampWindowMs", properties.getTimestampSkewMs() > 0 ? properties.getTimestampSkewMs() : 300000L);
        map.put("nonceEnabled", false);
        map.put("sm3SignEnabled", properties.isSmEnabled());
        map.put("sm4EncryptEnabled", false);
        map.put("disableDevtool", false);
        map.put("captchaAfterFailures", 3);
        map.put("captchaOnRegister", true);
        return map;
    }
}
