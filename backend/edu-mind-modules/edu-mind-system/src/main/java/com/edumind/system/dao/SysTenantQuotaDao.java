package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.mapper.SysTenantQuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysTenantQuotaDao {

    private final SysTenantQuotaMapper sysTenantQuotaMapper;

    public List<SysTenantQuotaEntity> listByTenantId(Long tenantId) {
        return sysTenantQuotaMapper.selectList(new LambdaQueryWrapper<SysTenantQuotaEntity>()
                .eq(SysTenantQuotaEntity::getTenantId, tenantId));
    }

    public SysTenantQuotaEntity findByTenantAndType(Long tenantId, String quotaType) {
        return sysTenantQuotaMapper.selectOne(new LambdaQueryWrapper<SysTenantQuotaEntity>()
                .eq(SysTenantQuotaEntity::getTenantId, tenantId)
                .eq(SysTenantQuotaEntity::getQuotaType, quotaType));
    }

    public int insert(SysTenantQuotaEntity entity) {
        return sysTenantQuotaMapper.insert(entity);
    }

    public int updateById(SysTenantQuotaEntity entity) {
        return sysTenantQuotaMapper.updateById(entity);
    }
}
