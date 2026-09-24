package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.mapper.KnowledgePointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgePointDao {

    private final KnowledgePointMapper knowledgePointMapper;

    public KnowledgePointEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return knowledgePointMapper.selectById(id);
    }

    public List<KnowledgePointEntity> findByCourseId(Long courseId, Long chapterId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KnowledgePointEntity> wrapper = new LambdaQueryWrapper<KnowledgePointEntity>()
                .eq(KnowledgePointEntity::getCourseId, courseId)
                .orderByAsc(KnowledgePointEntity::getSortOrder)
                .orderByAsc(KnowledgePointEntity::getId);
        if (chapterId != null) {
            wrapper.eq(KnowledgePointEntity::getChapterId, chapterId);
        }
        return knowledgePointMapper.selectList(wrapper);
    }

    public int insert(KnowledgePointEntity entity) {
        if (entity == null) {
            return 0;
        }
        return knowledgePointMapper.insert(entity);
    }

    public Long countByCourseId(Long courseId) {
        if (courseId == null) {
            return 0L;
        }
        return knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePointEntity>()
                .eq(KnowledgePointEntity::getCourseId, courseId));
    }

    public int updateById(KnowledgePointEntity entity) {
        if (entity == null) {
            return 0;
        }
        return knowledgePointMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return knowledgePointMapper.deleteById(id);
    }

    public int deleteByCourseId(Long courseId) {
        if (courseId == null) {
            return 0;
        }
        return knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePointEntity>()
                .eq(KnowledgePointEntity::getCourseId, courseId));
    }
}
