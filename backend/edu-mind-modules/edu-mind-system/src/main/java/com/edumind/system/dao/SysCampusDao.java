package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysCampusEntity;
import com.edumind.system.mapper.SysCampusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysCampusDao {

    private final SysCampusMapper sysCampusMapper;

    public SysCampusEntity findById(Long id) {
        if (id == null) return null;
        return sysCampusMapper.selectById(id);
    }

    public List<SysCampusEntity> listByTenantId(Long tenantId) {
        return sysCampusMapper.selectList(new LambdaQueryWrapper<SysCampusEntity>()
                .eq(SysCampusEntity::getTenantId, tenantId)
                .eq(SysCampusEntity::getStatus, 1)
                .orderByAsc(SysCampusEntity::getId));
    }

    public List<SysCampusEntity> listAllByTenantId(Long tenantId) {
        return sysCampusMapper.selectList(new LambdaQueryWrapper<SysCampusEntity>()
                .eq(SysCampusEntity::getTenantId, tenantId)
                .orderByAsc(SysCampusEntity::getId));
    }

    public SysCampusEntity findByCode(Long tenantId, String code) {
        return sysCampusMapper.selectOne(new LambdaQueryWrapper<SysCampusEntity>()
                .eq(SysCampusEntity::getTenantId, tenantId)
                .eq(SysCampusEntity::getCode, code));
    }

    public long countByTenantId(Long tenantId) {
        return sysCampusMapper.selectCount(new LambdaQueryWrapper<SysCampusEntity>()
                .eq(SysCampusEntity::getTenantId, tenantId));
    }

    public long countAll() {
        return sysCampusMapper.selectCount(null);
    }

    public int insert(SysCampusEntity entity) {
        return sysCampusMapper.insert(entity);
    }

    public int updateById(SysCampusEntity entity) {
        return sysCampusMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return sysCampusMapper.deleteById(id);
    }
}
