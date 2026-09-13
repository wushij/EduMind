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

    public List<SysCampusEntity> listByTenantId(Long tenantId) {
        return sysCampusMapper.selectList(new LambdaQueryWrapper<SysCampusEntity>()
                .eq(SysCampusEntity::getTenantId, tenantId)
                .eq(SysCampusEntity::getStatus, 1)
                .orderByAsc(SysCampusEntity::getId));
    }

    public int insert(SysCampusEntity entity) {
        return sysCampusMapper.insert(entity);
    }
}
