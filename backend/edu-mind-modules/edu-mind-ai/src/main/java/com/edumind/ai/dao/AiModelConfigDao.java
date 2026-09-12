package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiModelConfigDao {

    private final AiModelConfigMapper aiModelConfigMapper;

    public List<AiModelConfigEntity> listEnabled() {
        return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, true)
                .orderByAsc(AiModelConfigEntity::getPriority));
    }

    public AiModelConfigEntity findByModelKey(String modelKey) {
        return aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getModelKey, modelKey));
    }

    public List<AiModelConfigEntity> listAll() {
        return aiModelConfigMapper.selectList(new LambdaQueryWrapper<>());
    }

    public int updateById(AiModelConfigEntity entity) {
        return aiModelConfigMapper.updateById(entity);
    }
}
