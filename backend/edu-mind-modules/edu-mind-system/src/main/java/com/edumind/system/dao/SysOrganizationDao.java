package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.mapper.SysOrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysOrganizationDao {

    private final SysOrganizationMapper sysOrganizationMapper;

    public List<SysOrganizationEntity> listByTenantId(Long tenantId) {
        return sysOrganizationMapper.selectList(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getTenantId, tenantId)
                .orderByAsc(SysOrganizationEntity::getSortOrder)
                .orderByAsc(SysOrganizationEntity::getId));
    }

    public SysOrganizationEntity findById(Long id) {
        return sysOrganizationMapper.selectById(id);
    }

    public SysOrganizationEntity findByIdAndTenantId(Long id, Long tenantId) {
        return sysOrganizationMapper.selectOne(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getId, id)
                .eq(SysOrganizationEntity::getTenantId, tenantId));
    }

    public long countChildren(Long tenantId, Long parentId) {
        return sysOrganizationMapper.selectCount(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getTenantId, tenantId)
                .eq(SysOrganizationEntity::getParentId, parentId));
    }

    public int insert(SysOrganizationEntity entity) {
        return sysOrganizationMapper.insert(entity);
    }

    public int updateById(SysOrganizationEntity entity) {
        return sysOrganizationMapper.updateById(entity);
    }

    public int deleteByIdAndTenantId(Long id, Long tenantId) {
        return sysOrganizationMapper.delete(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getId, id)
                .eq(SysOrganizationEntity::getTenantId, tenantId));
    }

    public long countByType(Long tenantId, String orgType) {
        return sysOrganizationMapper.selectCount(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getTenantId, tenantId)
                .eq(SysOrganizationEntity::getOrgType, orgType));
    }

    public int deleteById(Long id) {
        return sysOrganizationMapper.deleteById(id);
    }

    public int deleteByTenantId(Long tenantId) {
        return sysOrganizationMapper.delete(new LambdaQueryWrapper<SysOrganizationEntity>()
                .eq(SysOrganizationEntity::getTenantId, tenantId));
    }
}

