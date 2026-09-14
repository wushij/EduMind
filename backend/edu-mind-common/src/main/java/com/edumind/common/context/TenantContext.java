package com.edumind.common.context;

import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;

/**
 * 智教云 · 全局租户上下文隔离容器
 * 位于 edu-mind-common 共享层，供基础设施（MyBatis-Plus、Redis、OSS）及各业务模块统一消费
 * 严格遵循 Fail-Closed 原则：当缺失租户上下文时直接拒绝，杜绝任何默认租户回退。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DELEGATED_SESSION_HOLDER = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> IGNORE_TENANT_HOLDER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {
    }

    /**
     * 设置当前可信租户 ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    /**
     * 获取当前租户 ID（可能为空）
     */
    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    /**
     * 强制要求当前请求必须具备有效租户上下文
     * 若未获取到租户，直接抛出未授权业务异常，绝不回退至默认租户
     */
    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未获取到有效租户上下文，访问受限 (Tenant Required)");
        }
        return tenantId;
    }

    /**
     * 标记当前是否处于平台超级管理员代管会话
     */
    public static void setDelegatedSession(boolean isDelegated) {
        DELEGATED_SESSION_HOLDER.set(isDelegated);
    }

    /**
     * 当前是否为代管会话
     */
    public static boolean isDelegatedSession() {
        Boolean delegated = DELEGATED_SESSION_HOLDER.get();
        return Boolean.TRUE.equals(delegated);
    }

    /**
     * 设置是否忽略租户过滤（供平台级运维/初始化任务使用）
     */
    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT_HOLDER.set(ignore);
    }

    /**
     * 判断当前是否忽略租户过滤
     */
    public static boolean isIgnoreTenant() {
        Boolean ignore = IGNORE_TENANT_HOLDER.get();
        return Boolean.TRUE.equals(ignore);
    }

    /**
     * 在忽略租户上下文的环境中执行特定代码块（执行完毕自动恢复）
     */
    public static void runWithoutTenant(Runnable runnable) {
        boolean previous = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            runnable.run();
        } finally {
            setIgnoreTenant(previous);
        }
    }

    /**
     * 在指定租户上下文中执行特定代码块（执行完毕自动恢复，专用于异步任务/事件监听器）
     */
    public static void runWithTenant(Long tenantId, Runnable runnable) {
        Long previous = getTenantId();
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previous != null) {
                setTenantId(previous);
            } else {
                TENANT_HOLDER.remove();
            }
        }
    }

    /**
     * 清理当前线程上下文
     */
    public static void clear() {
        TENANT_HOLDER.remove();
        DELEGATED_SESSION_HOLDER.remove();
        IGNORE_TENANT_HOLDER.remove();
    }
}
