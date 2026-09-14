package com.edumind.system.api;

import java.util.List;

/**
 * 用户领域跨模块只读查询公开 API
 */
public interface UserQueryApi {

    Object getUserById(Long userId);

    List<String> getRolesByUserId(Long userId);

    List<String> getPermissionsByUserId(Long userId);

    List<Long> listUserIdsByRoleId(Long roleId);

    /** 全部启用用户 ID */
    List<Long> listAllActiveUserIds();

    /** 按角色编码查询用户 ID（ADMIN/TEACHER/STUDENT） */
    List<Long> listUserIdsByRoleCode(String roleCode);

    long countActiveUsers();

    long countUsersByRoleCode(String roleCode);

    /** 按租户查询全部活跃成员用户 ID */
    List<Long> listActiveUserIdsByTenantId(Long tenantId);

    /** 按租户统计活跃成员总数 */
    long countActiveUsersByTenantId(Long tenantId);

    /** 按租户及角色编码查询成员用户 ID（ADMIN/TEACHER/STUDENT） */
    List<Long> listUserIdsByTenantAndRole(Long tenantId, String roleCode);

    /** 按租户及角色编码统计成员人数 */
    long countUsersByTenantAndRole(Long tenantId, String roleCode);
}

