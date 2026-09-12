package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.PromptTemplateVersionEntity;
import com.edumind.ai.mapper.PromptTemplateVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PromptTemplateVersionDao {

    private final PromptTemplateVersionMapper promptTemplateVersionMapper;

    public int insert(PromptTemplateVersionEntity entity) {
        return promptTemplateVersionMapper.insert(entity);
    }

    public List<PromptTemplateVersionEntity> listByTemplateId(Long templateId) {
        return promptTemplateVersionMapper.selectList(
                new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                        .eq(PromptTemplateVersionEntity::getTemplateId, templateId)
                        .orderByDesc(PromptTemplateVersionEntity::getVersion)
        );
    }
}
