package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.system.entity.SysTermEntity;
import com.edumind.system.mapper.SysTermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysTermDao {

    private final SysTermMapper sysTermMapper;

    public List<SysTermEntity> listByTenantId(Long tenantId) {
        return sysTermMapper.selectList(new LambdaQueryWrapper<SysTermEntity>()
                .eq(SysTermEntity::getTenantId, tenantId)
                .orderByDesc(SysTermEntity::getStartDate));
    }

    public SysTermEntity findCurrentTerm(Long tenantId) {
        return sysTermMapper.selectOne(new LambdaQueryWrapper<SysTermEntity>()
                .eq(SysTermEntity::getTenantId, tenantId)
                .eq(SysTermEntity::getIsCurrent, 1));
    }

    public int insert(SysTermEntity entity) {
        return sysTermMapper.insert(entity);
    }
}
