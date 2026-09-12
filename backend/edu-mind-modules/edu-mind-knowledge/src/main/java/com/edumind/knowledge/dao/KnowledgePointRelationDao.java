package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgePointRelationEntity;
import com.edumind.knowledge.mapper.KnowledgePointRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgePointRelationDao {

    private final KnowledgePointRelationMapper relationMapper;

    public List<KnowledgePointRelationEntity> listBySourceIds(List<Long> sourceIds, List<String> types) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        return relationMapper.selectList(new LambdaQueryWrapper<KnowledgePointRelationEntity>()
                .in(KnowledgePointRelationEntity::getSourceKnowledgePointId, sourceIds)
                .in(types != null && !types.isEmpty(), KnowledgePointRelationEntity::getRelationType, types));
    }

    public List<KnowledgePointRelationEntity> listPrerequisites(Long knowledgePointId) {
        return relationMapper.selectList(new LambdaQueryWrapper<KnowledgePointRelationEntity>()
                .eq(KnowledgePointRelationEntity::getSourceKnowledgePointId, knowledgePointId)
                .eq(KnowledgePointRelationEntity::getRelationType, "prerequisite"));
    }

    public int insert(KnowledgePointRelationEntity entity) {
        return relationMapper.insert(entity);
    }

    public List<KnowledgePointRelationEntity> listByKnowledgePointId(Long knowledgePointId) {
        return relationMapper.selectList(new LambdaQueryWrapper<KnowledgePointRelationEntity>()
                .eq(KnowledgePointRelationEntity::getSourceKnowledgePointId, knowledgePointId)
                .or()
                .eq(KnowledgePointRelationEntity::getTargetKnowledgePointId, knowledgePointId));
    }

    public KnowledgePointRelationEntity findById(Long id) {
        return relationMapper.selectById(id);
    }

    public int deleteById(Long id) {
        return relationMapper.deleteById(id);
    }
}
