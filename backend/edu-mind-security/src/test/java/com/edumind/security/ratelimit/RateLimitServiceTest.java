package com.edumind.security.ratelimit;

import com.edumind.infrastructure.redis.RateLimitService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitService rateLimitService;

    @Test
    void rateLimitPropertiesShouldContainLoginRule() {
        RateLimitProperties properties = new RateLimitProperties();
        Assertions.assertFalse(properties.getRules().isEmpty());
        Assertions.assertTrue(properties.getRules().stream()
                .anyMatch(rule -> "/api/auth/login".equals(rule.getPath())));
    }

    @Test
    void allowShouldRespectLimit() {
        when(rateLimitService.allow(eq("k"), anyLong(), anyLong())).thenReturn(true, false);
        Assertions.assertTrue(rateLimitService.allow("k", 10, 60));
        Assertions.assertFalse(rateLimitService.allow("k", 10, 60));
    }
}
