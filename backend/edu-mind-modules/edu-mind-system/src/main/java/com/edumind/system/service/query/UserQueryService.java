package com.edumind.system.service.query;

import com.edumind.system.vo.user.UserBriefVO;

import java.util.List;

public interface UserQueryService {

    UserBriefVO getUserById(Long userId);

    List<String> getRolesByUserId(Long userId);

    List<String> getPermissionsByUserId(Long userId);

    List<Long> listUserIdsByRoleId(Long roleId);

    List<Long> listAllActiveUserIds();

    List<Long> listUserIdsByRoleCode(String roleCode);

    long countActiveUsers();

    long countUsersByRoleCode(String roleCode);

    List<Long> listActiveUserIdsByTenantId(Long tenantId);

    long countActiveUsersByTenantId(Long tenantId);

    List<Long> listUserIdsByTenantAndRole(Long tenantId, String roleCode);

    long countUsersByTenantAndRole(Long tenantId, String roleCode);

    List<Long> findUserIdsByKeyword(String keyword);
}
