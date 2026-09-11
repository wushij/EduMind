package com.edumind.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSupport {

    private final RedisService redisService;
    private final Environment environment;

    public boolean isDevProfile() {
        return environment.acceptsProfiles("dev");
    }

    public boolean requireRedis() {
        return !isDevProfile();
    }

    public void assertRedisAvailable() {
        if (requireRedis() && !redisService.isAvailable()) {
            throw new IllegalStateException("Redis 不可用，生产环境必须启用 Redis");
        }
    }

    public boolean useRedisOrFallback() {
        if (redisService.isAvailable()) {
            return true;
        }
        return isDevProfile();
    }
}
