package com.edumind.system.api;

import java.util.List;
import java.util.Map;

/**
 * 租户领域跨模块只读查询公开 API
 */
public interface TenantQueryApi {

    /**
     * 根据租户ID获取学校租户基本信息 (Map/VO)
     */
    Map<String, Object> getTenantById(Long tenantId);

    /**
     * 根据租户编码获取学校租户ID
     */
    Long getTenantIdByCode(String code);

    /**
     * 获取指定用户可访问的所有租户列表
     */
    List<Map<String, Object>> listAvailableTenants(Long userId);

    /**
     * 校验用户是否属于指定租户
     */
    boolean isUserMemberOfTenant(Long userId, Long tenantId);

    /**
     * 获取当前所有处于启用状态的有效租户 ID 列表
     */
    List<Long> listActiveTenantIds();
}
