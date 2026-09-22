package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserRoleEntity;
import com.edumind.system.mapper.RoleMapper;
import com.edumind.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户-角色关联数据访问层（租户维度）
 * <p>隔离约定：平台级行 tenant_id = 0 对所有租户可见；其余行仅在自身租户内可见。
 * 无租户上下文时 Fail-Closed，仅返回平台级角色，绝不返回任意租户的授权。</p>
 */
@Repository
@RequiredArgsConstructor
public class UserRoleDao {

    /** 平台级角色编码：其授权对所有租户生效 */
    private static final Set<String> PLATFORM_ROLE_CODES = Set.of("ADMIN", "ROLE_ADMIN", "PLATFORM_ADMIN");

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public List<UserRoleEntity> findByUserId(Long userId) {
        return findByUserIdAndTenant(userId, TenantContext.getTenantId());
    }

    /**
     * 按「用户 + 租户」查询授权关系
     *
     * @param tenantId 目标租户；若为 null 则仅返回平台级授权（Fail-Closed）
     */
    public List<UserRoleEntity> findByUserIdAndTenant(Long userId, Long tenantId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserRoleEntity> wrapper = new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId);
        appendTenantScope(wrapper, tenantId);
        return userRoleMapper.selectList(wrapper);
    }

    public List<Long> listRoleIdsByUserId(Long userId) {
        return listRoleIdsByUserIdAndTenant(userId, TenantContext.getTenantId());
    }

    public List<Long> listRoleIdsByUserIdAndTenant(Long userId, Long tenantId) {
        return findByUserIdAndTenant(userId, tenantId).stream()
                .map(UserRoleEntity::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 批量按用户 ID 查询角色关联（单次 IN 查询，用于替代循环内逐条 findByUserId） */
    public List<UserRoleEntity> findByUserIds(Collection<Long> userIds) {
        return findByUserIdsAndTenant(userIds, TenantContext.getTenantId());
    }

    public List<UserRoleEntity> findByUserIdsAndTenant(Collection<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserRoleEntity> wrapper = new LambdaQueryWrapper<UserRoleEntity>()
                .in(UserRoleEntity::getUserId, userIds);
        appendTenantScope(wrapper, tenantId);
        return userRoleMapper.selectList(wrapper);
    }

    public List<Long> findRoleIdsByUserId(Long userId) {
        return listRoleIdsByUserId(userId);
    }

    public int deleteByUserId(Long userId) {
        return userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
    }

    public int insert(UserRoleEntity entity) {
        return userRoleMapper.insert(entity);
    }

    /**
     * 在当前租户范围内替换用户角色授权。
     * <p>仅清理「当前租户 + 平台级」的授权行，绝不影响该用户在其它租户内的既有授权。
     * 平台级角色（ADMIN / PLATFORM_ADMIN / ROLE_ADMIN）统一写为 tenant_id = 0。</p>
     */
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        replaceUserRoles(userId, roleIds, TenantContext.getTenantId());
    }

    public void replaceUserRoles(Long userId, List<Long> roleIds, Long tenantId) {
        if (userId == null) {
            return;
        }
        long scopedTenantId = tenantId != null && tenantId > 0 ? tenantId : UserRoleEntity.PLATFORM_TENANT_ID;
        // 只清理当前租户与平台级授权，保留该用户在其它租户的授权
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId)
                .and(w -> w.eq(UserRoleEntity::getTenantId, UserRoleEntity.PLATFORM_TENANT_ID)
                        .or().eq(UserRoleEntity::getTenantId, scopedTenantId)));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctRoleIds = roleIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> roleCodeMap = distinctRoleIds.isEmpty()
                ? Collections.emptyMap()
                : roleMapper.selectBatchIds(distinctRoleIds).stream()
                        .collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getRoleCode, (a, b) -> a));
        for (Long roleId : distinctRoleIds) {
            UserRoleEntity entity = new UserRoleEntity();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            entity.setTenantId(PLATFORM_ROLE_CODES.contains(roleCodeMap.get(roleId))
                    ? UserRoleEntity.PLATFORM_TENANT_ID
                    : scopedTenantId);
            userRoleMapper.insert(entity);
        }
    }

    public List<Long> findUserIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getRoleId, roleId))
                .stream()
                .map(UserRoleEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 租户范围条件：平台级(0) + 指定租户；无租户上下文时仅平台级（Fail-Closed）
     */
    private void appendTenantScope(LambdaQueryWrapper<UserRoleEntity> wrapper, Long tenantId) {
        if (tenantId != null && tenantId > 0) {
            long scoped = tenantId;
            wrapper.and(w -> w.eq(UserRoleEntity::getTenantId, UserRoleEntity.PLATFORM_TENANT_ID)
                    .or().eq(UserRoleEntity::getTenantId, scoped));
        } else {
            wrapper.eq(UserRoleEntity::getTenantId, UserRoleEntity.PLATFORM_TENANT_ID);
        }
    }
}
