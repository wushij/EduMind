package com.edumind.statistics.service.analytics.impl;

import com.edumind.ai.api.AiAuditQueryApi;
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

    @Override
    public LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId) {
        LearningAnalyticsVO vo = new LearningAnalyticsVO();
        vo.setCourseId(courseId);
        LocalDateTime since = resolveSince(range);
        List<LearningRecordEntity> records = learningRecordDao.listByCourseSince(courseId, since);
        Set<Long> students = records.stream().map(LearningRecordEntity::getStudentId).collect(Collectors.toCollection(HashSet::new));
        vo.setStudentCount(students.size());

        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        vo.setCompletionRate(submissionStats.getAvgSubmissionRate() != null
                ? submissionStats.getAvgSubmissionRate() / 100.0 : 0);
        vo.setAvgScore(submissionStats.getAvgScore());

        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0).sum();
        vo.setAvgStudyMinutes(students.isEmpty() ? 0 : totalMinutes / students.size());

        List<KnowledgeMasteryEntity> masteries = knowledgeMasteryDao.listByCourse(courseId);
        double masteryAvg = masteries.isEmpty() ? 0
                : masteries.stream().mapToDouble(m -> m.getMasteryScore().doubleValue()).average().orElse(0);
        vo.setKnowledgeMasteryAvg(masteryAvg);

        vo.setAiUsageCount((int) aiAuditQueryApi.countTotalCalls());

        buildTrends(vo, records, since);
        return vo;
    }

    private void buildTrends(LearningAnalyticsVO vo, List<LearningRecordEntity> records, LocalDateTime since) {
        Map<LocalDate, Set<Long>> dailyUsers = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreateTime().toLocalDate(),
                        Collectors.mapping(LearningRecordEntity::getStudentId, Collectors.toSet())));
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate start = since != null ? since.toLocalDate() : LocalDate.now().minusDays(7);
        for (LocalDate d = start; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            LearningAnalyticsVO.TrendPoint tp = new LearningAnalyticsVO.TrendPoint();
            tp.setDate(d.format(fmt));
            tp.setActiveUsers(dailyUsers.getOrDefault(d, Set.of()).size());
            vo.getTrends().getLearning().add(tp);

            LearningAnalyticsVO.ScoreTrendPoint sp = new LearningAnalyticsVO.ScoreTrendPoint();
            sp.setDate(d.format(fmt));
            sp.setAvgScore(vo.getAvgScore());
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
