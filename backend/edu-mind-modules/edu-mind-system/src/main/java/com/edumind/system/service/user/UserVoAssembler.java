package com.edumind.system.service.user;

import com.edumind.common.context.TenantContext;
import com.edumind.system.converter.UserConverter;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserVoAssembler {

    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final UserConverter userConverter;

    /**
     * 按当前租户上下文装配用户视图（角色与权限均随租户变化）
     */
    public UserVO toVO(UserEntity entity) {
        return toVO(entity, TenantContext.getTenantId());
    }

    /**
     * 按显式租户装配用户视图。
     * 登录场景必须使用本方法：登录请求尚未建立租户上下文，
     * 若依赖线程上下文会只解析到平台级角色，导致登录后菜单与权限缺失。
     */
    public UserVO toVO(UserEntity entity, Long tenantId) {
        if (entity == null) {
            return null;
        }
        List<RoleEntity> roles = roleDao.findRolesByUserIdAndTenant(entity.getId(), tenantId);
        List<String> roleCodes = roles.stream()
                .map(RoleEntity::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
        List<Long> roleIds = roles.stream().map(RoleEntity::getId).collect(Collectors.toList());
        List<String> permissions = roleIds.isEmpty()
                ? Collections.emptyList()
                : permissionDao.findByRoleIds(roleIds).stream()
                        .map(PermissionEntity::getPermissionCode)
                        .distinct()
                        .collect(Collectors.toList());
        return userConverter.toVO(entity, roleCodes, permissions);
    }
}
