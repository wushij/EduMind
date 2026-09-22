package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserRoleEntity;
import com.edumind.system.mapper.RoleMapper;
import com.edumind.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoleDao {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleEntity findById(Long id) {
        return roleMapper.selectById(id);
    }

    public RoleEntity findByRoleCode(String roleCode) {
        return roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, roleCode));
    }

    public List<RoleEntity> findAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .orderByAsc(RoleEntity::getId));
    }

    public List<RoleEntity> findByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(ids);
    }

    public List<String> listRoleCodesByIds(List<Long> roleIds) {
        return findByIds(roleIds).stream()
                .map(RoleEntity::getRoleCode)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户在当前租户上下文内的有效角色（平台级 + 当前租户）。
     * 无租户上下文时 Fail-Closed，仅返回平台级角色（ADMIN / PLATFORM_ADMIN / ROLE_ADMIN）。
     */
    public List<RoleEntity> findRolesByUserId(Long userId) {
        return findRolesByUserIdAndTenant(userId, TenantContext.getTenantId());
    }

    /**
     * 查询用户在指定租户内的有效角色（平台级 + 指定租户）。
     * 用于跨租户判定场景（如以显式 tenantId 解析数据范围），避免依赖线程上下文造成误判。
     */
    public List<RoleEntity> findRolesByUserIdAndTenant(Long userId, Long tenantId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserRoleEntity> wrapper = new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId);
        if (tenantId != null && tenantId > 0) {
            long scoped = tenantId;
            wrapper.and(w -> w.eq(UserRoleEntity::getTenantId, UserRoleEntity.PLATFORM_TENANT_ID)
                    .or().eq(UserRoleEntity::getTenantId, scoped));
        } else {
            wrapper.eq(UserRoleEntity::getTenantId, UserRoleEntity.PLATFORM_TENANT_ID);
        }
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(wrapper);
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());
        return findByIds(roleIds);
    }

    public int insert(RoleEntity entity) {
        return roleMapper.insert(entity);
    }

    public int updateById(RoleEntity entity) {
        return roleMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return roleMapper.deleteById(id);
    }
}
