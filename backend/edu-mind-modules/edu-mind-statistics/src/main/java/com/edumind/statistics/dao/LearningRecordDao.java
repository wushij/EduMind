package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return sumDuration(list);
    }

    /**
     * 批量按课程汇总学习时长（单条 SQL 聚合）。
     * 逐课程调用 {@link #getTotalDuration} 会把每门课的明细行全查回来再内存求和，
     * 课程数一多就是 N 次查询 + 大量无用行传输。
     */
    public Map<Long, Integer> sumDurationByCourses(Long studentId, List<Long> courseIds) {
        if (studentId == null || courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = learningRecordMapper.selectMaps(
                new QueryWrapper<LearningRecordEntity>()
                        .select("course_id AS courseId", "COALESCE(SUM(duration_minutes), 0) AS totalMinutes")
                        .eq("student_id", studentId)
                        .in("course_id", courseIds)
                        .groupBy("course_id"));
        Map<Long, Integer> totals = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object courseId = row.get("courseId");
            if (courseId == null) {
                continue;
            }
            Object total = row.get("totalMinutes");
            totals.put(Long.valueOf(String.valueOf(courseId)), total == null ? 0 : ((Number) total).intValue());
        }
        return totals;
    }

    public int getTotalDurationSince(Long courseId, Long studentId, LocalDateTime since) {
        List<LearningRecordEntity> list = listByCourseAndStudentSince(courseId, studentId, since);
        return sumDuration(list);
    }

    public List<LearningRecordEntity> listByCourseAndStudentSince(Long courseId, Long studentId, LocalDateTime since) {
        return learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecordEntity>()
                        .eq(LearningRecordEntity::getCourseId, courseId)
                        .eq(LearningRecordEntity::getStudentId, studentId)
                        .ge(since != null, LearningRecordEntity::getCreateTime, since)
        );
    }

    private int sumDuration(List<LearningRecordEntity> list) {
        return list.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
                .sum();
    }

    public List<LearningRecordEntity> listAll() {
        return learningRecordMapper.selectList(new LambdaQueryWrapper<>());
    }
}
