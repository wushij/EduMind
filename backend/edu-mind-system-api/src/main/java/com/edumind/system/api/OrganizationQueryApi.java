package com.edumind.system.api;

import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;

import java.util.List;

/**
 * 组织架构领域跨模块只读查询公开 API
 */
public interface OrganizationQueryApi {

    /**
     * 获取租户的完整组织架构树 (院系/专业/班级)
     */
    List<OrganizationBriefVO> getOrganizationTree(Long tenantId);

    /**
     * 获取指定班级/组织下的成员ID列表
     */
    List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId);

    /**
     * 获取指定班级/组织下的系统用户ID列表
     */
    List<Long> listUserIdsByOrgId(Long tenantId, Long organizationId);

    /**
     * 获取成员所属的组织信息
     */
    List<MemberOrgBriefVO> listOrgsByMemberId(Long tenantId, Long memberId);
}
