package com.edumind.system.api.impl;

import com.edumind.system.api.TenantQueryApi;
import com.edumind.system.service.query.TenantQueryService;
import com.edumind.system.vo.tenant.TenantBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantQueryApiImpl implements TenantQueryApi {

    private final TenantQueryService tenantQueryService;

    @Override
    public TenantBriefVO getTenantById(Long tenantId) {
        return tenantQueryService.getTenantById(tenantId);
    }

    @Override
    public Long getTenantIdByCode(String code) {
        return tenantQueryService.getTenantIdByCode(code);
    }

    @Override
    public List<TenantBriefVO> listAvailableTenants(Long userId) {
        return tenantQueryService.listAvailableTenants(userId);
    }

    @Override
    public boolean isUserMemberOfTenant(Long userId, Long tenantId) {
        return tenantQueryService.isUserMemberOfTenant(userId, tenantId);
    }

    @Override
    public List<Long> listActiveTenantIds() {
        return tenantQueryService.listActiveTenantIds();
    }
}
