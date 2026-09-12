package com.edumind.system.service.user;

import com.edumind.system.converter.UserConverter;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserVoAssembler {

    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final UserConverter userConverter;

    public UserVO toVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        List<RoleEntity> roles = roleDao.findRolesByUserId(entity.getId());
        List<String> roleCodes = roles.stream().map(RoleEntity::getRoleCode).collect(Collectors.toList());
        List<Long> roleIds = roles.stream().map(RoleEntity::getId).collect(Collectors.toList());
        List<String> permissions = permissionDao.findByRoleIds(roleIds).stream()
                .map(PermissionEntity::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
        return userConverter.toVO(entity, roleCodes, permissions);
    }
}
