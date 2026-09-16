package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.CourseLearningObjectiveEntity;
import com.edumind.course.mapper.CourseLearningObjectiveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseLearningObjectiveDao {

    private final CourseLearningObjectiveMapper mapper;

    public List<CourseLearningObjectiveEntity> listByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<CourseLearningObjectiveEntity>()
                .eq(CourseLearningObjectiveEntity::getCourseId, courseId)
                .orderByAsc(CourseLearningObjectiveEntity::getSortOrder)
                .orderByAsc(CourseLearningObjectiveEntity::getId));
    }

    public int deleteByCourseId(Long courseId) {
        if (courseId == null) {
            return 0;
        }
        return mapper.delete(new LambdaQueryWrapper<CourseLearningObjectiveEntity>()
                .eq(CourseLearningObjectiveEntity::getCourseId, courseId));
    }

    public int insert(CourseLearningObjectiveEntity entity) {
        return mapper.insert(entity);
    }
}
