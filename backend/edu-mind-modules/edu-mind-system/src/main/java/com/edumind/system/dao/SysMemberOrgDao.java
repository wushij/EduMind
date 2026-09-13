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

    public List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId) {
        List<SysMemberOrgEntity> list = sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId));
        return list.stream().map(SysMemberOrgEntity::getMemberId).collect(Collectors.toList());
    }

    public List<SysMemberOrgEntity> listByMemberId(Long tenantId, Long memberId) {
        return sysMemberOrgMapper.selectList(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getMemberId, memberId));
    }

    public int insert(SysMemberOrgEntity entity) {
        return sysMemberOrgMapper.insert(entity);
    }

    public int deleteByOrgId(Long tenantId, Long organizationId) {
        return sysMemberOrgMapper.delete(new LambdaQueryWrapper<SysMemberOrgEntity>()
                .eq(SysMemberOrgEntity::getTenantId, tenantId)
                .eq(SysMemberOrgEntity::getOrganizationId, organizationId));
    }
}
