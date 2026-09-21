package com.edumind.system.service.query;

import com.edumind.system.vo.user.UserBriefVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserQueryService {

    UserBriefVO getUserById(Long userId);

    /**
     * 批量查询用户简要信息（单次 IN 查询，不含角色与权限）。
     * 用于替代循环内逐个调用 {@link #getUserById(Long)}，消除 N+1。
     */
    Map<Long, UserBriefVO> mapUserBriefsByIds(Collection<Long> userIds);

    /**
     * 批量查询用户角色编码（单次 IN 查询）。
     * 用于替代循环内逐个调用 {@link #getRolesByUserId(Long)}，消除 N+1。
     */
    Map<Long, List<String>> mapRoleCodesByUserIds(Collection<Long> userIds);

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
