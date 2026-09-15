package com.edumind.system.service.query;

import com.edumind.system.vo.tenant.TenantBriefVO;

import java.util.List;

public interface TenantQueryService {

    TenantBriefVO getTenantById(Long tenantId);

    Long getTenantIdByCode(String code);

    List<TenantBriefVO> listAvailableTenants(Long userId);

    boolean isUserMemberOfTenant(Long userId, Long tenantId);

    List<Long> listActiveTenantIds();
}
