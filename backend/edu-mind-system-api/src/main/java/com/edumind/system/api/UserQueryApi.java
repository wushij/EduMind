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
}
