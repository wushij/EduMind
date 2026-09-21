package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysMemberOrgEntity;
import com.edumind.system.mapper.SysMemberOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员组织分配数据访问统一入口 DAO
 */
@Repository
@RequiredArgsConstructor
public class SysMemberOrgDao {

    private final SysMemberOrgMapper sysMemberOrgMapper;

    public long countByOrgId(Long tenantId, Long organizationId) {
        return sysMemberOrgMapper.selectCount(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId));
    }

    public List<SysMemberOrgEntity> listByOrgId(Long tenantId, Long organizationId) {
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId));
    }

    public List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId) {
        return listByOrgId(tenantId, organizationId).stream()
                .map(SysMemberOrgEntity::getMemberId)
                .collect(Collectors.toList());
    }

    public List<SysMemberOrgEntity> listByMemberId(Long tenantId, Long memberId) {
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getMemberId, memberId));
    }

    /** 批量按成员 ID 查询组织关系（单次 IN 查询），用于替代循环内逐成员查询 */
    public List<SysMemberOrgEntity> listByMemberIds(java.util.Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .in(SysMemberOrgEntity::getMemberId, memberIds));
    }

    public int insert(SysMemberOrgEntity entity) {
        return sysMemberOrgMapper.insert(entity);
    }

    public SysMemberOrgEntity findByTenantOrgAndMember(Long tenantId, Long organizationId, Long memberId) {
        return sysMemberOrgMapper.selectOne(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId)
                .eq(SysMemberOrgEntity::getMemberId, memberId));
    }

    public int deleteByTenantOrgAndMember(Long tenantId, Long organizationId, Long memberId) {
        return sysMemberOrgMapper.delete(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId)
                .eq(SysMemberOrgEntity::getMemberId, memberId));
    }

    public int updateById(SysMemberOrgEntity entity) {
        return sysMemberOrgMapper.updateById(entity);
    }

    public int deleteByOrgId(Long tenantId, Long organizationId) {
        return sysMemberOrgMapper.delete(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId));
    }

    public List<SysMemberOrgEntity> listByTenantId(Long tenantId) {
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId));
    }

    public List<SysMemberOrgEntity> listByOrgIds(Long tenantId, List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .in(SysMemberOrgEntity::getOrganizationId, orgIds));
    }

    public int deleteByTenantId(Long tenantId) {
        return sysMemberOrgMapper.delete(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId));
    }
}

