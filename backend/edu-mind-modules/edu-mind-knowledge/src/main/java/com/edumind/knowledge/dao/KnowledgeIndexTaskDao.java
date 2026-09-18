package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgeIndexTaskEntity;
import com.edumind.knowledge.mapper.KnowledgeIndexTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeIndexTaskDao {

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;

    public KnowledgeIndexTaskEntity findLatestByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
                        .eq(KnowledgeIndexTaskEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeIndexTaskEntity::getId)
                        .last("LIMIT 1")
        );
    }

    public int insert(KnowledgeIndexTaskEntity entity) {
        return knowledgeIndexTaskMapper.insert(entity);
    }

    public int updateById(KnowledgeIndexTaskEntity entity) {
        return knowledgeIndexTaskMapper.updateById(entity);
    }

    public KnowledgeIndexTaskEntity findLatestGlobal() {
        return knowledgeIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
                        .orderByDesc(KnowledgeIndexTaskEntity::getId)
                        .last("LIMIT 1")
        );
    }
}
