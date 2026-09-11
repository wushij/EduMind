package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public List<RoleEntity> findRolesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
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
