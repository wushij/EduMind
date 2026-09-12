package com.edumind.statistics.job;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.statistics.dao.CourseStatisticsDao;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.CourseStatisticsEntity;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程学情日聚合定时任务 (PRD §34)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseStatisticsJob {

    private final CourseStatisticsDao courseStatisticsDao;
    private final LearningRecordDao learningRecordDao;
    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final SubmissionQueryApi submissionQueryApi;
    private final AiAuditQueryApi aiAuditQueryApi;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;

    /**
     * 每日凌晨 02:00 定时执行前一日统计聚合
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyAggregation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting CourseStatisticsJob for date: {}", yesterday);
        // 查找有学习记录的所有课程
        List<LearningRecordEntity> records = learningRecordDao.listAll();
        Set<Long> courseIds = records.stream()
                .map(LearningRecordEntity::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long courseId : courseIds) {
            try {
                aggregateCourseStat(courseId, yesterday);
            } catch (Exception ex) {
                log.error("Failed to aggregate course statistics for courseId={}: {}", courseId, ex.getMessage());
            }
        }
        log.info("Completed CourseStatisticsJob for {} courses", courseIds.size());
    }

    /**
     * 聚合指定课程在某一天的统计数据
     */
    public CourseStatisticsEntity aggregateCourseStat(Long courseId, LocalDate date) {
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // 1. 当日活跃学生数
        List<LearningRecordEntity> records = learningRecordDao.listByCourseSince(courseId, startOfDay);
        Set<Long> activeStudents = records.stream()
                .filter(r -> r.getCreateTime() == null || !r.getCreateTime().isAfter(endOfDay))
                .map(LearningRecordEntity::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        // 2. 均分
        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        double avgScore = submissionStats != null && submissionStats.getAvgScore() != null ? submissionStats.getAvgScore() : 80.0;

        // 3. 掌握度均值
        List<KnowledgeMasteryEntity> masteries = knowledgeMasteryDao.listByCourse(courseId);
        double masteryAvg = masteries.isEmpty() ? 0.75 :
                masteries.stream()
                        .mapToDouble(m -> m.getMasteryScore() != null ? m.getMasteryScore().doubleValue() : 0.0)
                        .average().orElse(0.75);

        // 4. 当日 AI 调用量
        long aiCalls = aiAuditQueryApi.countCallsByCourse(courseId, startOfDay);

        // 5. 新增错题数 (当日窗口 [startOfDay, endOfDay] 内新增入库的错题)
        int wrongCount = 0;
        try {
            wrongCount = (int) wrongQuestionRecordDao.countByCourseAndDateRange(courseId, startOfDay, endOfDay);
        } catch (Exception ignored) {
        }

        CourseStatisticsEntity entity = new CourseStatisticsEntity();
        entity.setCourseId(courseId);
        entity.setStatDate(date);
        entity.setStudentCount(Math.max(1, activeStudents.size()));
        entity.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
        entity.setMasteryAvg(BigDecimal.valueOf(masteryAvg).setScale(4, RoundingMode.HALF_UP));
        entity.setAiCallCount((int) aiCalls);
        entity.setWrongCount(wrongCount);

        courseStatisticsDao.saveOrUpdate(entity);
        log.info("Aggregated statistics for courseId={}, date={}: studentCount={}, aiCalls={}",
                courseId, date, entity.getStudentCount(), entity.getAiCallCount());
        return entity;
    }
}
