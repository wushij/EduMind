package com.edumind.system.api.impl;

import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.service.query.OrganizationQueryService;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationQueryApiImpl implements OrganizationQueryApi {

    private final OrganizationQueryService organizationQueryService;

    @Override
    public List<OrganizationBriefVO> getOrganizationTree(Long tenantId) {
        return organizationQueryService.getOrganizationTree(tenantId);
    }

    @Override
    public List<MemberOrgBriefVO> listOrgsByMemberId(Long tenantId, Long memberId) {
        return organizationQueryService.listOrgsByMemberId(tenantId, memberId);
    }

    @Override
    public List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId) {
        return organizationQueryService.listMemberIdsByOrgId(tenantId, organizationId);
    }

    @Override
    public List<Long> listUserIdsByOrgId(Long tenantId, Long organizationId) {
        return organizationQueryService.listUserIdsByOrgId(tenantId, organizationId);
    }
}
