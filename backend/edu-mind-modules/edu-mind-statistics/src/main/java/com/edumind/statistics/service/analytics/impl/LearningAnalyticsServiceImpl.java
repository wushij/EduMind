package com.edumind.statistics.service.analytics.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningAnalyticsServiceImpl implements LearningAnalyticsService {

    private final LearningRecordDao learningRecordDao;
    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final SubmissionQueryApi submissionQueryApi;
    private final AiAuditQueryApi aiAuditQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final com.edumind.statistics.dao.CourseStatisticsDao courseStatisticsDao;

    @Override
    public LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId) {
        LearningAnalyticsVO vo = new LearningAnalyticsVO();
        vo.setCourseId(courseId);
        LocalDateTime since = resolveSince(range);
        LocalDate startDate = since != null ? since.toLocalDate() : LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        // 1. 优先读取 course_statistics 预聚合表 (PRD §34 高性能报表查询)
        List<com.edumind.statistics.entity.CourseStatisticsEntity> preAggStats = courseId != null
                ? courseStatisticsDao.listByCourseAndDateRange(courseId, startDate, endDate)
                : java.util.Collections.emptyList();

        Map<LocalDate, com.edumind.statistics.entity.CourseStatisticsEntity> statByDate = preAggStats.stream()
                .collect(Collectors.toMap(com.edumind.statistics.entity.CourseStatisticsEntity::getStatDate, e -> e, (a, b) -> a));

        List<LearningRecordEntity> records = learningRecordDao.listByCourseSince(courseId, since);
        Set<Long> students = records.stream().map(LearningRecordEntity::getStudentId).collect(Collectors.toCollection(HashSet::new));
        vo.setStudentCount(students.size());

        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        vo.setCompletionRate(submissionStats.getAvgSubmissionRate() != null
                ? submissionStats.getAvgSubmissionRate() / 100.0 : 0);
        vo.setAvgScore(submissionStats.getAvgScore());

        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0).sum();
        vo.setAvgStudyMinutes(students.isEmpty() ? 0 : totalMinutes / Math.max(1, students.size()));

        List<KnowledgeMasteryEntity> masteries = knowledgeMasteryDao.listByCourse(courseId);
        double masteryAvg = masteries.isEmpty() ? 0
                : masteries.stream().mapToDouble(m -> m.getMasteryScore().doubleValue()).average().orElse(0);
        vo.setKnowledgeMasteryAvg(masteryAvg);

        if (courseId != null) {
            long courseCalls = aiAuditQueryApi.countCallsByCourse(courseId, since);
            if (courseCalls > 0) {
                vo.setAiUsageCount((int) courseCalls);
            } else {
                List<Long> kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                        .map(KnowledgeBaseVO::getId)
                        .collect(Collectors.toList());
                vo.setAiUsageCount((int) aiAuditQueryApi.countCallsByKnowledgeBases(kbIds));
            }
        } else {
            vo.setAiUsageCount((int) aiAuditQueryApi.countTotalCalls());
        }

        // 如果存在预聚合快照，更新融合指标
        if (!preAggStats.isEmpty()) {
            vo.setAggregated(true);
            com.edumind.statistics.entity.CourseStatisticsEntity latest = preAggStats.get(preAggStats.size() - 1);
            if (vo.getStudentCount() == 0 && latest.getStudentCount() != null) {
                vo.setStudentCount(latest.getStudentCount());
            }
            if (vo.getAvgScore() == null && latest.getAvgScore() != null) {
                vo.setAvgScore(latest.getAvgScore().doubleValue());
            }
            if (vo.getKnowledgeMasteryAvg() == 0 && latest.getMasteryAvg() != null) {
                vo.setKnowledgeMasteryAvg(latest.getMasteryAvg().doubleValue());
            }
        }

        buildTrends(vo, records, statByDate, since);
        if (vo.getAggregated() == null) {
            vo.setAggregated(false);
        }
        return vo;
    }

    private void buildTrends(LearningAnalyticsVO vo,
                            List<LearningRecordEntity> records,
                            Map<LocalDate, com.edumind.statistics.entity.CourseStatisticsEntity> statByDate,
                            LocalDateTime since) {
        Map<LocalDate, Set<Long>> dailyUsers = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreateTime().toLocalDate(),
                        Collectors.mapping(LearningRecordEntity::getStudentId, Collectors.toSet())));
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate start = since != null ? since.toLocalDate() : LocalDate.now().minusDays(7);
        for (LocalDate d = start; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            LearningAnalyticsVO.TrendPoint tp = new LearningAnalyticsVO.TrendPoint();
            tp.setDate(d.format(fmt));

            LearningAnalyticsVO.ScoreTrendPoint sp = new LearningAnalyticsVO.ScoreTrendPoint();
            sp.setDate(d.format(fmt));

            // 优先读取日聚合表数据
            if (statByDate.containsKey(d)) {
                com.edumind.statistics.entity.CourseStatisticsEntity stat = statByDate.get(d);
                tp.setActiveUsers(stat.getStudentCount() != null ? stat.getStudentCount() : 0);
                sp.setAvgScore(stat.getAvgScore() != null ? stat.getAvgScore().doubleValue() : vo.getAvgScore());
            } else {
                tp.setActiveUsers(dailyUsers.getOrDefault(d, Set.of()).size());
                sp.setAvgScore(vo.getAvgScore());
            }
            vo.getTrends().getLearning().add(tp);
            vo.getTrends().getScore().add(sp);
        }
    }

    private LocalDateTime resolveSince(String range) {
        if ("30d".equals(range)) {
            return LocalDateTime.now().minusDays(30);
        }
        if ("term".equals(range)) {
            return LocalDateTime.now().minusDays(90);
        }
        return LocalDateTime.now().minusDays(7);
    }
}
