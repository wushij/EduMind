package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LearningRecordDao {

    private final LearningRecordMapper learningRecordMapper;

    public List<LearningRecordEntity> listByCourseSince(Long courseId, LocalDateTime since) {
        return learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecordEntity>()
                        .eq(LearningRecordEntity::getCourseId, courseId)
                        .ge(since != null, LearningRecordEntity::getCreateTime, since)
        );
    }

    public int insert(LearningRecordEntity entity) {
        return learningRecordMapper.insert(entity);
    }

    public List<LearningRecordEntity> listByCourse(Long courseId) {
        return learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecordEntity>()
                        .eq(LearningRecordEntity::getCourseId, courseId)
        );
    }

    public List<LearningRecordEntity> listByCourseAndStudent(Long courseId, Long studentId) {
        return learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecordEntity>()
                        .eq(LearningRecordEntity::getCourseId, courseId)
                        .eq(LearningRecordEntity::getStudentId, studentId)
        );
    }

    public int getTotalDuration(Long courseId, Long studentId) {
        List<LearningRecordEntity> list = listByCourseAndStudent(courseId, studentId);
        return list.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
                .sum();
    }

    public List<LearningRecordEntity> listAll() {
        return learningRecordMapper.selectList(new LambdaQueryWrapper<>());
    }
}
