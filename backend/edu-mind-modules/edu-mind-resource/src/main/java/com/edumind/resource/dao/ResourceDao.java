package com.edumind.resource.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.resource.entity.ResourceEntity;
import com.edumind.resource.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 教学资源 DAO
 */
@Repository
@RequiredArgsConstructor
public class ResourceDao {

    private final ResourceMapper resourceMapper;

    public ResourceEntity findById(Long id) {
        return resourceMapper.selectById(id);
    }

    public List<ResourceEntity> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return resourceMapper.selectBatchIds(ids);
    }

    public List<ResourceEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return resourceMapper.selectList(
                new LambdaQueryWrapper<ResourceEntity>()
                        .eq(ResourceEntity::getCourseId, courseId)
                        .orderByDesc(ResourceEntity::getCreateTime)
        );
    }

    public List<ResourceEntity> findByCourseAndChapter(Long courseId, Long chapterId, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<ResourceEntity>()
                .eq(ResourceEntity::getCourseId, courseId)
                .eq(chapterId != null, ResourceEntity::getChapterId, chapterId)
                .orderByDesc(ResourceEntity::getCreateTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return resourceMapper.selectList(wrapper);
    }
}
