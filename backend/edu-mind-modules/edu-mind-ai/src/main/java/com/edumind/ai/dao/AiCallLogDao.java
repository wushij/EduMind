package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.context.TenantContext;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiCallLogDao {

    private final AiCallLogMapper aiCallLogMapper;

    public AiCallLogEntity findById(Long id) {
        return aiCallLogMapper.selectById(id);
    }

    public int insert(AiCallLogEntity entity) {
        if (entity.getTenantId() == null && TenantContext.getTenantId() != null && TenantContext.getTenantId() > 0) {
            entity.setTenantId(TenantContext.getTenantId());
        }
        return aiCallLogMapper.insert(entity);
    }

    public Page<AiCallLogEntity> page(Page<AiCallLogEntity> page, LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectPage(page, wrapper);
    }

    public List<AiCallLogEntity> list(LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectList(wrapper);
    }

    public long count(LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectCount(wrapper);
    }
}
