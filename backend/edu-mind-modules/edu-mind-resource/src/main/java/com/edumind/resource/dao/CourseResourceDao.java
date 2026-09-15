package com.edumind.resource.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.resource.entity.CourseResourceEntity;
import com.edumind.resource.mapper.CourseResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseResourceDao {

    private final CourseResourceMapper courseResourceMapper;

    public List<CourseResourceEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResourceEntity>()
                        .eq(CourseResourceEntity::getCourseId, courseId)
                        .orderByDesc(CourseResourceEntity::getCreateTime)
        );
    }

    public Long countByCourseId(Long courseId) {
        if (courseId == null) {
            return 0L;
        }
        return courseResourceMapper.selectCount(
                new LambdaQueryWrapper<CourseResourceEntity>()
                        .eq(CourseResourceEntity::getCourseId, courseId)
        );
    }

    public CourseResourceEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return courseResourceMapper.selectById(id);
    }

    public int insert(CourseResourceEntity entity) {
        if (entity == null) {
            return 0;
        }
        return courseResourceMapper.insert(entity);
    }

    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return courseResourceMapper.deleteById(id);
    }
}
