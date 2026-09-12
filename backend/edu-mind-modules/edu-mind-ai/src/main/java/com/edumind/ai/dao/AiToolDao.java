package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.mapper.AiToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiToolDao {

    private final AiToolMapper aiToolMapper;

    public List<AiToolEntity> list(String category, String keyword) {
        LambdaQueryWrapper<AiToolEntity> wrapper = new LambdaQueryWrapper<AiToolEntity>()
                .eq(AiToolEntity::getStatus, 1);
        if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category)) {
            if ("RECOMMENDED".equalsIgnoreCase(category)) {
                wrapper.eq(AiToolEntity::getIsRecommended, 1);
            } else {
                wrapper.eq(AiToolEntity::getCategory, category);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiToolEntity::getName, keyword)
                    .or().like(AiToolEntity::getDescription, keyword)
                    .or().like(AiToolEntity::getTags, keyword));
        }
        wrapper.orderByDesc(AiToolEntity::getUseCount);
        return aiToolMapper.selectList(wrapper);
    }

    public AiToolEntity findById(String id) {
        return aiToolMapper.selectById(id);
    }

    public void incrementUseCount(String id) {
        AiToolEntity entity = aiToolMapper.selectById(id);
        if (entity == null) {
            return;
        }
        int current = entity.getUseCount() == null ? 0 : entity.getUseCount();
        AiToolEntity update = new AiToolEntity();
        update.setId(id);
        update.setUseCount(current + 1);
        aiToolMapper.updateById(update);
    }
}
