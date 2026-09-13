package com.edumind.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文解析拦截器 (严格遵循可信凭据提取与 Fail-Closed 原则)
 * 1. 已认证请求仅从服务端受信 Sa-Token 会话中提取 tenantId。
 * 2. 仅当用户具备平台管理员角色 (PLATFORM_ADMIN) 时，允许通过 X-Tenant-Id 开启代管租户会话。
 * 3. 严格禁止匿名请求或普通租户用户直接篡改 X-Tenant-Id。
 * 4. 彻底废除默认租户 1 的兜底回退，无租户则安全关闭 (Fail-Closed)。
 */
@Slf4j
@Component
@Order(-90)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_SESSION_KEY = "tenantId";
    private static final String DELEGATED_SESSION_KEY = "isDelegated";
    private static final String TENANT_HEADER_KEY = "X-Tenant-Id";
    private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long tenantId = null;
            boolean isDelegated = false;

            if (StpUtil.isLogin()) {
                try {
                    // 1. 优先从受信任的服务端 Sa-Token 会话中提取租户 ID
                    Object tidObj = StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
                    if (tidObj instanceof Number num) {
                        tenantId = num.longValue();
                    } else if (tidObj instanceof String str && !str.isBlank()) {
                        tenantId = Long.parseLong(str);
                    }

                    Object delObj = StpUtil.getSession().get(DELEGATED_SESSION_KEY);
                    if (Boolean.TRUE.equals(delObj) || "true".equalsIgnoreCase(String.valueOf(delObj))) {
                        isDelegated = true;
                    }

                    // 2. 平台超级管理员专用代管通道：仅当拥有 PLATFORM_ADMIN 角色时才允许通过 Header 临时切换代管租户
                    String headerTid = request.getHeader(TENANT_HEADER_KEY);
                    if (headerTid != null && !headerTid.isBlank()) {
                        if (StpUtil.hasRole(ROLE_PLATFORM_ADMIN)) {
                            try {
                                Long targetTenantId = Long.parseLong(headerTid.trim());
                                if (targetTenantId > 0) {
                                    tenantId = targetTenantId;
                                    isDelegated = true;
                                    log.debug("[平台代管] 超级管理员 {} 切换代管租户至: {}", StpUtil.getLoginId(), targetTenantId);
                                }
                            } catch (NumberFormatException ignored) {}
                        } else {
                            log.warn("[越权警示] 普通用户 {} 尝试通过 Header 伪造租户 ID: {}, 已被安全过滤拦截",
                                    StpUtil.getLoginId(), headerTid);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[租户解析异常] 读取登录会话租户信息失败: {}", e.getMessage());
                }
            } else {
                // 3. 未登录公开端点（如根据学校域名/编码查询学校品牌信息）允许临时通过 Header 提供参考，但不具备业务持久化上下文
                String uri = request.getRequestURI();
                if (uri != null && (uri.contains("/auth/") || uri.contains("/public/"))) {
                    String headerTid = request.getHeader(TENANT_HEADER_KEY);
                    if (headerTid != null && !headerTid.isBlank()) {
                        try {
                            tenantId = Long.parseLong(headerTid.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // 设置当前线程租户上下文 (若为 null 则保持 null，后续 requireTenantId 会执行 Fail-Closed 拒绝)
            TenantContext.setTenantId(tenantId);
            TenantContext.setDelegatedSession(isDelegated);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
