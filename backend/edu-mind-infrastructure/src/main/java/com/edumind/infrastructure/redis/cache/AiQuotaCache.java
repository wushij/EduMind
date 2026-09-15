package com.edumind.infrastructure.redis.cache;

import com.edumind.common.constant.RedisConstant;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class AiQuotaCache {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final RedisService redisService;

    public boolean checkAndIncrementDailyQuota(Long userId, long dailyLimit) {
        if (userId == null) {
            return true;
        }
        String key = RedisKeyBuilder.aiQuota(userId, LocalDate.now().format(DAY_FORMAT));
        long count = redisService.increment(key, RedisConstant.AI_QUOTA_TTL_SECONDS);
        return count <= dailyLimit;
    }
}
