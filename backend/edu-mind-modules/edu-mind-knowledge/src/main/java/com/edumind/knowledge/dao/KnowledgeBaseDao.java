package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeBaseDao {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseEntity findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public List<KnowledgeBaseEntity> findAll() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .orderByDesc(KnowledgeBaseEntity::getUpdateTime)
        );
    }

    public List<KnowledgeBaseEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getCourseId, courseId)
                        .orderByDesc(KnowledgeBaseEntity::getUpdateTime)
        );
    }

    public List<KnowledgeBaseEntity> findByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .in(KnowledgeBaseEntity::getCourseId, courseIds)
                        .orderByDesc(KnowledgeBaseEntity::getUpdateTime)
        );
    }

    public int insert(KnowledgeBaseEntity entity) {
        if (entity.getTenantId() == null && TenantContext.getTenantId() != null && TenantContext.getTenantId() > 0) {
            entity.setTenantId(TenantContext.getTenantId());
        }
        return knowledgeBaseMapper.insert(entity);
    }

    public int updateById(KnowledgeBaseEntity entity) {
        return knowledgeBaseMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return knowledgeBaseMapper.deleteById(id);
    }
}
