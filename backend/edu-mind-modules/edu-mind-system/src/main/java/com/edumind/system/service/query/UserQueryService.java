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

    /**
     * 按「当前租户上下文」解析用户有效角色（平台级 + 当前租户）。
     * 无租户上下文时仅返回平台级角色，切租户后结果随租户变化。
     */
    List<String> getRolesByUserId(Long userId);

    /**
     * 按「显式指定租户」解析用户有效角色（平台级 + 指定租户）。
     * 用于跨租户判定场景（如以入参 tenantId 解析数据范围），避免依赖线程上下文造成误判。
     */
    List<String> getRoleCodesByUserIdAndTenant(Long userId, Long tenantId);

    /** 按「当前租户上下文」解析用户有效权限编码 */
    List<String> getPermissionsByUserId(Long userId);

    /**
     * 查询用户加入的全部租户 ID（跨租户，不含租户过滤）。
     * 用于权限缓存失效等必须覆盖用户所有租户的场景。
     */
    List<Long> listTenantIdsByUserId(Long userId);

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
