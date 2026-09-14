package com.edumind.system.api.impl;

import com.edumind.system.api.UserQueryApi;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.service.user.UserVoAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserQueryApiImpl implements UserQueryApi {

    private final UserDao userDao;
    private final UserRoleDao userRoleDao;
    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final UserVoAssembler userVoAssembler;

    @Override
    public Object getUserById(Long userId) {
        UserEntity user = userDao.findById(userId);
        return userVoAssembler.toVO(user);
    }

    @Override
    public List<String> getRolesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return roleDao.findRolesByUserId(userId).stream()
                .map(RoleEntity::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return permissionDao.findPermissionsByUserId(userId).stream()
                .map(PermissionEntity::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return userRoleDao.findUserIdsByRoleId(roleId);
    }

    @Override
    public List<Long> listAllActiveUserIds() {
        return userDao.listAllActiveUserIds();
    }

    @Override
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        RoleEntity role = roleDao.findByRoleCode(roleCode.trim().toUpperCase());
        if (role == null) {
            return Collections.emptyList();
        }
        return userRoleDao.findUserIdsByRoleId(role.getId());
    }

    @Override
    public long countActiveUsers() {
        return userDao.countActiveUsers();
    }

    @Override
    public long countUsersByRoleCode(String roleCode) {
        return listUserIdsByRoleCode(roleCode).size();
    }

    @Override
    public List<Long> listActiveUserIdsByTenantId(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return sysTenantMemberDao.listUserIdsByTenantId(tenantId);
    }

    @Override
    public long countActiveUsersByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        return sysTenantMemberDao.countActiveUsersByTenantId(tenantId);
    }

    @Override
    public List<Long> listUserIdsByTenantAndRole(Long tenantId, String roleCode) {
        if (tenantId == null || roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> tenantUserIds = listActiveUserIdsByTenantId(tenantId);
        if (tenantUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        RoleEntity role = roleDao.findByRoleCode(roleCode.trim().toUpperCase());
        if (role == null) {
            return Collections.emptyList();
        }
        List<Long> roleUserIds = userRoleDao.findUserIdsByRoleId(role.getId());
        if (roleUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> roleUserSet = new HashSet<>(roleUserIds);
        return tenantUserIds.stream()
                .filter(roleUserSet::contains)
                .collect(Collectors.toList());
    }

    @Override
    public long countUsersByTenantAndRole(Long tenantId, String roleCode) {
        return listUserIdsByTenantAndRole(tenantId, roleCode).size();
    }
}
