package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * 批量按学生汇总某课程的学习时长（单条 SQL 聚合）。
     *
     * <p>用于学生学情榜单等"课程 × 全部学生"场景：逐学生调用
     * {@link #getTotalDuration(Long, Long)} 会把每个学生的明细行全查回来再内存求和，
     * 50 人班级就是 50 次查询 + 大量无用行传输。</p>
     *
     * <p>口径与 {@link #getTotalDuration(Long, Long)} 完全一致：不限定时间范围，
     * duration_minutes 为 NULL 的记录按 0 计入，未出现的学生不出现在结果里（由调用方取 0）。</p>
     *
     * @param courseId   课程 ID
     * @param studentIds 学生 ID 集合
     * @return studentId -> 学习总分钟数
     */
    public Map<Long, Integer> sumDurationByCourseStudents(Long courseId, Collection<Long> studentIds) {
        Set<Long> distinctStudentIds = studentIds == null
                ? Collections.emptySet()
                : studentIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (courseId == null || distinctStudentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = learningRecordMapper.selectMaps(
                new QueryWrapper<LearningRecordEntity>()
                        .select("student_id AS studentId", "COALESCE(SUM(duration_minutes), 0) AS totalMinutes")
                        .eq("course_id", courseId)
                        .in("student_id", distinctStudentIds)
                        .groupBy("student_id"));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> totals = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long studentId = toLongKey(row.get("studentId"));
            if (studentId == null) {
                continue;
            }
            Object total = row.get("totalMinutes");
            totals.put(studentId, total instanceof Number totalValue ? totalValue.intValue() : 0);
        }
        return totals;
    }

    /**
     * 聚合结果中的主键转换：不同 JDBC 驱动可能返回 Long / BigInteger / String，
     * 统一做宽松解析，避免因类型断言过严而静默丢数据。
     */
    private static Long toLongKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
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
