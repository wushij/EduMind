package com.edumind.system.api;

import com.edumind.system.vo.user.UserBriefVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户领域跨模块只读查询公开 API
 */
public interface UserQueryApi {

    UserBriefVO getUserById(Long userId);

    /**
     * 批量查询用户简要信息（单次 IN 查询，不含角色与权限）。
     * 列表/统计场景请优先使用本方法，替代循环内逐个调用 {@link #getUserById(Long)}，避免 N+1。
     */
    Map<Long, UserBriefVO> mapUserBriefsByIds(Collection<Long> userIds);

    /**
     * 批量查询用户角色编码（单次 IN 查询）。
     * 用于替代循环内逐个调用 {@link #getRolesByUserId(Long)}，避免 N+1。
     */
    Map<Long, List<String>> mapRoleCodesByUserIds(Collection<Long> userIds);

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

    /** 按用户名或真实姓名模糊搜索用户 ID 列表 */
    List<Long> findUserIdsByKeyword(String keyword);
}
