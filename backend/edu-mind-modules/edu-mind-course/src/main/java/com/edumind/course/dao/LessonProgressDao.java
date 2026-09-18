package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.LessonProgressEntity;
import com.edumind.course.mapper.LessonProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LessonProgressDao {

    private final LessonProgressMapper mapper;

    public LessonProgressEntity findByStudentAndLesson(Long studentId, Long lessonChapterId) {
        if (studentId == null || lessonChapterId == null) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<LessonProgressEntity>()
                .eq(LessonProgressEntity::getStudentId, studentId)
                .eq(LessonProgressEntity::getLessonChapterId, lessonChapterId)
                .last("LIMIT 1"));
    }

    public List<LessonProgressEntity> listByCourseAndStudent(Long courseId, Long studentId) {
        if (courseId == null || studentId == null) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<LessonProgressEntity>()
                .eq(LessonProgressEntity::getCourseId, courseId)
                .eq(LessonProgressEntity::getStudentId, studentId));
    }

    public Map<Long, LessonProgressEntity> mapByStudentAndLessons(Long studentId, List<Long> lessonIds) {
        if (studentId == null || lessonIds == null || lessonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapper.selectList(new LambdaQueryWrapper<LessonProgressEntity>()
                        .eq(LessonProgressEntity::getStudentId, studentId)
                        .in(LessonProgressEntity::getLessonChapterId, lessonIds))
                .stream()
                .collect(Collectors.toMap(LessonProgressEntity::getLessonChapterId, e -> e, (a, b) -> a));
    }

    public int insert(LessonProgressEntity entity) {
        return mapper.insert(entity);
    }

    public int updateById(LessonProgressEntity entity) {
        return mapper.updateById(entity);
    }

    public long countCompletedLessons(Long courseId, Long studentId, List<Long> lessonIds) {
        if (courseId == null || studentId == null || lessonIds == null || lessonIds.isEmpty()) {
            return 0;
        }
        return mapper.selectCount(new LambdaQueryWrapper<LessonProgressEntity>()
                .eq(LessonProgressEntity::getCourseId, courseId)
                .eq(LessonProgressEntity::getStudentId, studentId)
                .in(LessonProgressEntity::getLessonChapterId, lessonIds)
                .eq(LessonProgressEntity::getStatus, "COMPLETED"));
    }
}
