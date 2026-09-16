package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.CourseInstructorProfileEntity;
import com.edumind.course.mapper.CourseInstructorProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseInstructorProfileDao {

    private final CourseInstructorProfileMapper mapper;

    public List<CourseInstructorProfileEntity> listByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<CourseInstructorProfileEntity>()
                .eq(CourseInstructorProfileEntity::getCourseId, courseId)
                .orderByDesc(CourseInstructorProfileEntity::getIsPrimary)
                .orderByAsc(CourseInstructorProfileEntity::getSortOrder)
                .orderByAsc(CourseInstructorProfileEntity::getId));
    }

    public CourseInstructorProfileEntity findByCourseIdAndUserId(Long courseId, Long userId) {
        return mapper.selectOne(new LambdaQueryWrapper<CourseInstructorProfileEntity>()
                .eq(CourseInstructorProfileEntity::getCourseId, courseId)
                .eq(CourseInstructorProfileEntity::getUserId, userId));
    }

    public int insert(CourseInstructorProfileEntity entity) {
        return mapper.insert(entity);
    }

    public int updateById(CourseInstructorProfileEntity entity) {
        return mapper.updateById(entity);
    }

    public int deleteByCourseIdAndUserId(Long courseId, Long userId) {
        return mapper.delete(new LambdaQueryWrapper<CourseInstructorProfileEntity>()
                .eq(CourseInstructorProfileEntity::getCourseId, courseId)
                .eq(CourseInstructorProfileEntity::getUserId, userId));
    }
}
