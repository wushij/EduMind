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
}
