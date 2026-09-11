package com.edumind.security.ratelimit;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.infrastructure.redis.RateLimitService;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(-90)
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final RateLimitService rateLimitService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        for (RateLimitProperties.Rule rule : rateLimitProperties.getRules()) {
            if (!matches(rule, request)) {
                continue;
            }
            String key = buildKey(rule, request);
            if (!rateLimitService.allow(key, rule.getLimit(), rule.getWindowSeconds())) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(RateLimitProperties.Rule rule, HttpServletRequest request) {
        if (!"*".equalsIgnoreCase(rule.getMethod())
                && !rule.getMethod().equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return pathMatcher.match(rule.getPath(), request.getRequestURI());
    }

    private String buildKey(RateLimitProperties.Rule rule, HttpServletRequest request) {
        if ("user".equalsIgnoreCase(rule.getDimension()) && StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            if (rule.getPath().contains("/ai/chat/stream")) {
                return RedisKeyBuilder.rateAiChat(userId);
            }
            if (rule.getPath().contains("/ai/questions/generate")) {
                return RedisKeyBuilder.rateAiQuestion(userId);
            }
            if (rule.getPath().contains("/ai/exams/generate")) {
                return RedisKeyBuilder.rateAiExam(userId);
            }
            return "edumind:rate:user:" + userId + ":" + rule.getPath();
        }
        String ip = resolveClientIp(request);
        if (rule.getPath().contains("/auth/login")) {
            return RedisKeyBuilder.rateLogin(ip);
        }
        if (rule.getPath().contains("/auth/register")) {
            return RedisKeyBuilder.rateRegister(ip);
        }
        if (rule.getPath().contains("/auth/captcha")) {
            return RedisKeyBuilder.rateCaptcha(ip);
        }
        return "edumind:rate:ip:" + ip + ":" + rule.getPath();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
