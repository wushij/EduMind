package com.edumind.security.filter;

import com.edumind.common.api.ApiResponseWriter;
import com.edumind.common.api.ResultCode;
import com.edumind.security.config.DynamicSecurityConfigService;
import com.edumind.security.config.SecurityProperties;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.RedisSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-100)
@RequiredArgsConstructor
public class ReplayAttackFilter extends OncePerRequestFilter {

    public static final long TIME_WINDOW_MILLIS = 5 * 60 * 1000L;

    private final RedisService redisService;
    private final RedisSupport redisSupport;
    private final SecurityProperties securityProperties;
    private final DynamicSecurityConfigService dynamicSecurityConfigService;
    private final ConcurrentHashMap<String, Long> localNonceCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String timestampHeader = SecurityRequestSupport.resolveValue(request, "X-Timestamp");
        String nonceHeader = SecurityRequestSupport.resolveValue(request, "X-Nonce");
        boolean checkRequired = (securityProperties.isSmEnabled()
                || dynamicSecurityConfigService.isSm3SignEnabled()
                || dynamicSecurityConfigService.isTimestampEnabled()
                || dynamicSecurityConfigService.isNonceEnabled())
                && securityProperties.getSensitivePaths().stream()
                .anyMatch(request.getRequestURI()::startsWith);

        if (checkRequired && (timestampHeader == null || nonceHeader == null)) {
            ApiResponseWriter.write(response, ResultCode.FORBIDDEN, "安全防护已开启：缺少 X-Timestamp 或 X-Nonce");
            return;
        }

        if (timestampHeader != null && nonceHeader != null) {
            try {
                long clientTimestamp = Long.parseLong(timestampHeader);
                long currentTimestamp = System.currentTimeMillis();
                long skew = dynamicSecurityConfigService.getTimestampWindowMs();
                if (Math.abs(currentTimestamp - clientTimestamp) > skew) {
                    ApiResponseWriter.write(response, ResultCode.FORBIDDEN, "请求已失效：时间戳超出允许范围");
                    return;
                }

                boolean isNonceValid = validateNonce(nonceHeader, currentTimestamp);
                if (!isNonceValid) {
                    ApiResponseWriter.write(response, ResultCode.FORBIDDEN, "检测到重放攻击：Nonce 已被使用");
                    return;
                }
            } catch (NumberFormatException e) {
                ApiResponseWriter.write(response, ResultCode.VALIDATE_FAILED, "非法的 X-Timestamp 格式");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean validateNonce(String nonceHeader, long currentTimestamp) {
        if (redisSupport.useRedisOrFallback()) {
            try {
                return redisService.setIfAbsent(
                        RedisKeyBuilder.nonce(nonceHeader),
                        "1",
                        TIME_WINDOW_MILLIS / 1000);
            } catch (Exception ex) {
                if (redisSupport.requireRedis()) {
                    throw new IllegalStateException("Redis 不可用，无法完成防重放校验", ex);
                }
            }
        }
        localNonceCache.entrySet().removeIf(entry -> entry.getValue() < currentTimestamp);
        long expireAt = currentTimestamp + TIME_WINDOW_MILLIS;
        Long existing = localNonceCache.putIfAbsent(nonceHeader, expireAt);
        return existing == null || existing <= currentTimestamp;
    }
}
