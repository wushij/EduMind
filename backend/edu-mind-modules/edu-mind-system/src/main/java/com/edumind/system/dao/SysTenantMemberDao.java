package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.mapper.SysTenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysTenantMemberDao {

    private final SysTenantMemberMapper sysTenantMemberMapper;

    public SysTenantMemberEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return sysTenantMemberMapper.selectById(id);
    }

    public List<SysTenantMemberEntity> listByUserId(Long userId) {
        return sysTenantMemberMapper.selectList(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getUserId, userId)
                .eq(SysTenantMemberEntity::getStatus, 1));
    }

    public SysTenantMemberEntity findByTenantAndUser(Long tenantId, Long userId) {
        return sysTenantMemberMapper.selectOne(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId)
                .eq(SysTenantMemberEntity::getUserId, userId));
    }

    public List<SysTenantMemberEntity> listByIds(Long tenantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sysTenantMemberMapper.selectList(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId)
                .in(SysTenantMemberEntity::getId, ids));
    }

    public int insert(SysTenantMemberEntity entity) {
        return sysTenantMemberMapper.insert(entity);
    }

    public int deleteByTenantId(Long tenantId) {
        return sysTenantMemberMapper.delete(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId));
    }

    public List<Long> listUserIdsByTenantId(Long tenantId) {
        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }
        return sysTenantMemberMapper.selectList(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId)
                .eq(SysTenantMemberEntity::getStatus, 1))
                .stream()
                .map(SysTenantMemberEntity::getUserId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    public long countActiveUsersByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        return sysTenantMemberMapper.selectCount(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId)
                .eq(SysTenantMemberEntity::getStatus, 1));
    }

    public List<SysTenantMemberEntity> listByTenantId(Long tenantId) {
        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }
        return sysTenantMemberMapper.selectList(new LambdaQueryWrapper<SysTenantMemberEntity>()
                .eq(SysTenantMemberEntity::getTenantId, tenantId)
                .eq(SysTenantMemberEntity::getStatus, 1));
    }
}

