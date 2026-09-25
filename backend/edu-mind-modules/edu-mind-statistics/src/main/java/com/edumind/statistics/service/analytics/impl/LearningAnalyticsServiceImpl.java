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
import com.edumind.statistics.enums.WrongErrorType;
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
import org.springframework.util.StringUtils;

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
        return getLearningAnalytics(courseId, range, classId, null, null);
    }

    @Override
    public LearningAnalyticsVO getLearningAnalytics(Long courseId, String range, Long classId, LocalDate customStartDate, LocalDate customEndDate) {
        LearningAnalyticsVO vo = new LearningAnalyticsVO();
        vo.setCourseId(courseId);

        LocalDate startDate;
        LocalDate endDate;
        LocalDateTime since;

        if (customStartDate != null && customEndDate != null) {
            startDate = customStartDate;
            endDate = customEndDate;
            since = customStartDate.atStartOfDay();
        } else {
            since = resolveSince(range);
            startDate = since != null ? since.toLocalDate() : LocalDate.now().minusDays(30);
            endDate = LocalDate.now();
        }

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

        // 学情口径统一：learning_record 里混有教师/管理员浏览课程产生的行为（教师预览课节同样会落库），
        // 因此所有"学生学情"指标必须先按选课学生过滤。
        // 历史实现直接用全量 records 除以选课人数，导致教师自己的操作把「人均学习时长」「日学习活跃人次」
        // 「数据更新时间」全部抬高（某课程 167 分钟里 137 分钟来自非选课账号）。
        Set<Long> enrolledStudentSet = new HashSet<>(enrolledStudentIds);
        List<LearningRecordEntity> studentRecords = records.stream()
                .filter(r -> r.getStudentId() != null && enrolledStudentSet.contains(r.getStudentId()))
                .collect(Collectors.toList());

        SubmissionStatsVO submissionStats = courseId != null ? submissionQueryApi.getCourseSubmissionStats(courseId) : new SubmissionStatsVO();
        // getCourseSubmissionStats 已在同一次答卷扫描中产出学生维度明细（studentScores），
        // courseHealth.passRate 与学生榜单均分直接复用，无需再单独发起一次全量查询。
        vo.setCompletionRate(submissionStats.getAvgSubmissionRate() != null
                ? submissionStats.getAvgSubmissionRate() / 100.0 : 0);
        vo.setAvgScore(submissionStats.getAvgScore() != null ? submissionStats.getAvgScore() : 0);

        int totalMinutes = studentRecords.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0).sum();
        vo.setAvgStudyMinutes(enrolledStudentIds.isEmpty() ? 0 : totalMinutes / Math.max(1, enrolledStudentIds.size()));

        // 知识点掌握度同样只统计选课学生：非选课账号（管理员/教师试做等）的实测记录会把班级值稀释或抬高，
        // 与逐考点明细、薄弱考点卡片使用同一口径，保证 KPI 与图表可互相验证。
        List<KnowledgeMasteryEntity> masteries = courseId != null ? knowledgeMasteryDao.listByCourse(courseId) : Collections.emptyList();
        List<KnowledgeMasteryEntity> classMasteries = masteries.stream()
                .filter(m -> m != null && m.getStudentId() != null && m.getMasteryScore() != null)
                .filter(m -> enrolledStudentSet.contains(m.getStudentId()))
                .collect(Collectors.toList());
        double masteryAvg = classMasteries.isEmpty() ? 0
                : classMasteries.stream().mapToDouble(m -> m.getMasteryScore().doubleValue()).average().orElse(0);
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

        // 日学习活跃人次同样只统计选课学生，避免"教师在线"被画成"学生活跃"
        buildTrends(vo, studentRecords, statByDate, startDate, endDate);
        if (vo.getAggregated() == null) {
            vo.setAggregated(false);
        }

        // 组装班级选课学生学情明细列表 (供班级整体分析学生榜单)
        vo.setStudents(buildStudentRoster(courseId, submissionStats, since));

        // 组装课程深入分析专用维度（章节进度、健康度雷达、薄弱考点）
        buildCourseDeepInsights(vo, courseId, enrolledStudentIds, submissionStats, records);

        // 数据更新时间：取统计范围内最近一次真实"学生"学习行为，
        // 既避免用前端本地时间冒充，也避免把教师的操作时间当成班级学情更新时间。
        studentRecords.stream()
                .map(LearningRecordEntity::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .ifPresent(t -> vo.setDataUpdatedAt(t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        return vo;
    }

    private List<StudentLearningItemVO> buildStudentRoster(Long courseId, SubmissionStatsVO submissionStats, LocalDateTime since) {
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

        // 批量补全学生用户与班级信息，避免逐学生跨模块查询造成 N+1
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(studentIds);
        Map<Long, MemberOrgBriefVO> orgMap = Collections.emptyMap();
        try {
            orgMap = organizationQueryApi.mapPrimaryClassesByUserIds(null, studentIds);
        } catch (Exception ignored) {
        }
        // 四类学情指标一次性批量加载（学习时长 / 掌握度 / 错题数 / AI 用量），
        // 替代历史实现的「每个学生 4 次查询」：50 人班级由 200 次查询降为 4 次聚合查询。
        // 各指标的取值口径与舍入规则与逐学生版本严格一致，结果为空的指标按原兜底值 0 处理。
        Map<Long, Integer> studyMinutesMap = safeSumStudyMinutes(courseId, studentIds);
        Map<Long, Double> masteryAvgMap = safeLoadMasteryAvgByStudent(courseId, studentIds);
        Map<Long, Integer> wrongCountMap = safeCountWrongQuestionsByStudent(courseId, studentIds);
        Map<Long, Long> aiUsageMap = resolveStudentAiUsageMap(courseId, studentIds, since);

        List<StudentLearningItemVO> list = new ArrayList<>(studentIds.size());
        for (Long sId : studentIds) {
            UserBriefVO user = userMap.get(sId);
            MemberOrgBriefVO orgBrief = orgMap.get(sId);

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

            item.setStudyMinutes(studyMinutesMap.getOrDefault(sId, 0));

            SubmissionStatsVO.StudentScoreVO sScore = scoreMap.get(sId);
            double avgScore = sScore != null && sScore.getAvgScore() != null ? sScore.getAvgScore() : 0;
            item.setAvgScore(Math.round(avgScore * 10.0) / 10.0);

            double subRate = 0;
            if (sScore != null && sScore.getSubmissionCount() != null
                    && submissionStats.getTotalAssignments() != null && submissionStats.getTotalAssignments() > 0) {
                subRate = Math.min(1.0, (double) sScore.getSubmissionCount() / submissionStats.getTotalAssignments()) * 100;
            }
            item.setSubmissionRate(Math.round(subRate * 10.0) / 10.0);

            item.setMasteryScore(masteryAvgMap.getOrDefault(sId, 0.0));

            item.setWrongCount(wrongCountMap.getOrDefault(sId, 0));

            item.setAiUsageCount(aiUsageMap.getOrDefault(sId, 0L).intValue());

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

    /** 批量学习时长（分钟）：单次 GROUP BY 聚合，口径等同逐学生 getTotalDuration；异常时全量按 0 */
    private Map<Long, Integer> safeSumStudyMinutes(Long courseId, List<Long> studentIds) {
        try {
            return learningRecordDao.sumDurationByCourseStudents(courseId, studentIds);
        } catch (Exception e) {
            log.warn("Failed to batch sum study minutes for course {}: {}", courseId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 批量掌握度均分：一次取课程全部掌握度记录后按学生分组求平均。
     * 舍入规则与逐学生版本一致（0~1 的分值 × 100 保留 1 位小数）；无记录的学生不进入结果，取 0。
     */
    private Map<Long, Double> safeLoadMasteryAvgByStudent(Long courseId, List<Long> studentIds) {
        Map<Long, Double> result = new HashMap<>();
        try {
            Set<Long> scope = new HashSet<>(studentIds);
            Map<Long, List<KnowledgeMasteryEntity>> byStudent = knowledgeMasteryDao.listByCourse(courseId).stream()
                    .filter(m -> m.getStudentId() != null && scope.contains(m.getStudentId()))
                    .filter(m -> m.getMasteryScore() != null)
                    .collect(Collectors.groupingBy(KnowledgeMasteryEntity::getStudentId));
            byStudent.forEach((studentId, records) -> {
                double avg = records.stream()
                        .mapToDouble(m -> m.getMasteryScore().doubleValue())
                        .average()
                        .orElse(0);
                result.put(studentId, Math.round(avg * 1000.0) / 10.0);
            });
        } catch (Exception e) {
            log.warn("Failed to batch load mastery by student for course {}: {}", courseId, e.getMessage());
        }
        return result;
    }

    /** 批量错题条数：单次 GROUP BY 聚合，口径等同逐学生 countByStudentAndCourse；异常时全量按 0 */
    private Map<Long, Integer> safeCountWrongQuestionsByStudent(Long courseId, List<Long> studentIds) {
        Map<Long, Integer> result = new HashMap<>();
        try {
            wrongQuestionRecordDao.countGroupByStudentIds(courseId, studentIds)
                    .forEach((studentId, count) -> result.put(studentId, count.intValue()));
        } catch (Exception e) {
            log.warn("Failed to batch count wrong questions for course {}: {}", courseId, e.getMessage());
        }
        return result;
    }

    /**
     * 批量 AI 用量：单次 GROUP BY 聚合取每位学生的真实调用次数；
     * 沿用单学生版本的兜底语义——该生无直连调用记录时退回课程知识库维度总量。
     * 兜底值对全班一致，因此只计算一次（原实现对每个学生都算一遍）。
     */
    private Map<Long, Long> resolveStudentAiUsageMap(Long courseId, List<Long> studentIds, LocalDateTime since) {
        Map<Long, Long> result = new HashMap<>();
        if (courseId == null || studentIds.isEmpty()) {
            return result;
        }
        Map<Long, Long> batchUsage;
        try {
            batchUsage = aiAuditQueryApi.countCallsByCourseUserBatch(courseId, studentIds, since);
        } catch (Exception e) {
            log.warn("Failed to batch count AI usage for course {}: {}", courseId, e.getMessage());
            batchUsage = null;
        }
        final Map<Long, Long> directUsage = batchUsage != null ? batchUsage : Collections.emptyMap();

        boolean needFallback = studentIds.stream().anyMatch(id -> directUsage.getOrDefault(id, 0L) <= 0);
        long fallback = needFallback ? resolveCourseAiUsageFallback(courseId) : 0L;

        for (Long studentId : studentIds) {
            long direct = directUsage.getOrDefault(studentId, 0L);
            result.put(studentId, direct > 0 ? direct : fallback);
        }
        return result;
    }

    /** 课程知识库维度的 AI 调用总量，作为单学生直连记录缺失时的兜底值 */
    private long resolveCourseAiUsageFallback(Long courseId) {
        try {
            List<Long> kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                    .map(KnowledgeBaseVO::getId)
                    .collect(Collectors.toList());
            if (kbIds.isEmpty()) {
                return 0L;
            }
            return aiAuditQueryApi.countCallsByKnowledgeBases(kbIds);
        } catch (Exception e) {
            log.warn("Failed to resolve course-level AI usage fallback for course {}: {}", courseId, e.getMessage());
            return 0L;
        }
    }

    @Override
    public StudentPortraitVO getStudentPortrait(Long courseId, Long studentId, String range) {
        if (courseId == null || studentId == null) {
            throw new BusinessException("课程或学员参数无效");
        }
        if (!courseQueryApi.isCourseMember(courseId, studentId)) {
            throw new BusinessException("该学员未加入本课程，无法查看学情画像");
        }

        LocalDateTime since = resolveSince(range);
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
        // 该学员的学习记录只加载一次：最近活跃时间、累计时长、区间时长都由这一份数据算出，
        // 替代原先三次各自全量查询同一张表
        List<LearningRecordEntity> studentRecords = learningRecordDao.listByCourseAndStudent(courseId, studentId);
        studentRecords.stream()
                .map(LearningRecordEntity::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .ifPresent(t -> info.setLastActiveTime(t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        // 2. 核心 KPI 汇总
        StudentPortraitVO.PortraitSummaryVO summary = vo.getSummary();
        summary.setTotalStudyMinutesAllTime(sumDurations(studentRecords, null));
        summary.setTotalStudyMinutes(sumDurations(studentRecords, since));
        summary.setClassAvgStudyMinutes(computeClassAvgStudyMinutes(courseId, enrolledIds, since));

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
        summary.setAiUsageCount((int) resolveStudentAiUsageCount(courseId, studentId, since));
        if (personalAvgScore > 0) {
            summary.setLearningPace(personalAvgScore >= 85 ? "FAST" : (personalAvgScore >= 70 ? "STEADY" : "LAGGING"));
        }

        // 3. 知识点雷达图与知识体系画像
        KnowledgeMasteryVO masteryVO = knowledgeMasteryService.getMastery(courseId, studentId);
        StudentPortraitVO.RadarDataVO radar = vo.getRadar();
        radar.setDimensions(masteryVO.getDimensions());
        radar.setPersonalScores(masteryVO.getPersonal());
        radar.setClassAvgScores(masteryVO.getClassAvg());
        compactRadarForDisplay(radar);

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
            // 展示层统一清洗机器标记，避免「主要失分诱因代码：[ ]」透给学生
            String cleanDiagnosis = WrongErrorType.stripTypeMarker(wr.getDiagnosis());
            wi.setDiagnosis(StringUtils.hasText(cleanDiagnosis) ? cleanDiagnosis : null);
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
        // 复用第 3 步已加载的 masteryVO：路径编排与逐周取题都基于同一份掌握度，
        // 不再各自重复执行「全班 × 全考点」矩阵计算
        LearningPathVO adaptivePath = adaptivePathService.buildAdaptivePath(courseId, studentId, masteryVO);
        vo.setAdaptiveWeeks(adaptivePath.getWeeks());

        vo.setAiDiagnosis(null);

        return vo;
    }

    /**
     * 汇总学习时长（分钟）。
     *
     * <p>口径与 DAO 保持一致：{@code since} 为 null 时统计全部记录；非 null 时对应
     * {@code create_time >= since}，即 create_time 为空的记录不纳入区间统计
     * （SQL 中 NULL 参与比较不会命中条件），duration_minutes 为空按 0 计。</p>
     */
    private static int sumDurations(List<LearningRecordEntity> records, LocalDateTime since) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        return records.stream()
                .filter(r -> since == null || (r.getCreateTime() != null && !r.getCreateTime().isBefore(since)))
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
                .sum();
    }

    private double computeClassAvgStudyMinutes(Long courseId, List<Long> studentIds, LocalDateTime since) {
        if (courseId == null || studentIds == null || studentIds.isEmpty()) {
            return 0;
        }
        Set<Long> studentSet = new HashSet<>(studentIds);
        List<LearningRecordEntity> records = since != null
                ? learningRecordDao.listByCourseSince(courseId, since)
                : learningRecordDao.listByCourse(courseId);
        int total = records.stream()
                .filter(r -> r.getStudentId() != null && studentSet.contains(r.getStudentId()))
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
                .sum();
        return total / (double) studentIds.size();
    }

    /**
     * 单个学生的 AI 用量（学情画像等单人场景使用）。
     * 多人榜单请走 {@link #resolveStudentAiUsageMap} 的批量聚合，避免 N+1。
     */
    private long resolveStudentAiUsageCount(Long courseId, Long studentId, LocalDateTime since) {
        if (courseId == null || studentId == null) {
            return 0L;
        }
        long direct = aiAuditQueryApi.countCallsByCourseAndUser(courseId, studentId, since);
        if (direct > 0) {
            return direct;
        }
        return resolveCourseAiUsageFallback(courseId);
    }

    private static final int RADAR_MAX_DIMENSIONS = 8;
    private static final String RADAR_OTHER_LABEL = "其他考点";

    /**
     * 知识点过多时压缩雷达维度，避免长标题占满图表。
     */
    private void compactRadarForDisplay(StudentPortraitVO.RadarDataVO radar) {
        if (radar == null || radar.getDimensions() == null || radar.getDimensions().size() <= RADAR_MAX_DIMENSIONS) {
            return;
        }
        List<String> dims = radar.getDimensions();
        List<Integer> personal = radar.getPersonalScores();
        List<Integer> classAvg = radar.getClassAvgScores();
        int n = dims.size();

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        indices.sort((a, b) -> {
            int pa = a < personal.size() ? personal.get(a) : 0;
            int pb = b < personal.size() ? personal.get(b) : 0;
            boolean testedA = pa > 0;
            boolean testedB = pb > 0;
            if (testedA != testedB) {
                return testedB ? 1 : -1;
            }
            return Integer.compare(pa, pb);
        });

        Set<Integer> picked = new LinkedHashSet<>();
        for (int idx : indices) {
            if (picked.size() >= RADAR_MAX_DIMENSIONS) {
                break;
            }
            picked.add(idx);
        }

        List<String> newDims = new ArrayList<>();
        List<Integer> newPersonal = new ArrayList<>();
        List<Integer> newClass = new ArrayList<>();
        double otherPersonalSum = 0;
        double otherClassSum = 0;
        int otherCount = 0;

        for (int i = 0; i < n; i++) {
            int p = i < personal.size() ? personal.get(i) : 0;
            int c = i < classAvg.size() ? classAvg.get(i) : 0;
            if (picked.contains(i)) {
                newDims.add(dims.get(i));
                newPersonal.add(p);
                newClass.add(c);
            } else {
                otherPersonalSum += p;
                otherClassSum += c;
                otherCount++;
            }
        }
        if (otherCount > 0) {
            newDims.add(RADAR_OTHER_LABEL);
            newPersonal.add((int) Math.round(otherPersonalSum / otherCount));
            newClass.add((int) Math.round(otherClassSum / otherCount));
        }
        radar.setDimensions(newDims);
        radar.setPersonalScores(newPersonal);
        radar.setClassAvgScores(newClass);
    }

    /**
     * 构建学习活跃度与成绩演进趋势。
     *
     * <p>全部指标来自真实数据聚合，某天没有数据就如实留空，不再用正弦波拟合或
     * "选课人数 × 系数"模拟：</p>
     * <ul>
     *   <li>活跃人数：course_statistics 日快照 -> learning_record 去重学生数 -> 0</li>
     *   <li>班级均分：课程作业提交按提交日真实聚合（与"班级平均分"KPI 同源同口径）</li>
     *   <li>全校对照：全平台作业提交按提交日真实聚合</li>
     * </ul>
     */
    private void buildTrends(LearningAnalyticsVO vo,
                            List<LearningRecordEntity> records,
                            Map<LocalDate, com.edumind.statistics.entity.CourseStatisticsEntity> statByDate,
                            LocalDate startDate,
                            LocalDate endDate) {
        Map<LocalDate, Set<Long>> dailyUsers = records.stream()
                .filter(r -> r.getCreateTime() != null && r.getStudentId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreateTime().toLocalDate(),
                        Collectors.mapping(LearningRecordEntity::getStudentId, Collectors.toSet())));

        Map<LocalDate, Double> classDailyScore = toDailyScoreMap(
                submissionQueryApi.listDailyAvgScores(vo.getCourseId(), startDate, endDate));
        Map<LocalDate, Double> schoolDailyScore = toDailyScoreMap(
                submissionQueryApi.listDailyAvgScores(null, startDate, endDate));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LearningAnalyticsVO.TrendPoint tp = new LearningAnalyticsVO.TrendPoint();
            tp.setDate(d.format(fmt));
            com.edumind.statistics.entity.CourseStatisticsEntity stat = statByDate.get(d);
            if (stat != null && stat.getStudentCount() != null && stat.getStudentCount() > 0) {
                tp.setActiveUsers(stat.getStudentCount());
            } else {
                tp.setActiveUsers(dailyUsers.getOrDefault(d, Collections.emptySet()).size());
            }

            LearningAnalyticsVO.ScoreTrendPoint sp = new LearningAnalyticsVO.ScoreTrendPoint();
            sp.setDate(d.format(fmt));
            sp.setAvgScore(classDailyScore.get(d));
            sp.setSchoolAvgScore(schoolDailyScore.get(d));
            vo.getTrends().getLearning().add(tp);
            vo.getTrends().getScore().add(sp);
        }
    }

    /** 把按日聚合结果转换为 date -> 均分映射，便于趋势逐日取值 */
    private Map<LocalDate, Double> toDailyScoreMap(List<SubmissionStatsVO.DailyScoreVO> dailyScores) {
        Map<LocalDate, Double> map = new HashMap<>();
        if (dailyScores == null) {
            return map;
        }
        for (SubmissionStatsVO.DailyScoreVO item : dailyScores) {
            if (item == null || item.getDate() == null || item.getAvgScore() == null) {
                continue;
            }
            try {
                map.put(LocalDate.parse(item.getDate()), item.getAvgScore());
            } catch (Exception e) {
                log.warn("Skip malformed daily score date: {}", item.getDate());
            }
        }
        return map;
    }

    /**
     * 组装课程深入分析维度（章节学习覆盖、课程质效 5 维、薄弱考点）。
     *
     * <p>所有分项均来自真实数据：章节维度把 learning_record.chapter_id 按顶层章上卷；
     * 质效指数取真实的章节覆盖、提交率、活跃学生、及格率与 AI 渗透率；
     * 薄弱考点只保留有真实掌握度记录的知识点。历史实现这里是按章节序号递减的公式、
     * 硬编码 92 分大纲覆盖与编造的错题数，属于伪数据。</p>
     */
    private void buildCourseDeepInsights(LearningAnalyticsVO vo,
                                         Long courseId,
                                         List<Long> enrolledStudentIds,
                                         SubmissionStatsVO submissionStats,
                                         List<LearningRecordEntity> records) {
        if (courseId == null) {
            return;
        }
        int totalStudents = Math.max(1, enrolledStudentIds.size());
        Set<Long> enrolledSet = new HashSet<>(enrolledStudentIds);

        // 1. 章节维度：learning_record 可能挂在顶层章或微课节，统一上卷到顶层章
        List<com.edumind.course.vo.chapter.ChapterTreeVO> chapters = Collections.emptyList();
        try {
            List<com.edumind.course.vo.chapter.ChapterTreeVO> loaded = courseQueryApi.listChaptersByCourseId(courseId);
            if (loaded != null) {
                chapters = loaded;
            }
        } catch (Exception e) {
            log.warn("Failed to load chapters for course {}: {}", courseId, e.getMessage());
        }

        Map<Long, Set<Long>> chapterScope = new LinkedHashMap<>();
        for (com.edumind.course.vo.chapter.ChapterTreeVO top : chapters) {
            Set<Long> scope = new LinkedHashSet<>();
            scope.add(top.getId());
            if (top.getChildren() != null) {
                for (com.edumind.course.vo.chapter.ChapterTreeVO child : top.getChildren()) {
                    scope.add(child.getId());
                }
            }
            chapterScope.put(top.getId(), scope);
        }

        Map<Long, Set<Long>> studentsByChapter = new HashMap<>();
        Map<Long, Integer> minutesByChapter = new HashMap<>();
        for (LearningRecordEntity r : records) {
            if (r.getChapterId() == null || r.getStudentId() == null || !enrolledSet.contains(r.getStudentId())) {
                continue;
            }
            studentsByChapter.computeIfAbsent(r.getChapterId(), k -> new HashSet<>()).add(r.getStudentId());
            minutesByChapter.merge(r.getChapterId(),
                    r.getDurationMinutes() != null ? r.getDurationMinutes() : 0, Integer::sum);
        }

        Map<Long, Double> chapterMastery = buildChapterMastery(courseId, enrolledSet);

        int sortIndex = 0;
        for (com.edumind.course.vo.chapter.ChapterTreeVO top : chapters) {
            sortIndex++;
            Set<Long> scope = chapterScope.getOrDefault(top.getId(), Collections.singleton(top.getId()));
            Set<Long> chapterStudents = new HashSet<>();
            int chapterMinutes = 0;
            for (Long chapterId : scope) {
                chapterStudents.addAll(studentsByChapter.getOrDefault(chapterId, Collections.emptySet()));
                chapterMinutes += minutesByChapter.getOrDefault(chapterId, 0);
            }
            LearningAnalyticsVO.ChapterProgressVO cp = new LearningAnalyticsVO.ChapterProgressVO();
            cp.setChapterId(top.getId());
            cp.setChapterTitle(top.getTitle());
            cp.setSort(top.getSort() != null ? top.getSort() : sortIndex);
            cp.setCompletionRate(Math.round(chapterStudents.size() * 1000.0 / totalStudents) / 10.0);
            cp.setStudentCount(chapterStudents.size());
            cp.setAvgStudyMinutes(chapterStudents.isEmpty() ? 0 : chapterMinutes / chapterStudents.size());
            cp.setAvgScore(chapterMastery.get(top.getId()));
            vo.getChapterProgressList().add(cp);
        }

        // 2. 课程 5 维质效：每一项都由真实数据推导，无数据记 0
        LearningAnalyticsVO.CourseHealthVO health = new LearningAnalyticsVO.CourseHealthVO();
        long touchedChapters = chapterScope.values().stream()
                .filter(scope -> scope.stream()
                        .anyMatch(id -> !studentsByChapter.getOrDefault(id, Collections.emptySet()).isEmpty()))
                .count();
        int syllabusCov = chapterScope.isEmpty() ? 0 : (int) Math.round(touchedChapters * 100.0 / chapterScope.size());
        int assignComp = (int) Math.round((vo.getCompletionRate() != null ? vo.getCompletionRate() : 0) * 100);
        Set<Long> activeStudents = records.stream()
                .map(LearningRecordEntity::getStudentId)
                .filter(Objects::nonNull)
                .filter(enrolledSet::contains)
                .collect(Collectors.toSet());
        int interaction = (int) Math.round(activeStudents.size() * 100.0 / totalStudents);
        int passRate = computePassRate(submissionStats, enrolledSet);
        int aiRate = computeAiPenetration(records, enrolledSet, totalStudents);

        health.setSyllabusCoverage(syllabusCov);
        health.setAssignmentCompletion(assignComp);
        health.setStudentInteraction(interaction);
        health.setPassRate(passRate);
        health.setAiAssistanceRate(aiRate);

        double overall = (syllabusCov * 0.2) + (assignComp * 0.25) + (interaction * 0.15) + (passRate * 0.25) + (aiRate * 0.15);
        health.setOverallScore(Math.round(overall * 10.0) / 10.0);
        health.setHealthLevel(overall >= 85 ? "EXCELLENT" : (overall >= 70 ? "GOOD" : "WARNING"));
        vo.setCourseHealth(health);

        // 3. 全量考点真实掌握度 + 高频预警薄弱考点
        buildCourseKnowledgePoints(vo, courseId, enrolledSet);
    }

    /**
     * 章节掌握度：该章关联知识点的真实平均掌握度 × 100，无数据的章节不返回。
     * 只统计选课学生的实测记录，与课程薄弱考点、概览图保持同一口径。
     */
    private Map<Long, Double> buildChapterMastery(Long courseId, Set<Long> enrolledSet) {
        Map<Long, Double> result = new HashMap<>();
        try {
            List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
            if (points == null || points.isEmpty()) {
                return result;
            }
            Map<Long, Double> masteryByKp = knowledgeMasteryDao.listByCourse(courseId).stream()
                    .filter(m -> m != null && m.getKnowledgePointId() != null && m.getMasteryScore() != null)
                    .filter(m -> m.getStudentId() != null && enrolledSet.contains(m.getStudentId()))
                    .collect(Collectors.groupingBy(KnowledgeMasteryEntity::getKnowledgePointId,
                            Collectors.averagingDouble(m -> m.getMasteryScore().doubleValue())));
            Map<Long, List<Double>> byChapter = new HashMap<>();
            for (KnowledgePointVO pt : points) {
                Double mastery = masteryByKp.get(pt.getId());
                if (pt.getChapterId() == null || mastery == null) {
                    continue;
                }
                byChapter.computeIfAbsent(pt.getChapterId(), k -> new ArrayList<>()).add(mastery * 100.0);
            }
            byChapter.forEach((chapterId, values) -> {
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                result.put(chapterId, Math.round(avg * 10.0) / 10.0);
            });
        } catch (Exception e) {
            log.warn("Failed to build chapter mastery for course {}: {}", courseId, e.getMessage());
        }
        return result;
    }

    /**
     * 测验及格率：本课程选课学生中，已批改均分达到 60 分的占比（真实值，无样本记 0）。
     *
     * <p>学生明细来自课程下全部作业的答卷，其中可能混入非选课账号（例如教务/教师试做提交），
     * 历史实现直接用该明细做分母，导致及格率被非本班学生稀释或抬高，故此处按选课学生名单过滤。</p>
     */
    private int computePassRate(SubmissionStatsVO submissionStats, Set<Long> enrolledSet) {
        List<SubmissionStatsVO.StudentScoreVO> allScores = submissionStats != null ? submissionStats.getStudentScores() : null;
        if (allScores == null || allScores.isEmpty()) {
            return 0;
        }
        List<SubmissionStatsVO.StudentScoreVO> scores = allScores.stream()
                .filter(s -> s != null && s.getStudentId() != null && enrolledSet.contains(s.getStudentId()))
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return 0;
        }
        long pass = scores.stream()
                .filter(s -> s.getAvgScore() != null && s.getAvgScore() >= 60)
                .count();
        return (int) Math.round(pass * 100.0 / scores.size());
    }

    /** AI 助学渗透率：产生过 AI 学习行为的学生数占选课学生的比例（真实值，无数据记 0） */
    private int computeAiPenetration(List<LearningRecordEntity> records, Set<Long> enrolledSet, int totalStudents) {
        Set<Long> aiStudents = records.stream()
                .filter(r -> r.getActionType() != null && r.getActionType().toUpperCase().contains("AI"))
                .map(LearningRecordEntity::getStudentId)
                .filter(Objects::nonNull)
                .filter(enrolledSet::contains)
                .collect(Collectors.toSet());
        return (int) Math.round(aiStudents.size() * 100.0 / Math.max(1, totalStudents));
    }

    /**
     * 课程考点掌握度明细与薄弱考点清单，两者都只取真实数据。
     *
     * <p>{@code courseKnowledgePoints} 覆盖课程全部考点：没有实测掌握度记录的考点 mastery 返回 null，
     * 由前端标注"未测评"，从根上杜绝"先验基准补算"把 0% 渲染成 36% 的口径矛盾；
     * {@code courseWeakPoints} 只保留掌握度低于 80 的考点，错题累计与受影响人数同样取真实值。
     * 历史实现对无掌握度数据的知识点默认按 0.68 掌握度，并按公式编造错题数与受影响人数。</p>
     *
     * <p>统计范围一律限定在课程选课学生：掌握度取选课学生的实测记录，
     * 错题累计取选课学生的 <b>wrong_count 累加值</b>（与 KnowledgeMasteryServiceImpl 的"失分累积"同口径：
     * 同一考点下多道错题记录必须累加而非只数条数），管理员/教师试做产生的记录一律不计入班级指标。</p>
     */
    private void buildCourseKnowledgePoints(LearningAnalyticsVO vo, Long courseId, Set<Long> enrolledSet) {
        try {
            List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
            if (points == null || points.isEmpty()) {
                return;
            }
            Map<Long, List<KnowledgeMasteryEntity>> masteryByKp = knowledgeMasteryDao.listByCourse(courseId).stream()
                    .filter(m -> m != null && m.getKnowledgePointId() != null && m.getMasteryScore() != null)
                    .filter(m -> m.getStudentId() != null && enrolledSet.contains(m.getStudentId()))
                    .collect(Collectors.groupingBy(KnowledgeMasteryEntity::getKnowledgePointId));

            // 错题累计：一次性取课程错题记录后按考点累加 wrong_count，
            // 既避免逐考点查询的 N+1，也保证"错题累计频次"与其它口径同源
            Map<Long, Integer> wrongTotalByKp = new HashMap<>();
            try {
                for (WrongQuestionRecordEntity record : wrongQuestionRecordDao.listByCourse(courseId)) {
                    if (record == null || record.getKnowledgePointId() == null) {
                        continue;
                    }
                    if (record.getStudentId() == null || !enrolledSet.contains(record.getStudentId())) {
                        continue;
                    }
                    wrongTotalByKp.merge(record.getKnowledgePointId(),
                            record.getWrongCount() != null ? record.getWrongCount() : 1, Integer::sum);
                }
            } catch (Exception e) {
                log.warn("Failed to aggregate wrong question counts for course {}: {}", courseId, e.getMessage());
            }

            for (KnowledgePointVO pt : points) {
                List<KnowledgeMasteryEntity> kpMasteries = masteryByKp.get(pt.getId());
                LearningAnalyticsVO.KnowledgePointMasteryVO item = new LearningAnalyticsVO.KnowledgePointMasteryVO();
                item.setKnowledgePointId(pt.getId());
                item.setTitle(pt.getTitle());
                if (kpMasteries == null || kpMasteries.isEmpty()) {
                    item.setMastery(null);
                    item.setAssessedStudentCount(0);
                    vo.getCourseKnowledgePoints().add(item);
                    continue;
                }
                double avgMastery = kpMasteries.stream()
                        .mapToDouble(m -> m.getMasteryScore().doubleValue())
                        .average().orElse(0);
                double mastery = Math.round(avgMastery * 1000.0) / 10.0;
                item.setMastery(mastery);
                item.setAssessedStudentCount((int) kpMasteries.stream()
                        .map(KnowledgeMasteryEntity::getStudentId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count());
                vo.getCourseKnowledgePoints().add(item);

                if (mastery >= 80.0) {
                    continue;
                }
                LearningAnalyticsVO.CourseWeakPointVO wp = new LearningAnalyticsVO.CourseWeakPointVO();
                wp.setKnowledgePointId(pt.getId());
                wp.setTitle(pt.getTitle());
                wp.setMastery(mastery);
                wp.setWrongCount(wrongTotalByKp.getOrDefault(pt.getId(), 0));
                wp.setAffectedStudents((int) kpMasteries.stream()
                        .filter(m -> m.getMasteryScore() != null && m.getMasteryScore().doubleValue() < 0.70)
                        .map(KnowledgeMasteryEntity::getStudentId)
                        .distinct()
                        .count());
                wp.setUrgency(mastery < 65.0 ? "HIGH" : "MEDIUM");
                vo.getCourseWeakPoints().add(wp);
            }
            vo.getCourseWeakPoints().sort(Comparator.comparing(LearningAnalyticsVO.CourseWeakPointVO::getMastery));
        } catch (Exception e) {
            log.warn("Failed to build course knowledge point mastery for course {}: {}", courseId, e.getMessage());
        }
    }

    private LocalDateTime resolveSince(String range) {
        if ("7d".equals(range)) {
            return LocalDateTime.now().minusDays(7);
        }
        if ("30d".equals(range)) {
            return LocalDateTime.now().minusDays(30);
        }
        if ("semester".equals(range) || "term".equals(range) || "90d".equals(range)) {
            return LocalDateTime.now().minusDays(90);
        }
        return LocalDateTime.now().minusDays(30);
    }
}
