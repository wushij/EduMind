package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.system.api.TenantDataScopeApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.entity.UserRoleEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 租户内 RBAC 收敛集成测试（V2.6.4）
 *
 * <p>修复前：{@code sys_user_role} 无租户维度，角色全局生效，导致</p>
 * <ul>
 *   <li>切换租户后权限集完全不变（菜单/权限不随租户变化）；</li>
 *   <li>A 校的 TENANT_ADMIN 切到 B 校后仍被判定为校级管理员，
 *       从而拿到 B 校 allTenant 数据范围与配额管理权（跨租户越权）。</li>
 * </ul>
 *
 * <p>本用例断言修复后的「租户内角色」语义：租户级角色只在授权租户内生效，
 * 平台级角色（ADMIN/PLATFORM_ADMIN/ROLE_ADMIN，tenant_id=0）对所有租户生效。</p>
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantScopedRbacIntegrationTest {

    private static final Long TENANT_A = 990001L;
    private static final Long TENANT_B = 990002L;

    @Autowired
    private UserQueryApi userQueryApi;

    @Autowired
    private TenantDataScopeApi tenantDataScopeApi;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private RoleDao roleDao;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        TenantContext.runWithoutTenant(() -> {
            UserEntity user = new UserEntity();
            user.setUsername("rbac-iso-" + System.nanoTime());
            // 测试用户仅用于角色隔离校验，密码为占位值，不参与登录
            user.setPassword("$2b$10$testplaceholder");
            user.setRealName("RBAC租户隔离测试用户");
            user.setStatus("ENABLE");
            userDao.insert(user);
            testUserId = user.getId();
        });
    }

    @AfterEach
    void tearDown() {
        if (testUserId != null) {
            Long userId = testUserId;
            TenantContext.runWithoutTenant(() -> {
                userRoleDao.deleteByUserId(userId);
                userDao.deleteById(userId);
            });
        }
        TenantContext.clear();
    }

    @Test
    @DisplayName("租户级角色(TENANT_ADMIN)仅在授权租户内生效，切换租户后不得获得全域数据范围")
    void tenantAdminRoleMustBeScopedToItsTenant() {
        RoleEntity tenantAdmin = roleDao.findByRoleCode("TENANT_ADMIN");
        Assertions.assertNotNull(tenantAdmin, "种子角色 TENANT_ADMIN 必须存在（见 R__seed_legacy.sql）");
        assignRole(tenantAdmin.getId(), TENANT_A);

        // 1. 在授权租户 A 内：角色可见，且获得租户全域数据范围与配额管理权
        TenantContext.setTenantId(TENANT_A);
        Assertions.assertTrue(userQueryApi.getRolesByUserId(testUserId).contains("TENANT_ADMIN"),
                "在授权租户内必须解析到 TENANT_ADMIN");
        Assertions.assertTrue(tenantDataScopeApi.resolve(testUserId, TENANT_A).isAllTenant(),
                "在授权租户内应获得租户全域数据范围");
        Assertions.assertTrue(tenantDataScopeApi.resolve(testUserId, TENANT_A).isCanManageQuota(),
                "在授权租户内应具备配额管理权");

        // 2. 切换到未授权租户 B：角色不可见，且不得获得全域数据范围（修复前此处为 true，即跨租户越权）
        TenantContext.setTenantId(TENANT_B);
        Assertions.assertFalse(userQueryApi.getRolesByUserId(testUserId).contains("TENANT_ADMIN"),
                "切到未授权租户后不得再持有该租户的 TENANT_ADMIN 角色");
        Assertions.assertFalse(tenantDataScopeApi.resolve(testUserId, TENANT_B).isAllTenant(),
                "未授权租户内绝不可获得全域数据范围");
        Assertions.assertFalse(tenantDataScopeApi.resolve(testUserId, TENANT_B).isCanManageQuota(),
                "未授权租户内绝不可获得配额管理权");
    }

    @Test
    @DisplayName("平台级角色(ADMIN, tenant_id=0)对所有租户生效，保证平台治理能力不被租户切分")
    void platformAdminRoleMustApplyToAllTenants() {
        RoleEntity admin = roleDao.findByRoleCode("ADMIN");
        Assertions.assertNotNull(admin, "种子角色 ADMIN 必须存在");
        assignRole(admin.getId(), UserRoleEntity.PLATFORM_TENANT_ID);

        TenantContext.setTenantId(TENANT_A);
        Assertions.assertTrue(userQueryApi.getRolesByUserId(testUserId).contains("ADMIN"),
                "平台级角色在租户 A 内必须可见");
        Assertions.assertTrue(tenantDataScopeApi.resolve(testUserId, TENANT_A).isAllTenant());

        TenantContext.setTenantId(TENANT_B);
        Assertions.assertTrue(userQueryApi.getRolesByUserId(testUserId).contains("ADMIN"),
                "平台级角色在租户 B 内同样必须可见");
        Assertions.assertTrue(tenantDataScopeApi.resolve(testUserId, TENANT_B).isAllTenant());
    }

    @Test
    @DisplayName("无租户上下文时 Fail-Closed：仅返回平台级角色，绝不返回任意租户的授权")
    void missingTenantContextMustOnlyExposePlatformRoles() {
        RoleEntity tenantAdmin = roleDao.findByRoleCode("TENANT_ADMIN");
        RoleEntity admin = roleDao.findByRoleCode("ADMIN");
        Assertions.assertNotNull(tenantAdmin);
        Assertions.assertNotNull(admin);
        assignRole(tenantAdmin.getId(), TENANT_A);
        assignRole(admin.getId(), UserRoleEntity.PLATFORM_TENANT_ID);

        TenantContext.clear();
        java.util.List<String> roles = userQueryApi.getRolesByUserId(testUserId);
        Assertions.assertTrue(roles.contains("ADMIN"), "无租户上下文时平台级角色仍应可见");
        Assertions.assertFalse(roles.contains("TENANT_ADMIN"),
                "无租户上下文时不得暴露任何租户内的角色授权");
    }

    private void assignRole(Long roleId, Long tenantId) {
        TenantContext.runWithoutTenant(() -> {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(testUserId);
            relation.setRoleId(roleId);
            relation.setTenantId(tenantId);
            userRoleDao.insert(relation);
        });
    }
}
