package com.edumind.statistics.service.analytics.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.CourseStatisticsDao;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.learning.AdaptivePathService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.analytics.StudentLearningItemVO;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningAnalyticsServiceImpl implements LearningAnalyticsService {

    private final LearningRecordDao learningRecordDao;
    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final CourseStatisticsDao courseStatisticsDao;
    private final SubmissionQueryApi submissionQueryApi;
    private final AiAuditQueryApi aiAuditQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final UserQueryApi userQueryApi;
    private final OrganizationQueryApi organizationQueryApi;
    private final QuestionQueryApi questionQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final AdaptivePathService adaptivePathService;

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
                : Collections.emptyList();

        Map<LocalDate, com.edumind.statistics.entity.CourseStatisticsEntity> statByDate = preAggStats.stream()
                .collect(Collectors.toMap(com.edumind.statistics.entity.CourseStatisticsEntity::getStatDate, e -> e, (a, b) -> a));

        List<LearningRecordEntity> records = courseId != null ? learningRecordDao.listByCourseSince(courseId, since) : Collections.emptyList();
        List<Long> enrolledStudentIds = courseId != null
                ? courseQueryApi.listStudentUserIdsByCourseId(courseId)
                : Collections.emptyList();
        vo.setStudentCount(enrolledStudentIds.size());

        SubmissionStatsVO submissionStats = courseId != null ? submissionQueryApi.getCourseSubmissionStats(courseId) : new SubmissionStatsVO();
        vo.setCompletionRate(submissionStats.getAvgSubmissionRate() != null
                ? submissionStats.getAvgSubmissionRate() / 100.0 : 0);
        vo.setAvgScore(submissionStats.getAvgScore() != null ? submissionStats.getAvgScore() : 0);

        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0).sum();
        vo.setAvgStudyMinutes(enrolledStudentIds.isEmpty() ? 0 : totalMinutes / Math.max(1, enrolledStudentIds.size()));

        List<KnowledgeMasteryEntity> masteries = courseId != null ? knowledgeMasteryDao.listByCourse(courseId) : Collections.emptyList();
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
            if (latest.getAvgScore() != null) {
                vo.setAvgScore(latest.getAvgScore().doubleValue());
            }
            if (latest.getMasteryAvg() != null) {
                vo.setKnowledgeMasteryAvg(latest.getMasteryAvg().doubleValue());
            }
        }

        buildTrends(vo, records, statByDate, since);
        if (vo.getAggregated() == null) {
            vo.setAggregated(false);
        }

        // 组装班级选课学生学情明细列表 (供班级整体分析学生榜单)
        vo.setStudents(buildStudentRoster(courseId, vo, submissionStats));

        return vo;
    }

    private List<StudentLearningItemVO> buildStudentRoster(Long courseId, LearningAnalyticsVO vo, SubmissionStatsVO submissionStats) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        List<Long> studentIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
        if (studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SubmissionStatsVO.StudentScoreVO> scoreMap = submissionStats.getStudentScores() != null
                ? submissionStats.getStudentScores().stream()
                .collect(Collectors.toMap(SubmissionStatsVO.StudentScoreVO::getStudentId, s -> s, (a, b) -> a))
                : Collections.emptyMap();

        List<StudentLearningItemVO> list = new ArrayList<>();
        for (Long sId : studentIds) {
            UserBriefVO user = userQueryApi.getUserById(sId);
            MemberOrgBriefVO orgBrief = null;
            try {
                orgBrief = organizationQueryApi.getPrimaryClassByUserId(null, sId);
            } catch (Exception ignored) {}

            StudentLearningItemVO item = new StudentLearningItemVO();
            item.setStudentId(sId);
            item.setUsername(user != null ? user.getUsername() : "student_" + sId);
            item.setRealName(user != null && user.getRealName() != null ? user.getRealName() : "学员 " + sId);
            if (orgBrief != null && orgBrief.getMemberNo() != null && !orgBrief.getMemberNo().isBlank()) {
                item.setStudentNo(orgBrief.getMemberNo());
            } else {
                item.setStudentNo("STU-" + String.format("%04d", sId));
            }
            item.setAvatar(user != null ? user.getAvatar() : null);

            item.setStudyMinutes(learningRecordDao.getTotalDuration(courseId, sId));

            SubmissionStatsVO.StudentScoreVO sScore = scoreMap.get(sId);
            double avgScore = sScore != null && sScore.getAvgScore() != null ? sScore.getAvgScore() : 0;
            item.setAvgScore(Math.round(avgScore * 10.0) / 10.0);

            double subRate = 0;
            if (sScore != null && sScore.getSubmissionCount() != null
                    && submissionStats.getTotalAssignments() != null && submissionStats.getTotalAssignments() > 0) {
                subRate = Math.min(1.0, (double) sScore.getSubmissionCount() / submissionStats.getTotalAssignments()) * 100;
            }
            item.setSubmissionRate(Math.round(subRate * 10.0) / 10.0);

            List<KnowledgeMasteryEntity> sMastery = knowledgeMasteryDao.listByCourseAndStudent(courseId, sId);
            double mAvg = sMastery.isEmpty()
                    ? 0
                    : sMastery.stream().mapToDouble(m -> m.getMasteryScore().doubleValue()).average().orElse(0);
            item.setMasteryScore(Math.round(mAvg * 1000.0) / 10.0);

            long wrongCount = wrongQuestionRecordDao.countByStudentAndCourse(sId, courseId);
            item.setWrongCount((int) wrongCount);

            item.setAiUsageCount(0);

            if (item.getAvgScore() >= 85 && item.getMasteryScore() >= 80) {
                item.setStatus("EXCELLENT");
            } else if (item.getAvgScore() >= 70 && item.getMasteryScore() >= 65) {
                item.setStatus("GOOD");
            } else if (item.getAvgScore() < 60 || item.getMasteryScore() < 60) {
                item.setStatus("RISK");
            } else {
                item.setStatus("WARNING");
            }
            list.add(item);
        }
        return list;
    }

    @Override
    public StudentPortraitVO getStudentPortrait(Long courseId, Long studentId) {
        if (courseId == null || studentId == null) {
            throw new BusinessException("课程或学员参数无效");
        }
        if (!courseQueryApi.isCourseMember(courseId, studentId)) {
            throw new BusinessException("该学员未加入本课程，无法查看学情画像");
        }

        StudentPortraitVO vo = new StudentPortraitVO();
        List<Long> enrolledIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);

        // 1. 学生基本资料与上下文
        StudentPortraitVO.StudentInfoVO info = vo.getStudentInfo();
        info.setStudentId(studentId);
        UserBriefVO user = userQueryApi.getUserById(studentId);
        info.setUsername(user != null ? user.getUsername() : "student_" + studentId);
        info.setRealName(user != null && user.getRealName() != null ? user.getRealName() : "学员 " + studentId);
        info.setAvatar(user != null ? user.getAvatar() : null);

        MemberOrgBriefVO orgBrief = null;
        try {
            orgBrief = organizationQueryApi.getPrimaryClassByUserId(null, studentId);
        } catch (Exception e) {
            log.warn("Failed to retrieve primary class for student {}: {}", studentId, e.getMessage());
        }

        if (orgBrief != null && orgBrief.getMemberNo() != null && !orgBrief.getMemberNo().isBlank()) {
            info.setStudentNo(orgBrief.getMemberNo());
        } else {
            info.setStudentNo("STU-" + String.format("%04d", studentId));
        }

        if (orgBrief != null && orgBrief.getName() != null && !orgBrief.getName().isBlank()) {
            info.setClassName(orgBrief.getName());
        } else {
            info.setClassName("未分配行政班");
        }
        info.setRole("选课学员");
        learningRecordDao.listByCourseAndStudent(courseId, studentId).stream()
                .map(LearningRecordEntity::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .ifPresent(t -> info.setLastActiveTime(t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        // 2. 核心 KPI 汇总
        StudentPortraitVO.PortraitSummaryVO summary = vo.getSummary();
        summary.setTotalStudyMinutes(learningRecordDao.getTotalDuration(courseId, studentId));
        summary.setClassAvgStudyMinutes(computeClassAvgStudyMinutes(courseId, enrolledIds));

        SubmissionStatsVO submissionStats = submissionQueryApi.getCourseSubmissionStats(courseId);
        double classAvgScore = submissionStats.getAvgScore() != null ? submissionStats.getAvgScore() : 0;
        summary.setClassAvgScore(classAvgScore);

        SubmissionStatsVO.StudentScoreVO studentScore = submissionStats.getStudentScores() != null
                ? submissionStats.getStudentScores().stream()
                .filter(s -> Objects.equals(s.getStudentId(), studentId))
                .findFirst().orElse(null)
                : null;
        double personalAvgScore = studentScore != null && studentScore.getAvgScore() != null
                ? studentScore.getAvgScore() : 0;
        summary.setAvgScore(personalAvgScore);

        double subRate = 0;
        if (studentScore != null && studentScore.getSubmissionCount() != null
                && submissionStats.getTotalAssignments() != null && submissionStats.getTotalAssignments() > 0) {
            subRate = ((double) studentScore.getSubmissionCount() / submissionStats.getTotalAssignments()) * 100.0;
        }
        summary.setSubmissionRate(subRate);

        long wrongCount = wrongQuestionRecordDao.countByStudentAndCourse(studentId, courseId);
        summary.setWrongQuestionCount((int) wrongCount);
        summary.setAiUsageCount(0);
        if (personalAvgScore > 0) {
            summary.setLearningPace(personalAvgScore >= 85 ? "FAST" : (personalAvgScore >= 70 ? "STEADY" : "LAGGING"));
        }

        // 3. 知识点雷达图与知识体系画像
        KnowledgeMasteryVO masteryVO = knowledgeMasteryService.getMastery(courseId, studentId);
        StudentPortraitVO.RadarDataVO radar = vo.getRadar();
        radar.setDimensions(masteryVO.getDimensions());
        radar.setPersonalScores(masteryVO.getPersonal());
        radar.setClassAvgScores(masteryVO.getClassAvg());

        // 计算整体掌握度
        if (!masteryVO.getPersonal().isEmpty()) {
            double personalAvg = masteryVO.getPersonal().stream().mapToInt(Integer::intValue).average().orElse(0);
            summary.setOverallMastery(Math.round(personalAvg * 10.0) / 10.0);
        } else {
            summary.setOverallMastery(0.0);
        }

        // 4. 知识点列表与薄弱/已掌握考点
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, BigDecimal> studentMasteries = knowledgeMasteryDao.listByCourseAndStudent(courseId, studentId).stream()
                .collect(Collectors.toMap(KnowledgeMasteryEntity::getKnowledgePointId, KnowledgeMasteryEntity::getMasteryScore, (a, b) -> a));

        List<StudentPortraitVO.WeakPointVO> weakPoints = new ArrayList<>();
        List<StudentPortraitVO.MasteredPointVO> masteredPoints = new ArrayList<>();
        List<StudentPortraitVO.KnowledgePointMasteryItemVO> allPoints = new ArrayList<>();

        for (KnowledgePointVO pt : points) {
            BigDecimal scoreDec = studentMasteries.get(pt.getId());
            if (scoreDec == null) {
                continue;
            }
            double score = scoreDec.doubleValue();
            StudentPortraitVO.KnowledgePointMasteryItemVO item = new StudentPortraitVO.KnowledgePointMasteryItemVO();
            item.setKnowledgePointId(pt.getId());
            item.setTitle(pt.getTitle());
            item.setMasteryScore(Math.round(score * 1000.0) / 10.0);
            item.setSampleCount(0);

            if (score < 0.70) {
                item.setStatus("WEAK");
                item.setSuggestion("建议强化复习该知识点，完成 3 题变式题巩固");
                StudentPortraitVO.WeakPointVO wp = new StudentPortraitVO.WeakPointVO();
                wp.setKnowledgePointId(pt.getId());
                wp.setTitle(pt.getTitle());
                wp.setMastery(Math.round(score * 1000.0) / 10.0);
                wp.setSuggestion("建议回顾大纲章节精讲视频并进行定向测验");
                weakPoints.add(wp);
            } else if (score >= 0.85) {
                item.setStatus("MASTERED");
                item.setSuggestion("掌握牢固，可作为高阶变式拔高训练");
                StudentPortraitVO.MasteredPointVO mp = new StudentPortraitVO.MasteredPointVO();
                mp.setKnowledgePointId(pt.getId());
                mp.setTitle(pt.getTitle());
                mp.setMastery(Math.round(score * 1000.0) / 10.0);
                masteredPoints.add(mp);
            } else {
                item.setStatus("LEARNING");
                item.setSuggestion("理解良好，保持持续演练");
            }
            allPoints.add(item);
        }
        vo.setKnowledgePoints(allPoints);
        vo.setWeakPoints(weakPoints);
        vo.setMasteredPoints(masteredPoints);

        // 5. 个人错题归因明细
        List<WrongQuestionRecordEntity> wrongRecords = wrongQuestionRecordDao.listByStudentAndCourse(studentId, courseId, 5);
        List<StudentPortraitVO.StudentWrongItemVO> wrongItems = new ArrayList<>();
        Map<Long, String> kpTitleMap = points.stream().collect(Collectors.toMap(KnowledgePointVO::getId, KnowledgePointVO::getTitle, (a, b) -> a));

        for (WrongQuestionRecordEntity wr : wrongRecords) {
            StudentPortraitVO.StudentWrongItemVO wi = new StudentPortraitVO.StudentWrongItemVO();
            wi.setRecordId(wr.getId());
            wi.setQuestionId(wr.getQuestionId());
            wi.setKnowledgePointId(wr.getKnowledgePointId());
            wi.setKnowledgePointTitle(kpTitleMap.getOrDefault(wr.getKnowledgePointId(), "核心考点 #" + wr.getKnowledgePointId()));
            wi.setErrorTypes(wr.getErrorTypes() != null ? wr.getErrorTypes() : "CONCEPT");
            wi.setDiagnosis(wr.getDiagnosis());
            wi.setWrongCount(wr.getWrongCount() != null ? wr.getWrongCount() : 1);
            wi.setCreateTime(wr.getCreateTime() != null ? wr.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : LocalDate.now().toString());

            if (wr.getQuestionId() != null) {
                QuestionVO q = questionQueryApi.getQuestionById(wr.getQuestionId());
                wi.setQuestionStem(q != null ? q.getStem() : "题目 #" + wr.getQuestionId());
            } else {
                wi.setQuestionStem("题目 #" + (wr.getId() != null ? wr.getId() : 1001));
            }
            wrongItems.add(wi);
        }
        vo.setWrongQuestions(wrongItems);

        // 6. 自适应推荐学习周计划
        LearningPathVO adaptivePath = adaptivePathService.buildAdaptivePath(courseId, studentId);
        vo.setAdaptiveWeeks(adaptivePath.getWeeks());

        vo.setAiDiagnosis(null);

        return vo;
    }

    private double computeClassAvgStudyMinutes(Long courseId, List<Long> studentIds) {
        if (courseId == null || studentIds == null || studentIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Long sid : studentIds) {
            total += learningRecordDao.getTotalDuration(courseId, sid);
        }
        return total / (double) studentIds.size();
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
        if ("semester".equals(range) || "term".equals(range)) {
            return LocalDateTime.now().minusDays(90);
        }
        return LocalDateTime.now().minusDays(7);
    }
}
