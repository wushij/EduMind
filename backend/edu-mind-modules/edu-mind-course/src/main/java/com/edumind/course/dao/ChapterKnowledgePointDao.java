package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.ChapterKnowledgePointEntity;
import com.edumind.course.mapper.ChapterKnowledgePointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChapterKnowledgePointDao {

    private final ChapterKnowledgePointMapper mapper;

    public List<ChapterKnowledgePointEntity> findByChapterId(Long chapterId) {
        if (chapterId == null) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<ChapterKnowledgePointEntity>()
                .eq(ChapterKnowledgePointEntity::getChapterId, chapterId)
                .orderByAsc(ChapterKnowledgePointEntity::getSortOrder)
                .orderByAsc(ChapterKnowledgePointEntity::getId));
    }

    public List<ChapterKnowledgePointEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<ChapterKnowledgePointEntity>()
                .eq(ChapterKnowledgePointEntity::getCourseId, courseId));
    }

    public Map<Long, Long> countByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapper.selectList(new LambdaQueryWrapper<ChapterKnowledgePointEntity>()
                        .in(ChapterKnowledgePointEntity::getChapterId, chapterIds))
                .stream()
                .collect(Collectors.groupingBy(ChapterKnowledgePointEntity::getChapterId, Collectors.counting()));
    }

    public void deleteByChapterId(Long chapterId) {
        if (chapterId == null) {
            return;
        }
        mapper.delete(new LambdaQueryWrapper<ChapterKnowledgePointEntity>()
                .eq(ChapterKnowledgePointEntity::getChapterId, chapterId));
    }

    public void insertBatch(Long courseId, Long chapterId, List<Long> knowledgePointIds) {
        deleteByChapterId(chapterId);
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        int sort = 1;
        for (Long kpId : knowledgePointIds) {
            if (kpId == null) {
                continue;
            }
            ChapterKnowledgePointEntity entity = new ChapterKnowledgePointEntity();
            entity.setCourseId(courseId);
            entity.setChapterId(chapterId);
            entity.setKnowledgePointId(kpId);
            entity.setSortOrder(sort++);
            entity.setCreateTime(java.time.LocalDateTime.now());
            mapper.insert(entity);
        }
    }
}
