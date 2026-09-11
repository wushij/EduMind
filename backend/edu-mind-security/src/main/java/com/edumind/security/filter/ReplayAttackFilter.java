package com.edumind.security.filter;

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
    private final ConcurrentHashMap<String, Long> localNonceCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String timestampHeader = request.getHeader("X-Timestamp");
        String nonceHeader = request.getHeader("X-Nonce");

        if (timestampHeader != null && nonceHeader != null) {
            try {
                long clientTimestamp = Long.parseLong(timestampHeader);
                long currentTimestamp = System.currentTimeMillis();
                if (Math.abs(currentTimestamp - clientTimestamp) > TIME_WINDOW_MILLIS) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, "请求已失效：时间戳超出允许范围");
                    return;
                }

                boolean isNonceValid = validateNonce(nonceHeader, currentTimestamp);
                if (!isNonceValid) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, "检测到重放攻击：Nonce 已被使用");
                    return;
                }
            } catch (NumberFormatException e) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, 400, "非法的 X-Timestamp 格式");
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

    private void writeError(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\"}");
    }
}
