package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiModelConfigDao {

    private final AiModelConfigMapper aiModelConfigMapper;

    public List<AiModelConfigEntity> listEnabled() {
        return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, true)
                .orderByAsc(AiModelConfigEntity::getSortOrder, AiModelConfigEntity::getPriority));
    }

    public List<AiModelConfigEntity> listAll() {
        return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigEntity>()
                .orderByDesc(AiModelConfigEntity::getIsDefault)
                .orderByAsc(AiModelConfigEntity::getSortOrder, AiModelConfigEntity::getPriority));
    }

    public List<AiModelConfigEntity> listByFilter(String configType, String status) {
        LambdaQueryWrapper<AiModelConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configType)) {
            wrapper.eq(AiModelConfigEntity::getConfigType, configType);
        }
        if (StringUtils.hasText(status)) {
            boolean enabled = "enabled".equalsIgnoreCase(status) || "1".equals(status);
            wrapper.eq(AiModelConfigEntity::getEnabled, enabled);
        }
        wrapper.orderByDesc(AiModelConfigEntity::getIsDefault)
                .orderByAsc(AiModelConfigEntity::getSortOrder, AiModelConfigEntity::getPriority);
        return aiModelConfigMapper.selectList(wrapper);
    }

    public AiModelConfigEntity findByModelKey(String modelKey) {
        return aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getModelKey, modelKey));
    }

    public AiModelConfigEntity findByConfigName(String configName) {
        return aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getConfigName, configName));
    }

    public AiModelConfigEntity findDefaultByType(String configType) {
        AiModelConfigEntity byDefault = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getConfigType, configType)
                .eq(AiModelConfigEntity::getIsDefault, true)
                .eq(AiModelConfigEntity::getEnabled, true)
                .last("LIMIT 1"));
        if (byDefault != null) {
            return byDefault;
        }
        return aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getConfigType, configType)
                .eq(AiModelConfigEntity::getEnabled, true)
                .orderByAsc(AiModelConfigEntity::getSortOrder, AiModelConfigEntity::getPriority)
                .last("LIMIT 1"));
    }

    public int insert(AiModelConfigEntity entity) {
        return aiModelConfigMapper.insert(entity);
    }

    public int updateById(AiModelConfigEntity entity) {
        return aiModelConfigMapper.updateById(entity);
    }

    public int deleteByConfigName(String configName) {
        return aiModelConfigMapper.delete(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getConfigName, configName));
    }

    public int clearDefaultByType(String configType, String excludeConfigName) {
        LambdaUpdateWrapper<AiModelConfigEntity> wrapper = new LambdaUpdateWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getConfigType, configType)
                .set(AiModelConfigEntity::getIsDefault, false);
        if (StringUtils.hasText(excludeConfigName)) {
            wrapper.ne(AiModelConfigEntity::getConfigName, excludeConfigName);
        }
        return aiModelConfigMapper.update(null, wrapper);
    }
}
