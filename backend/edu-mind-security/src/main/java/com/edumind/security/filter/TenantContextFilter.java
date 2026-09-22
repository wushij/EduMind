package com.edumind.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.context.TenantContext;
import com.edumind.system.api.TenantQueryApi;
import com.edumind.system.vo.tenant.TenantBriefVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户上下文解析拦截器 (严格遵循可信凭据提取与 Fail-Closed 原则)
 * <ol>
 *   <li>已认证请求仅从服务端受信 Sa-Token 会话中提取 tenantId。</li>
 *   <li>仅当用户具备<b>平台级</b>管理员角色时，允许通过 X-Tenant-Id 开启代管租户会话，
 *       且目标租户必须真实存在且处于启用状态（防止伪造不存在租户进入代管态）。</li>
 *   <li>普通租户用户、匿名请求一律不得通过 Header 篡改租户。</li>
 *   <li>彻底废除默认租户兜底回退，无租户则安全关闭 (Fail-Closed)。</li>
 * </ol>
 */
@Slf4j
@Component
@Order(-90)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_SESSION_KEY = "tenantId";
    private static final String DELEGATED_SESSION_KEY = "isDelegated";
    private static final String TENANT_HEADER_KEY = "X-Tenant-Id";

    /** 允许匿名请求携带租户参考信息的公开入口（仅用于品牌/公开课程展示，不具备持久化上下文） */
    private static final String[] PUBLIC_TENANT_PREFIXES = {"/api/auth/", "/api/public/"};

    /** 租户有效性校验缓存（避免每请求一次 DB 校验；TTL 30s，可容忍租户停用后最长 30s 的窗口） */
    private static final long TENANT_CACHE_TTL_MILLIS = 30_000L;
    private final Map<Long, CachedTenant> tenantValidityCache = new ConcurrentHashMap<>();

    private final TenantQueryApi tenantQueryApi;

    public TenantContextFilter(TenantQueryApi tenantQueryApi) {
        this.tenantQueryApi = tenantQueryApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long tenantId = null;
            boolean isDelegated = false;

            if (isLoggedIn()) {
                try {
                    // 1. 优先从受信任的服务端 Sa-Token 会话中提取租户 ID
                    tenantId = readSessionTenantId();

                    Object delObj = StpUtil.getSession().get(DELEGATED_SESSION_KEY);
                    if (Boolean.TRUE.equals(delObj) || "true".equalsIgnoreCase(String.valueOf(delObj))) {
                        isDelegated = true;
                    }

                    // 2. 平台超级管理员专用代管通道
                    String headerTid = request.getHeader(TENANT_HEADER_KEY);
                    if (headerTid != null && !headerTid.isBlank()) {
                        Long targetTenantId = parsePositiveLong(headerTid);
                        if (targetTenantId == null) {
                            log.warn("[越权警示] 非法租户标识格式，已忽略: {}", headerTid);
                        } else if (isPlatformAdmin()) {
                            // 目标租户必须真实存在且启用，否则不得进入代管态
                            if (isTenantUsable(targetTenantId)) {
                                tenantId = targetTenantId;
                                isDelegated = true;
                                log.debug("[平台代管] 超级管理员 {} 切换代管租户至: {}", StpUtil.getLoginId(), targetTenantId);
                            } else {
                                log.warn("[越权警示] 平台管理员 {} 请求代管不存在或已停用租户 {}，已拒绝",
                                        StpUtil.getLoginId(), targetTenantId);
                            }
                        } else {
                            log.warn("[越权警示] 普通用户 {} 尝试通过 Header 伪造租户 ID: {}, 已被安全过滤拦截",
                                    StpUtil.getLoginId(), headerTid);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[租户解析异常] 读取登录会话租户信息失败: {}", e.getMessage());
                }
            } else {
                // 3. 未登录公开端点（如根据学校域名/编码查询学校品牌信息）允许携带租户参考，
                //    但必须落在白名单路径内且租户真实有效，避免被用于枚举任意租户数据
                String uri = request.getRequestURI();
                String headerTid = request.getHeader(TENANT_HEADER_KEY);
                if (uri != null && headerTid != null && !headerTid.isBlank() && isPublicEndpoint(uri)) {
                    Long targetTenantId = parsePositiveLong(headerTid);
                    if (targetTenantId != null && isTenantUsable(targetTenantId)) {
                        tenantId = targetTenantId;
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

    /**
     * 登录态判定：本过滤器顺序先于 MVC 层 Sa-Token 拦截器，
     * 若 Sa-Token 上下文尚未就绪（过滤器顺序被调整）时必须容错降级为「未登录」，
     * 而不是抛出异常导致整个请求 500。
     */
    private boolean isLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            log.warn("[租户解析] Sa-Token 上下文不可用，按未登录处理: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 平台级管理员判定。
     * <p>此处刻意在「租户上下文尚未建立」时调用：角色查询只返回 tenant_id = 0 的平台级授权，
     * 因此 TENANT_ADMIN 等租户级角色不会命中本判定，杜绝用租户管理员身份开启代管通道。</p>
     */
    private boolean isPlatformAdmin() {
        try {
            return StpUtil.hasRole("PLATFORM_ADMIN")
                    || StpUtil.hasRole("ADMIN")
                    || StpUtil.hasRole("ROLE_ADMIN")
                    || Long.valueOf(1L).equals(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            log.warn("[租户解析] 平台管理员判定失败，按非管理员处理: {}", e.getMessage());
            return false;
        }
    }

    private Long readSessionTenantId() {
        Object raw = StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
        if (raw instanceof Number num) {
            return num.longValue();
        }
        if (raw instanceof String str && !str.isBlank()) {
            return parsePositiveLong(str);
        }
        return null;
    }

    private boolean isPublicEndpoint(String uri) {
        for (String prefix : PUBLIC_TENANT_PREFIXES) {
            if (uri.startsWith(prefix) || uri.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Long parsePositiveLong(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 校验租户是否存在且启用（带 30s 本地缓存）
     */
    private boolean isTenantUsable(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        CachedTenant cached = tenantValidityCache.get(tenantId);
        if (cached != null && now - cached.checkedAt < TENANT_CACHE_TTL_MILLIS) {
            return cached.usable;
        }
        boolean usable;
        try {
            TenantBriefVO tenant = tenantQueryApi.getTenantById(tenantId);
            usable = tenant != null && tenant.getStatus() != null && tenant.getStatus() == 1;
        } catch (Exception e) {
            log.warn("[租户校验] 查询租户 {} 失败，按不可用处理: {}", tenantId, e.getMessage());
            usable = false;
        }
        tenantValidityCache.put(tenantId, new CachedTenant(usable, now));
        if (tenantValidityCache.size() > 512) {
            tenantValidityCache.entrySet().removeIf(e -> now - e.getValue().checkedAt > TENANT_CACHE_TTL_MILLIS);
        }
        return usable;
    }

    private record CachedTenant(boolean usable, long checkedAt) {
    }
}
