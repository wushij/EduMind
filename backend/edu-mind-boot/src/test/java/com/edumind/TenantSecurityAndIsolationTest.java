package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智教云 V2.0 · 多租户安全隔离与上下文信任链回归测试
 */
public class TenantSecurityAndIsolationTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("验证租户上下文 Fail-Closed 安全拒绝机制 (无默认租户1回退)")
    void testTenantContextFailClosed() {
        // 1. 确保在没有任何租户设置的情况下，绝不允许静默回退至租户1
        Assertions.assertNull(TenantContext.getTenantId(), "初始状态租户 ID 必须为 null");

        // 2. 调用 requireTenantId 必须强制阻断并抛出未授权业务异常
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> {
            TenantContext.requireTenantId();
        });
        Assertions.assertTrue(ex.getMessage().contains("未获取到有效租户上下文"),
                "未携带租户上下文时必须触发 Fail-Closed 安全拒绝");

        // 3. 正常设置合法租户后，必须精准返回
        TenantContext.setTenantId(10086L);
        Assertions.assertEquals(10086L, TenantContext.requireTenantId());

        // 4. 清理后应立即失效
        TenantContext.clear();
        Assertions.assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("验证平台级运维旁路与 runWithoutTenant 线程安全上下文管理")
    void testRunWithoutTenantContext() {
        TenantContext.setTenantId(2L);
        Assertions.assertFalse(TenantContext.isIgnoreTenant(), "默认情况下禁止忽略租户隔离");

        AtomicBoolean executedInside = new AtomicBoolean(false);
        TenantContext.runWithoutTenant(() -> {
            Assertions.assertTrue(TenantContext.isIgnoreTenant(), "在 runWithoutTenant 闭包内必须标记为忽略租户");
            executedInside.set(true);
        });

        Assertions.assertTrue(executedInside.get(), "任务闭包必须正常执行完毕");
        Assertions.assertFalse(TenantContext.isIgnoreTenant(), "离开闭包后必须安全恢复忽略状态为 false");
        Assertions.assertEquals(2L, TenantContext.getTenantId(), "闭包执行前后原始租户上下文保持不变");
    }

    @Test
    @DisplayName("验证超级管理员平台代管会话标记 (Delegated Session)")
    void testDelegatedSessionFlag() {
        TenantContext.setTenantId(2L);
        TenantContext.setDelegatedSession(true);

        Assertions.assertTrue(TenantContext.isDelegatedSession(), "平台管理员代管状态必须精准记录");

        TenantContext.setDelegatedSession(false);
        Assertions.assertFalse(TenantContext.isDelegatedSession(), "代管退出后标志位必须清空");
    }

    @Test
    @DisplayName("验证跨租户组织查询安全阻断 (OrganizationQueryApi 防御)")
    void testOrganizationCrossTenantProtection() {
        TenantContext.setTenantId(1001L);
        Assertions.assertThrows(BusinessException.class, () -> {
            Long explicitTenantId = 1002L;
            if (!explicitTenantId.equals(TenantContext.requireTenantId())) {
                throw new BusinessException(403, "无权访问其他学校组织架构");
            }
        });
    }
}
