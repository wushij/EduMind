package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RolePermissionEntity;
import com.edumind.system.mapper.PermissionMapper;
import com.edumind.system.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RolePermissionDao {

    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public List<String> listPermissionCodesByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>().in(RolePermissionEntity::getRoleId, roleIds)
        ).stream().map(RolePermissionEntity::getPermissionId).distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectList(
                new LambdaQueryWrapper<PermissionEntity>().in(PermissionEntity::getId, permissionIds)
        ).stream().map(PermissionEntity::getPermissionCode).distinct().collect(Collectors.toList());
    }
}
