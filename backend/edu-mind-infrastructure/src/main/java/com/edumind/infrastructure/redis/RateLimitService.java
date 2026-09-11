package com.edumind.infrastructure.redis;

import com.edumind.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisService redisService;

    public boolean allow(String key, long limit, long windowSeconds) {
        long count = redisService.increment(key, windowSeconds);
        return count <= limit;
    }

    public boolean allow(String key, long limit) {
        return allow(key, limit, RedisConstant.RATE_WINDOW_SECONDS);
    }
}
