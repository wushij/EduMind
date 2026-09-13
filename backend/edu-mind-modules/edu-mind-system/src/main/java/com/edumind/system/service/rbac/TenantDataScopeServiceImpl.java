package com.edumind.system.service.rbac;

import com.edumind.common.context.TenantContext;
import com.edumind.system.api.TenantDataScope;
import com.edumind.system.api.TenantDataScopeApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.dao.SysMemberOrgDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.entity.SysMemberOrgEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 租户五级数据范围解析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDataScopeServiceImpl implements TenantDataScopeApi {

    private final UserQueryApi userQueryApi;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final SysMemberOrgDao sysMemberOrgDao;
    private final SysOrganizationDao sysOrganizationDao;

    @Override
    public TenantDataScope resolve(Long userId, Long tenantId) {
        if (userId == null) {
            return TenantDataScope.builder().allTenant(false).canManageQuota(false).build();
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();

        List<String> roles = userQueryApi.getRolesByUserId(userId);
        if (roles == null) {
            roles = Collections.emptyList();
        }

        // 1. 平台超级管理员 (ADMIN / PLATFORM_ADMIN)
        if (roles.contains("ADMIN") || roles.contains("PLATFORM_ADMIN")) {
            return TenantDataScope.fullTenant(TenantContext.isDelegatedSession());
        }

        // 2. 租户管理员 (TENANT_ADMIN)
        if (roles.contains("TENANT_ADMIN")) {
            return TenantDataScope.builder()
                    .allTenant(true)
                    .platformDelegated(false)
                    .canManageQuota(true)
                    .orgIds(Collections.emptySet())
                    .courseIds(Collections.emptySet())
                    .build();
        }

        if (resolvedTenantId == null) {
            return TenantDataScope.builder().allTenant(false).canManageQuota(false).build();
        }

        // 查询该用户在当前租户下的成员身份
        SysTenantMemberEntity member = sysTenantMemberDao.findByTenantAndUser(resolvedTenantId, userId);
        Set<Long> visibleOrgIds = new HashSet<>();

        if (member != null) {
            List<SysMemberOrgEntity> relations = sysMemberOrgDao.listByMemberId(resolvedTenantId, member.getId());
            boolean isOrgAdmin = roles.contains("ORG_ADMIN");

            // 收集所管理的组织根节点 (HEAD_TEACHER 或 ORG_ADMIN)
            Set<Long> managedRootOrgIds = new HashSet<>();
            for (SysMemberOrgEntity rel : relations) {
                if (isOrgAdmin || "HEAD_TEACHER".equalsIgnoreCase(rel.getRoleType()) || "ORG_ADMIN".equalsIgnoreCase(rel.getRoleType())) {
                    managedRootOrgIds.add(rel.getOrganizationId());
                }
                // 默认将自身关联的组织加入
                visibleOrgIds.add(rel.getOrganizationId());
            }

            // 若具有院系管理权限，递归展开子树
            if (!managedRootOrgIds.isEmpty()) {
                List<SysOrganizationEntity> allOrgs = sysOrganizationDao.listByTenantId(resolvedTenantId);
                expandChildOrgs(managedRootOrgIds, allOrgs, visibleOrgIds);
            }
        }

        return TenantDataScope.builder()
                .allTenant(false)
                .platformDelegated(false)
                .canManageQuota(false)
                .orgIds(visibleOrgIds)
                .courseIds(new HashSet<>())
                .build();
    }

    private void expandChildOrgs(Set<Long> parentIds, List<SysOrganizationEntity> allOrgs, Set<Long> result) {
        if (CollectionUtils.isEmpty(parentIds) || CollectionUtils.isEmpty(allOrgs)) {
            return;
        }
        result.addAll(parentIds);
        Set<Long> nextParents = new HashSet<>();
        for (SysOrganizationEntity org : allOrgs) {
            if (org.getParentId() != null && parentIds.contains(org.getParentId()) && !result.contains(org.getId())) {
                nextParents.add(org.getId());
                result.add(org.getId());
            }
        }
        if (!nextParents.isEmpty()) {
            expandChildOrgs(nextParents, allOrgs, result);
        }
    }
}
