package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.UserRoleEntity;
import com.edumind.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRoleDao {

    private final UserRoleMapper userRoleMapper;

    public List<UserRoleEntity> findByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
    }

    public List<Long> listRoleIdsByUserId(Long userId) {
        return findByUserId(userId).stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());
    }

    /** 批量按用户 ID 查询角色关联（单次 IN 查询，用于替代循环内逐条 findByUserId） */
    public List<UserRoleEntity> findByUserIds(java.util.Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .in(UserRoleEntity::getUserId, userIds));
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

    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        deleteByUserId(userId);
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            UserRoleEntity entity = new UserRoleEntity();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
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
}
