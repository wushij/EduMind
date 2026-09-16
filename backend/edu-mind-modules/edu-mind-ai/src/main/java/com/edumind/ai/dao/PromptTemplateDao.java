package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.mapper.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PromptTemplateDao {

    private final PromptTemplateMapper promptTemplateMapper;

    public PromptTemplateEntity findById(Long id) {
        return promptTemplateMapper.selectById(id);
    }

    public PromptTemplateEntity findByCode(String code) {
        return promptTemplateMapper.selectOne(
                new LambdaQueryWrapper<PromptTemplateEntity>().eq(PromptTemplateEntity::getCode, code)
        );
    }

    public List<PromptTemplateEntity> list(String category, String status, String keyword) {
        LambdaQueryWrapper<PromptTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category.trim())) {
            wrapper.eq(PromptTemplateEntity::getCategory, category.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromptTemplateEntity::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PromptTemplateEntity::getName, keyword)
                    .or().like(PromptTemplateEntity::getCode, keyword));
        }
        wrapper.orderByDesc(PromptTemplateEntity::getUpdateTime);
        return promptTemplateMapper.selectList(wrapper);
    }

    public int insert(PromptTemplateEntity entity) {
        return promptTemplateMapper.insert(entity);
    }

    public int updateById(PromptTemplateEntity entity) {
        return promptTemplateMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return promptTemplateMapper.deleteById(id);
    }
}
