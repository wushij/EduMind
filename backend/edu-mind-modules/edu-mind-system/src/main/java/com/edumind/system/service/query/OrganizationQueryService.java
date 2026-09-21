package com.edumind.system.service.query;

import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface OrganizationQueryService {

    List<OrganizationBriefVO> getOrganizationTree(Long tenantId);

    List<MemberOrgBriefVO> listOrgsByMemberId(Long tenantId, Long memberId);

    List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId);

    List<Long> listUserIdsByOrgId(Long tenantId, Long organizationId);

    MemberOrgBriefVO getPrimaryClassByUserId(Long tenantId, Long userId);

    /**
     * 批量解析用户的主要行政班级与学号信息（固定次数批量查询）。
     * 用于替代循环内逐个调用 {@link #getPrimaryClassByUserId(Long, Long)}，消除 N+1。
     * 无成员记录的用户不会出现在返回 Map 中（与单条方法返回 null 等价）。
     */
    Map<Long, MemberOrgBriefVO> mapPrimaryClassesByUserIds(Long tenantId, Collection<Long> userIds);
}
