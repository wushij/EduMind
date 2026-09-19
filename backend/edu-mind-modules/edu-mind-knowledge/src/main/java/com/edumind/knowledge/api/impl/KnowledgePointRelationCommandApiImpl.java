package com.edumind.knowledge.api.impl;

import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import com.edumind.knowledge.dao.KnowledgePointRelationDao;
import com.edumind.knowledge.entity.KnowledgePointRelationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgePointRelationCommandApiImpl implements KnowledgePointRelationCommandApi {

    private final KnowledgePointRelationDao knowledgePointRelationDao;

    @Override
    public void syncPrerequisites(Long sourceKnowledgePointId, List<Long> prerequisiteTargetIds) {
        if (sourceKnowledgePointId == null) {
            return;
        }
        knowledgePointRelationDao.deletePrerequisitesForSource(sourceKnowledgePointId);
        if (CollectionUtils.isEmpty(prerequisiteTargetIds)) {
            return;
        }
        for (Long targetId : prerequisiteTargetIds) {
            if (targetId == null || targetId.equals(sourceKnowledgePointId)) {
                continue;
            }
            KnowledgePointRelationEntity entity = new KnowledgePointRelationEntity();
            entity.setSourceKnowledgePointId(sourceKnowledgePointId);
            entity.setTargetKnowledgePointId(targetId);
            entity.setRelationType("prerequisite");
            entity.setCreateTime(LocalDateTime.now());
            knowledgePointRelationDao.insert(entity);
        }
    }

    @Override
    public void deleteAllRelationsForPoint(Long knowledgePointId) {
        if (knowledgePointId == null) {
            return;
        }
        knowledgePointRelationDao.deleteAllForPoint(knowledgePointId);
    }

    @Override
    public Map<Long, List<Long>> listPrerequisiteTargetsBySourceIds(List<Long> sourceKnowledgePointIds) {
        if (CollectionUtils.isEmpty(sourceKnowledgePointIds)) {
            return Map.of();
        }
        List<Long> sourceIds = sourceKnowledgePointIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        List<KnowledgePointRelationEntity> relations = knowledgePointRelationDao.listBySourceIds(
                sourceIds, List.of("prerequisite"));
        Map<Long, List<Long>> result = new HashMap<>();
        for (KnowledgePointRelationEntity rel : relations) {
            if (rel == null || rel.getSourceKnowledgePointId() == null || rel.getTargetKnowledgePointId() == null) {
                continue;
            }
            result.computeIfAbsent(rel.getSourceKnowledgePointId(), k -> new ArrayList<>())
                    .add(rel.getTargetKnowledgePointId());
        }
        return result;
    }
}
