package com.edumind.infrastructure.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @Test
    void setIfAbsentShouldUseTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("k"), eq("1"), any(Duration.class))).thenReturn(true);

        boolean ok = redisService.setIfAbsent("k", "1", 300);

        Assertions.assertTrue(ok);
        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).setIfAbsent(eq("k"), eq("1"), captor.capture());
        Assertions.assertEquals(300, captor.getValue().getSeconds());
    }

    @Test
    void incrementShouldSetExpireOnFirstHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("counter")).thenReturn(1L);

        long count = redisService.increment("counter", 60);

        Assertions.assertEquals(1L, count);
        verify(stringRedisTemplate).expire(eq("counter"), any(Duration.class));
    }
}
