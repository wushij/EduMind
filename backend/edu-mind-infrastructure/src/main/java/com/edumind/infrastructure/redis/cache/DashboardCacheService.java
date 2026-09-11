package com.edumind.infrastructure.redis.cache;

import com.edumind.common.constant.RedisConstant;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final RedisService redisService;

    public <T> T getOrLoad(Long userId, Class<T> type, Supplier<T> loader) {
        if (userId == null) {
            return loader.get();
        }
        String key = RedisKeyBuilder.dashboard(userId);
        T cached = redisService.getObject(key, type);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        if (value != null) {
            redisService.setObject(key, value, RedisConstant.DASHBOARD_TTL_SECONDS);
        }
        return value;
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        redisService.delete(RedisKeyBuilder.dashboard(userId));
    }
}
