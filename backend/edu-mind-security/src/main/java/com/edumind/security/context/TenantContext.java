package com.edumind.security.context;

/**
 * 租户上下文代理类 (向后兼容层，实际委托给 com.edumind.common.context.TenantContext)
 */
public final class TenantContext {

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        com.edumind.common.context.TenantContext.setTenantId(tenantId);
    }

    public static Long getTenantId() {
        return com.edumind.common.context.TenantContext.getTenantId();
    }

    public static Long requireTenantId() {
        return com.edumind.common.context.TenantContext.requireTenantId();
    }

    public static void setDelegatedSession(boolean isDelegated) {
        com.edumind.common.context.TenantContext.setDelegatedSession(isDelegated);
    }

    public static boolean isDelegatedSession() {
        return com.edumind.common.context.TenantContext.isDelegatedSession();
    }

    public static void setIgnoreTenant(boolean ignore) {
        com.edumind.common.context.TenantContext.setIgnoreTenant(ignore);
    }

    public static boolean isIgnoreTenant() {
        return com.edumind.common.context.TenantContext.isIgnoreTenant();
    }

    public static void runWithoutTenant(Runnable runnable) {
        com.edumind.common.context.TenantContext.runWithoutTenant(runnable);
    }

    public static <T> T callWithoutTenant(java.util.function.Supplier<T> supplier) {
        return com.edumind.common.context.TenantContext.callWithoutTenant(supplier);
    }

    public static void clear() {
        com.edumind.common.context.TenantContext.clear();
    }
}
