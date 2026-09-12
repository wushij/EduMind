package com.edumind.system.api.impl;

import com.edumind.system.api.UserQueryApi;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.service.user.UserVoAssembler;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserQueryApiImpl implements UserQueryApi {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final UserRoleDao userRoleDao;
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
}
