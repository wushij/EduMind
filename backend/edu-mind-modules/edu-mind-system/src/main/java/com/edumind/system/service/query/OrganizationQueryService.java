package com.edumind.system.service.query;

import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;

import java.util.List;

public interface OrganizationQueryService {

    List<OrganizationBriefVO> getOrganizationTree(Long tenantId);

    List<MemberOrgBriefVO> listOrgsByMemberId(Long tenantId, Long memberId);

    List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId);

    List<Long> listUserIdsByOrgId(Long tenantId, Long organizationId);

    MemberOrgBriefVO getPrimaryClassByUserId(Long tenantId, Long userId);
}
