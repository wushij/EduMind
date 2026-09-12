package com.edumind.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.PermissionCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final PermissionCacheService permissionCacheService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .order(Ordered.HIGHEST_PRECEDENCE)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/email-login",
                        "/api/auth/send-email-code",
                        "/api/auth/verify-reset-code",
                        "/api/auth/reset-password",
                        "/api/auth/register",
                        "/api/auth/captcha",
                        "/api/captcha/**",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**"
                );

        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    List<String> roles = Collections.emptyList();
                    List<String> permissions = Collections.emptyList();
                    try {
                        roles = permissionCacheService.getRoles(userId);
                        permissions = permissionCacheService.getPermissions(userId);
                    } catch (Exception ignored) {
                    }
                    if ((roles == null || roles.isEmpty()) && Long.valueOf(1L).equals(userId)) {
                        roles = Collections.singletonList(SecurityConstant.ROLE_ADMIN);
                    }
                    UserContext.set(LoginUser.builder()
                            .id(userId)
                            .roles(roles != null ? roles : Collections.emptyList())
                            .permissions(permissions != null ? permissions : Collections.emptyList())
                            .build());
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                UserContext.clear();
            }
        }).order(Ordered.HIGHEST_PRECEDENCE + 1).addPathPatterns("/**");
    }
}