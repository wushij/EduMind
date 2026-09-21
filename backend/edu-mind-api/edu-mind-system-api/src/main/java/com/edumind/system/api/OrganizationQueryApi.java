package com.edumind.system.api;

import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    /**
     * 根据系统用户ID获取该用户所属的主要行政班级与学号信息
     */
    MemberOrgBriefVO getPrimaryClassByUserId(Long tenantId, Long userId);

    /**
     * 批量获取多个用户的主要行政班级与学号信息（固定次数批量查询）。
     * 列表/统计场景请优先使用本方法，替代循环内逐个调用 {@link #getPrimaryClassByUserId(Long, Long)}，避免 N+1。
     */
    Map<Long, MemberOrgBriefVO> mapPrimaryClassesByUserIds(Long tenantId, Collection<Long> userIds);
}
