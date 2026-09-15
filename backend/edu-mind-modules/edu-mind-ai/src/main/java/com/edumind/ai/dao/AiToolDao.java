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
        wrapper.orderByAsc(AiToolEntity::getSortOrder)
                .orderByDesc(AiToolEntity::getUseCount);
        return aiToolMapper.selectList(wrapper);
    }

    public List<AiToolEntity> listForAdmin(String category, Integer status, String keyword, Boolean isRecommended) {
        LambdaQueryWrapper<AiToolEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AiToolEntity::getStatus, status);
        }
        if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category)) {
            if ("RECOMMENDED".equalsIgnoreCase(category)) {
                wrapper.eq(AiToolEntity::getIsRecommended, 1);
            } else {
                wrapper.eq(AiToolEntity::getCategory, category);
            }
        }
        if (isRecommended != null) {
            wrapper.eq(AiToolEntity::getIsRecommended, isRecommended ? 1 : 0);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiToolEntity::getName, keyword)
                    .or().like(AiToolEntity::getDescription, keyword)
                    .or().like(AiToolEntity::getTags, keyword)
                    .or().like(AiToolEntity::getRoute, keyword));
        }
        wrapper.orderByAsc(AiToolEntity::getSortOrder)
                .orderByDesc(AiToolEntity::getUseCount);
        return aiToolMapper.selectList(wrapper);
    }

    public List<AiToolEntity> listAll() {
        return aiToolMapper.selectList(new LambdaQueryWrapper<AiToolEntity>()
                .orderByAsc(AiToolEntity::getSortOrder)
                .orderByDesc(AiToolEntity::getUseCount));
    }

    public boolean existsById(String id) {
        return aiToolMapper.selectCount(new LambdaQueryWrapper<AiToolEntity>()
                .eq(AiToolEntity::getId, id)) > 0;
    }

    public void insert(AiToolEntity entity) {
        aiToolMapper.insert(entity);
    }

    public void update(AiToolEntity entity) {
        aiToolMapper.updateById(entity);
    }

    public void deleteById(String id) {
        aiToolMapper.deleteById(id);
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
