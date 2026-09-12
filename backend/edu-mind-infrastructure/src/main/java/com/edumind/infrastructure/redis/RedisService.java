package com.edumind.infrastructure.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey("edumind:ping:probe")
                    || stringRedisTemplate.opsForValue().setIfAbsent("edumind:ping:probe", "1", Duration.ofSeconds(5)));
        } catch (Exception ex) {
            return false;
        }
    }

    public void set(String key, String value, long ttlSeconds) {
        if (ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
    }

    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(result);
    }

    public long increment(String key, long ttlSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L && ttlSeconds > 0) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        return count == null ? 0L : count;
    }

    public void expire(String key, long ttlSeconds) {
        stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    public Set<String> keys(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        return keys == null ? Collections.emptySet() : keys;
    }

    public void deleteByPattern(String pattern) {
        delete(keys(pattern));
    }

    public <T> T getObject(String key, Class<T> type) {
        String json = get(key);
        if (json == null) {
            return null;
        }
        return JSON.parseObject(json, type);
    }

    public <T> T getObject(String key, TypeReference<T> typeReference) {
        String json = get(key);
        if (json == null) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }

    public void setObject(String key, Object value, long ttlSeconds) {
        set(key, JSON.toJSONString(value), ttlSeconds);
    }

    public boolean tryAcquire(String key, long ttlSeconds) {
        return setIfAbsent(key, "1", ttlSeconds);
    }

    public void release(String key) {
        delete(key);
    }
}
