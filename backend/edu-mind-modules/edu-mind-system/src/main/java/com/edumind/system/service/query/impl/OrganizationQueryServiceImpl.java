package com.edumind.system.service.query.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.dao.SysMemberOrgDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.entity.SysMemberOrgEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.service.SysOrganizationService;
import com.edumind.system.service.query.OrganizationQueryService;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.tenant.OrganizationBriefVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationQueryServiceImpl implements OrganizationQueryService {

    private final SysOrganizationService sysOrganizationService;
    private final SysMemberOrgDao sysMemberOrgDao;
    private final SysOrganizationDao sysOrganizationDao;
    private final SysTenantMemberDao sysTenantMemberDao;

    @Override
    public List<OrganizationBriefVO> getOrganizationTree(Long tenantId) {
        List<OrganizationNodeVO> tree = sysOrganizationService.getTree(tenantId);
        return tree.stream().map(node -> OrganizationBriefVO.builder()
                .id(node.getId())
                .name(node.getName())
                .orgType(node.getOrgType())
                .memberCount(node.getMemberCount())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<MemberOrgBriefVO> listOrgsByMemberId(Long tenantId, Long memberId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        List<SysMemberOrgEntity> relations = sysMemberOrgDao.listByMemberId(resolvedTenantId, memberId);
        List<MemberOrgBriefVO> result = new ArrayList<>();
        for (SysMemberOrgEntity rel : relations) {
            SysOrganizationEntity org = sysOrganizationDao.findByIdAndTenantId(rel.getOrganizationId(), resolvedTenantId);
            if (org != null) {
                result.add(MemberOrgBriefVO.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .type(org.getOrgType())
                        .roleType(rel.getRoleType())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        return sysMemberOrgDao.listMemberIdsByOrgId(resolvedTenantId, organizationId);
    }

    @Override
    public List<Long> listUserIdsByOrgId(Long tenantId, Long organizationId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        List<Long> memberIds = sysMemberOrgDao.listMemberIdsByOrgId(resolvedTenantId, organizationId);
        if (CollectionUtils.isEmpty(memberIds)) {
            return Collections.emptyList();
        }
        return sysTenantMemberDao.listByIds(resolvedTenantId, memberIds).stream()
                .map(SysTenantMemberEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long resolveAndVerifyTenantId(Long explicitTenantId) {
        Long currentTenantId = TenantContext.getTenantId();
        boolean isGlobalAdmin = StpUtil.isLogin() && (
                Long.valueOf(1L).equals(StpUtil.getLoginIdAsLong())
                        || StpUtil.hasRole("ADMIN")
                        || StpUtil.hasRole("PLATFORM_ADMIN")
                        || StpUtil.hasRole("ROLE_ADMIN")
        );
        if (explicitTenantId != null) {
            if (isGlobalAdmin || (currentTenantId != null && explicitTenantId.equals(currentTenantId))) {
                return explicitTenantId;
            }
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他学校组织架构");
        }
        if (currentTenantId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "缺少租户上下文，拒绝访问组织架构");
        }
        return currentTenantId;
    }
}
