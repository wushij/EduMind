package com.edumind.system.api.impl;

import com.edumind.system.api.UserQueryApi;
import com.edumind.system.service.query.UserQueryService;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryApiImpl implements UserQueryApi {

    private final UserQueryService userQueryService;

    @Override
    public UserBriefVO getUserById(Long userId) {
        return userQueryService.getUserById(userId);
    }

    @Override
    public List<String> getRolesByUserId(Long userId) {
        return userQueryService.getRolesByUserId(userId);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        return userQueryService.getPermissionsByUserId(userId);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return userQueryService.listUserIdsByRoleId(roleId);
    }

    @Override
    public List<Long> listAllActiveUserIds() {
        return userQueryService.listAllActiveUserIds();
    }

    @Override
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        return userQueryService.listUserIdsByRoleCode(roleCode);
    }

    @Override
    public long countActiveUsers() {
        return userQueryService.countActiveUsers();
    }

    @Override
    public long countUsersByRoleCode(String roleCode) {
        return userQueryService.countUsersByRoleCode(roleCode);
    }

    @Override
    public List<Long> listActiveUserIdsByTenantId(Long tenantId) {
        return userQueryService.listActiveUserIdsByTenantId(tenantId);
    }

    @Override
    public long countActiveUsersByTenantId(Long tenantId) {
        return userQueryService.countActiveUsersByTenantId(tenantId);
    }

    @Override
    public List<Long> listUserIdsByTenantAndRole(Long tenantId, String roleCode) {
        return userQueryService.listUserIdsByTenantAndRole(tenantId, roleCode);
    }

    @Override
    public long countUsersByTenantAndRole(Long tenantId, String roleCode) {
        return userQueryService.countUsersByTenantAndRole(tenantId, roleCode);
    }

    @Override
    public List<Long> findUserIdsByKeyword(String keyword) {
        return userQueryService.findUserIdsByKeyword(keyword);
    }
}
