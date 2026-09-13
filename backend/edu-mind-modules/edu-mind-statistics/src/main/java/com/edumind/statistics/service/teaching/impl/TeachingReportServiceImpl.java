package com.edumind.statistics.service.teaching.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.service.teaching.TeachingReportService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import com.edumind.statistics.vo.teaching.TeachingReportVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingReportServiceImpl implements TeachingReportService {

    private final CourseQueryApi courseQueryApi;
    private final RecommendationService recommendationService;
    private final AiAuditQueryApi aiAuditQueryApi;
    private final SubmissionQueryApi submissionQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final LearningAnalyticsService learningAnalyticsService;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final com.edumind.statistics.dao.CourseStatisticsDao courseStatisticsDao;

    @Override
    public TeachingReportVO buildReport(Long courseId, String range) {
        String effectiveRange = range != null ? range : "7d";
        TeachingReportVO report = new TeachingReportVO();
        report.setCourseId(courseId);
        report.setRange(effectiveRange);
        List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
        report.setTotalChapters(chapters.size());
        List<RecommendedQuestionVO> questions = recommendationService.recommendQuestions(courseId, null, 5);
        List<RecommendedResourceVO> resources = recommendationService.recommendResources(courseId, null, 5);
        report.setRecommendedQuestions(questions.size());
        report.setRecommendedResources(resources.size());
        report.setAiCallCount(countAiCallsByCourse(courseId, effectiveRange));
        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        report.setAvgSubmissionRate(submissionStats.getAvgSubmissionRate() != null ? submissionStats.getAvgSubmissionRate() : 0);

        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, null);
        double masteryAvg = mastery.getClassAvg().isEmpty() ? 0
                : mastery.getClassAvg().stream().mapToInt(Integer::intValue).average().orElse(0) / 100.0;
        report.setKnowledgeMasteryAvg(masteryAvg);

        LearningAnalyticsVO learning = learningAnalyticsService.getLearningAnalytics(courseId, effectiveRange, null);
        if (learning.getTrends() != null && learning.getTrends().getLearning() != null) {
            report.setWeeklyActivity(learning.getTrends().getLearning().stream().map(tp -> {
                TeachingReportVO.WeeklyActivityVO item = new TeachingReportVO.WeeklyActivityVO();
                item.setDate(tp.getDate());
                item.setCount(tp.getActiveUsers());
                return item;
            }).collect(Collectors.toList()));
        }
        report.setErrorCategories(buildErrorCategories(courseId));

        Page<WrongQuestionRecordEntity> wrongPage = wrongQuestionRecordDao.pageByCourse(new Page<>(1, 5), courseId, null);
        if (!wrongPage.getRecords().isEmpty()) {
            report.setWeakPoints(wrongPage.getRecords().stream().map(w -> {
                TeachingReportVO.WeakPointVO weak = new TeachingReportVO.WeakPointVO();
                weak.setTitle("题目 #" + w.getQuestionId());
                weak.setWrongCount(w.getWrongCount());
                weak.setSuggestion(w.getDiagnosis() != null ? w.getDiagnosis() : "建议复习相关章节并完成变式练习");
                return weak;
            }).collect(Collectors.toList()));
        } else {
            report.setWeakPoints(mastery.getWeakPoints().stream().map(wp -> {
                TeachingReportVO.WeakPointVO weak = new TeachingReportVO.WeakPointVO();
                weak.setTitle(wp.getTitle());
                weak.setWrongCount(1);
                weak.setSuggestion(wp.getSuggestion());
                return weak;
            }).collect(Collectors.toList()));
        }
        return report;
    }

    private int countAiCallsByCourse(Long courseId, String range) {
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays("30d".equals(range) ? 30 : 7);
        List<com.edumind.statistics.entity.CourseStatisticsEntity> stats =
                courseStatisticsDao.listByCourseAndDateRange(courseId, startDate, endDate);
        if (!stats.isEmpty()) {
            return stats.stream()
                    .mapToInt(s -> s.getAiCallCount() != null ? s.getAiCallCount() : 0)
                    .sum();
        }
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays("30d".equals(range) ? 30 : 7);
        long direct = aiAuditQueryApi.countCallsByCourse(courseId, since);
        if (direct > 0) {
            return (int) direct;
        }
        List<Long> kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                .map(KnowledgeBaseVO::getId)
                .collect(Collectors.toList());
        return (int) aiAuditQueryApi.countCallsByKnowledgeBases(kbIds);
    }

    private List<TeachingReportVO.ErrorCategoryVO> buildErrorCategories(Long courseId) {
        Page<WrongQuestionRecordEntity> page = wrongQuestionRecordDao.pageByCourse(new Page<>(1, 200), courseId, null);
        Map<String, Integer> counts = new HashMap<>();
        for (WrongQuestionRecordEntity entity : page.getRecords()) {
            if (!StringUtils.hasText(entity.getErrorTypes())) {
                continue;
            }
            for (String type : entity.getErrorTypes().split(",")) {
                String key = type.trim();
                if (StringUtils.hasText(key)) {
                    counts.merge(key, 1, Integer::sum);
                }
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return List.of();
        }
        Map<String, String> labels = Map.of(
                "CONCEPT", "概念理解错误",
                "LOGIC", "逻辑推理错误",
                "CALC", "计算失误");
        return counts.entrySet().stream().map(e -> {
            TeachingReportVO.ErrorCategoryVO vo = new TeachingReportVO.ErrorCategoryVO();
            vo.setType(e.getKey());
            vo.setName(labels.getOrDefault(e.getKey(), e.getKey()));
            vo.setPercent((int) Math.round(e.getValue() * 100.0 / total));
            return vo;
        }).collect(Collectors.toList());
    }
}
