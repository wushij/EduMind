package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.system.entity.PermissionEntity;
import com.edumind.system.entity.RolePermissionEntity;
import com.edumind.system.mapper.PermissionMapper;
import com.edumind.system.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PermissionDao {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleDao userRoleDao;

    public PermissionEntity findById(Long id) {
        return permissionMapper.selectById(id);
    }

    public List<PermissionEntity> findAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
                .orderByAsc(PermissionEntity::getId));
    }

    public List<PermissionEntity> findByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return permissionMapper.selectBatchIds(ids);
    }

    public List<PermissionEntity> findByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return findByRoleIds(Collections.singletonList(roleId));
    }

    public List<PermissionEntity> findByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = findPermissionIdsByRoleIds(roleIds);
        return findByIds(permissionIds);
    }

    /**
     * 查询用户在当前租户上下文内的权限（平台级 + 当前租户），切换租户后结果随之变化。
     */
    public List<PermissionEntity> findPermissionsByUserId(Long userId) {
        return findPermissionsByUserIdAndTenant(userId, TenantContext.getTenantId());
    }

    public List<PermissionEntity> findPermissionsByUserIdAndTenant(Long userId, Long tenantId) {
        List<Long> roleIds = userRoleDao.listRoleIdsByUserIdAndTenant(userId, tenantId);
        return findByRoleIds(roleIds);
    }

    public List<Long> findPermissionIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .in(RolePermissionEntity::getRoleId, roleIds))
                .stream()
                .map(RolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());
    }

    public int deleteByRoleId(Long roleId) {
        return rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
    }

    public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        deleteByRoleId(roleId);
        if (roleId == null || CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        for (Long permissionId : permissionIds) {
            RolePermissionEntity entity = new RolePermissionEntity();
            entity.setRoleId(roleId);
            entity.setPermissionId(permissionId);
            rolePermissionMapper.insert(entity);
        }
    }
}
