package com.edumind.system.api;

/**
 * 租户五级数据范围解析公开服务 API
 */
public interface TenantDataScopeApi {

    /**
     * 根据当前用户与租户上下文解析数据范围 (平台超管 / 租户管理员 / 院系管理员 / 教师 / 学生)
     *
     * @param userId   当前登录用户 ID
     * @param tenantId 当前租户 ID
     * @return 数据范围模型
     */
    TenantDataScope resolve(Long userId, Long tenantId);
}
