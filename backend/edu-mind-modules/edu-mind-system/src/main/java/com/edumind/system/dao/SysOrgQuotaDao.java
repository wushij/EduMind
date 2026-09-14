package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysOrgQuotaEntity;
import com.edumind.system.mapper.SysOrgQuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 组织院系算力配额 DAO
 */
@Repository
@RequiredArgsConstructor
public class SysOrgQuotaDao {

    private final SysOrgQuotaMapper sysOrgQuotaMapper;

    public List<SysOrgQuotaEntity> listByTenantId(Long tenantId) {
        return sysOrgQuotaMapper.selectList(new LambdaQueryWrapper<SysOrgQuotaEntity>()
                .eq(SysOrgQuotaEntity::getTenantId, tenantId));
    }

    public List<SysOrgQuotaEntity> listByTenantAndOrgId(Long tenantId, Long orgId) {
        return sysOrgQuotaMapper.selectList(new LambdaQueryWrapper<SysOrgQuotaEntity>()
                .eq(SysOrgQuotaEntity::getTenantId, tenantId)
                .eq(SysOrgQuotaEntity::getOrgId, orgId));
    }

    public SysOrgQuotaEntity findByTenantOrgAndType(Long tenantId, Long orgId, String quotaType) {
        return sysOrgQuotaMapper.selectOne(new LambdaQueryWrapper<SysOrgQuotaEntity>()
                .eq(SysOrgQuotaEntity::getTenantId, tenantId)
                .eq(SysOrgQuotaEntity::getOrgId, orgId)
                .eq(SysOrgQuotaEntity::getQuotaType, quotaType));
    }

    public int insert(SysOrgQuotaEntity entity) {
        return sysOrgQuotaMapper.insert(entity);
    }

    public int updateById(SysOrgQuotaEntity entity) {
        return sysOrgQuotaMapper.updateById(entity);
    }

    public int consumeOrgQuotaAtomic(Long tenantId, Long orgId, String quotaType, long delta) {
        return sysOrgQuotaMapper.consumeOrgQuotaAtomic(tenantId, orgId, quotaType, delta);
    }
}
