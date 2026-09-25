package com.edumind.statistics.service.teaching.impl;

import com.edumind.ai.api.AiAuditQueryApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.enums.WrongErrorType;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.teaching.TeachingReportService;
import com.edumind.statistics.service.teaching.support.DiagnosisAdviceExtractor;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.teaching.TeachingReportVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教学报告聚合服务。
 *
 * <p><b>数据真实性约束</b>：本服务的所有指标必须可追溯到真实数据源。
 * 历史实现存在三类问题，已在本类中修正：</p>
 * <ol>
 *   <li><b>语义错位</b>：把作业提交率当作"班级期末及格预测率"返回；</li>
 *   <li><b>公式合成</b>：大纲推进度 = 章节数 × 12、掌握度 = 92 - 错误数 × 11、
 *       周活跃走势写死 {12,19,28,22,35,41,30}；</li>
 *   <li><b>文案编造</b>：无诊断记录时填固定错因与建议。</li>
 * </ol>
 * <p>修正后无数据一律返回 {@code null} / {@code 0}，由前端展示"暂无数据"。</p>
 *
 * <p><b>建议与诊断同源</b>：{@code weakPoints.suggestion} 优先复用
 * {@code wrong_question_record.diagnosis} 里 AI 给出的教学补救结论（与「错题分析」同一条记录），
 * 抽不到时才回落到错因类型模板；两者都没有时只输出状态说明，不编造教学建议。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingReportServiceImpl implements TeachingReportService {

    /** 薄弱考点榜单最大条数 */
    private static final int MAX_WEAK_POINTS = 6;

    /** 单份答卷人工批改均时（分钟），用于估算 AI 辅助批改节约工时。属估算系数，前端需标注"估算" */
    private static final double AI_GRADING_MINUTES_PER_PAPER = 3.0;

    /** 错因类型 → 中文名，与 wrong_question_record.error_types 取值域保持一致 */
    private static final Map<String, String> ERROR_TYPE_LABELS = Map.of(
            "CONCEPT", "概念理解错误",
            "LOGIC", "逻辑推理错误",
            "CALC", "计算失误",
            "READING", "审题理解偏差",
            "TRANSFER", "迁移应用错误",
            "MEMORY", "记忆遗忘型错误");

    /**
     * 错因类型 → 规则化教学建议模板。
     * 文案依据真实的错因分类生成，不含对具体班级的虚构事实描述。
     */
    private static final Map<String, String> SUGGESTION_TEMPLATES = Map.of(
            "CONCEPT", "建议重新讲评概念的定义与判定条件，配合 3 组辨析变式题强化边界认知",
            "CALC", "建议训练分步验算与估算校验习惯，针对易错步骤安排限时计算专项",
            "LOGIC", "建议梳理前置知识依赖链，重点讲评推理链断点与充分必要条件",
            "READING", "建议开展审题训练：先圈定题设限定条件，再动笔建模",
            "TRANSFER", "建议安排跨情境迁移变式训练，强化知识点的适用边界辨析",
            "MEMORY", "建议采用间隔重复与回忆式复习，配合课堂随机抽查巩固记忆");

    /** 无 AI 归因可接入时的状态说明：只解释为什么没有建议，不虚构教学建议 */
    private static final String ADVICE_PENDING =
            "该考点尚未完成 AI 认知归因，暂无可执行建议；可在「错题分析」中对原题发起 AI 深度诊断后查看。";

    /** 空白作答记录的状态说明：错因无从归因，直接引导到可执行的下一步 */
    private static final String ADVICE_UNANSWERED =
            "该考点本次为空白作答（未提交答案），不构成可归因的失分模式；建议先让学生完成作答，或直接生成针对性巩固测验。";

    /**
     * 关键词推断错因类型时的特征词表。
     *
     * <p>{@code wrong_question_record.error_types} 为空时，过去只看"概念/定义"就判 CONCEPT，
     * 会把"零点定理量词误读"这类逻辑问题误判成概念问题。改为按命中次数取最多者，
     * 平票时按本表顺序（越具体的审题信号越优先），并给结果打上 inferred 标记。</p>
     */
    private static final List<Map.Entry<String, List<String>>> ERROR_TYPE_KEYWORDS = List.of(
            Map.entry("READING", List.of("审题", "题设", "题意", "漏看", "限定条件")),
            Map.entry("LOGIC", List.of("逻辑", "推理", "量词", "存在性", "唯一性", "因果")),
            Map.entry("CALC", List.of("计算", "运算", "漏算", "符号", "算错")),
            Map.entry("CONCEPT", List.of("概念", "定义", "定理", "判定条件", "适用条件")));

    /**
     * 薄弱考点榜单排序：实测掌握度升序（越薄弱越靠前），无实测数据的统一后置；
     * 同掌握度按累计答错人次降序。榜单标题承诺的是"掌握度榜"，排序依据必须与之一致。
     */
    private static final Comparator<TeachingReportVO.WeakPointVO> WEAK_POINT_ORDER =
            Comparator.comparingInt((TeachingReportVO.WeakPointVO w) -> w.getMasteryRate() == null ? 1 : 0)
                    .thenComparingInt(w -> w.getMasteryRate() == null ? 0 : w.getMasteryRate())
                    .thenComparingInt(w -> w.getWrongCount() == null ? 0 : -w.getWrongCount());

    /**
     * 考点掌握度统计。
     *
     * @param avgPercent      班级实测均值 (0~100)
     * @param studentCount    该考点有实测记录的学生数（口径为"人"，不是测评"人次"）
     * @param assessmentCount 该考点累计测评次数（Σ knowledge_mastery.sample_count）
     */
    private record KpMasteryStat(double avgPercent, int studentCount, int assessmentCount) {
    }

    private final CourseQueryApi courseQueryApi;
    private final QuestionQueryApi questionQueryApi;
    private final AiAuditQueryApi aiAuditQueryApi;
    private final SubmissionQueryApi submissionQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final LearningAnalyticsService learningAnalyticsService;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final com.edumind.statistics.dao.CourseStatisticsDao courseStatisticsDao;

    @Override
    public TeachingReportVO buildReport(Long courseId, String range) {
        String effectiveRange = StringUtils.hasText(range) ? range : "7d";
        TeachingReportVO report = new TeachingReportVO();
        report.setCourseId(courseId);
        report.setRange(effectiveRange);

        // 1. 课程基本元数据与学生总数（真实选课数据）
        fillCourseMeta(report, courseId);

        // 2. 学习行为分析：大纲推进度、测验及格率、活跃趋势、数据更新时间均为真实值
        LearningAnalyticsVO learning = safeLoadLearningAnalytics(courseId, effectiveRange);
        fillFromLearningAnalytics(report, learning);

        // 3. 作业提交与批改统计（真实提交率口径见 SubmissionStatsQueryServiceImpl）
        SubmissionStatsVO submissionStats = safeLoadSubmissionStats(courseId);
        report.setAvgSubmissionRate(round1(submissionStats.getAvgSubmissionRate()));
        report.setSubmittedCount(submissionStats.getSubmittedCount());
        report.setExpectedSubmissionCount(submissionStats.getExpectedSubmissionCount() != null
                ? submissionStats.getExpectedSubmissionCount() : 0);
        report.setGradedCount(submissionStats.getGradedCount());
        report.setAvgScore(round1(submissionStats.getAvgScore()));

        // 4. AI 答疑调用次数（预聚合优先，兜底 ai_call_log / 知识库维度）
        report.setAiCallCount(countAiCallsByCourse(courseId, effectiveRange));

        // 5. AI 辅助批改节约工时：基于真实已批改份数估算，明确标记为估算值
        int gradedCount = submissionStats.getGradedCount() != null ? submissionStats.getGradedCount() : 0;
        report.setSavedHours(round1(gradedCount * AI_GRADING_MINUTES_PER_PAPER / 60.0));
        report.setSavedHoursEstimated(true);

        /*
         * 班级名单（真实选课成员）：掌握度与错题的班级口径都必须收敛到这份名单。
         * 库中存在非本课程学员的历史学情数据（练习会话按题目所属课程落库产生），
         * 若不收敛，会出现「班级只有 2 名学员，却显示累计答错 12 人次」这类自相矛盾的数字。
         * 名单获取失败时返回 null，此时不做收敛，宁可多算也不能把报告整页清空。
         */
        List<Long> classStudentIds = loadClassStudentIds(courseId);

        // 6. 知识点全班掌握度：真实实测掌握度记录聚合；只要存在推算方格即标记为含推算
        KnowledgeMasteryVO mastery = safeLoadMastery(courseId);
        Map<Long, KpMasteryStat> kpMasteryMap = buildKpMasteryMap(courseId, classStudentIds);
        boolean hasRealMastery = !kpMasteryMap.isEmpty();
        if (mastery != null && mastery.getClassAvgMastery() != null && mastery.getClassAvgMastery() > 0) {
            report.setKnowledgeMasteryAvg(round1(mastery.getClassAvgMastery()));
        }
        report.setMasteryEstimated(!hasRealMastery || hasEstimatedCells(mastery));
        report.setMasteryStudentCount(mastery != null && mastery.getStudentCount() != null ? mastery.getStudentCount() : 0);

        // 7. 错题记录：错因聚类与薄弱考点榜单共用同一份全量数据，避免同一请求内重复全表查询
        List<WrongQuestionRecordEntity> wrongRecords = loadWrongQuestionRecords(courseId, classStudentIds);

        // 8. 错因聚类（wrong_question_record.error_types 真实聚合）
        report.setErrorCategories(buildErrorCategories(wrongRecords));

        // 9. 薄弱考点：真实题干 + 真实掌握度 + 真实错因类型，无数据不编造
        report.setWeakPoints(buildWeakPoints(courseId, wrongRecords, kpMasteryMap, mastery));

        // 10. 报告是否存在可用于分析的真实数据
        boolean hasLearning = report.getWeeklyActivity() != null && !report.getWeeklyActivity().isEmpty();
        report.setHasRealData(hasRealMastery
                || !report.getWeakPoints().isEmpty()
                || !report.getErrorCategories().isEmpty()
                || hasLearning);

        // 说明：本接口不再调用 RecommendationService 计算「推荐题/推荐资源」条数。
        // 该字段前端从未展示，而推荐服务内部会为同一次请求再触发 2~3 次
        // KnowledgeMasteryService.getMastery（每次都含错题与答卷的全量扫描），
        // 实测是接口耗时的主要来源之一，故整条计算链路已移除。
        return report;
    }

    /** 一次性加载课程错题记录；异常时返回空列表，由上层走空态展示 */
    private List<WrongQuestionRecordEntity> loadWrongQuestionRecords(Long courseId, List<Long> classStudentIds) {
        try {
            List<WrongQuestionRecordEntity> records = wrongQuestionRecordDao.listByCourse(courseId);
            return restrictToClass(records, classStudentIds);
        } catch (Exception e) {
            log.warn("Failed to load wrong question records for course {}: {}", courseId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 班级名单 ID 集合；{@code null} 表示名单不可用（接口异常），调用方据此跳过收敛。
     *
     * <p>口径与 {@code KnowledgeMasteryServiceImpl} 的班级名单一致（真实选课成员）；
     * 后者额外剔除了管理员/测试账号，此处不重复实现该规则，避免同一条判定散落成第三份副本。
     * 报告中「参与统计学生数」同样取自课程选课人数，保证同一页面的两处人数可互相验证。</p>
     */
    private List<Long> loadClassStudentIds(Long courseId) {
        try {
            List<Long> studentIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
            if (studentIds == null) {
                return null;
            }
            return studentIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load enrolled students for course {}: {}", courseId, e.getMessage());
            return null;
        }
    }

    /**
     * 把学员维度记录收敛到班级名单内。
     *
     * <p>{@code classStudentIds} 为 {@code null} 或空集合时原样返回：前者代表名单不可用，
     * 后者代表课程确实没有选课学员，此时收敛只会把报告清空，交由上层展示空态而非静默丢数。</p>
     */
    private static List<WrongQuestionRecordEntity> restrictToClass(List<WrongQuestionRecordEntity> records,
                                                                  List<Long> classStudentIds) {
        if (records == null) {
            return new ArrayList<>();
        }
        if (classStudentIds == null || classStudentIds.isEmpty()) {
            return records;
        }
        Set<Long> allowed = new HashSet<>(classStudentIds);
        return records.stream()
                .filter(record -> record != null && record.getStudentId() != null && allowed.contains(record.getStudentId()))
                .collect(Collectors.toList());
    }

    /** 课程名称、编码、教师、选课学生数、章节数（均为真实课程域数据） */
    private void fillCourseMeta(TeachingReportVO report, Long courseId) {
        try {
            CourseDetailVO courseDetail = courseQueryApi.getCourseById(courseId);
            if (courseDetail != null) {
                report.setCourseName(courseDetail.getName());
                report.setCourseCode(courseDetail.getCode());
                report.setTeacherName(courseDetail.getTeacherName());
                if (courseDetail.getStudentCount() != null && courseDetail.getStudentCount() > 0) {
                    report.setStudentCount(courseDetail.getStudentCount().intValue());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch course details for courseId {}: {}", courseId, e.getMessage());
        }

        if (report.getStudentCount() == null) {
            try {
                List<Long> students = courseQueryApi.listStudentUserIdsByCourseId(courseId);
                report.setStudentCount(students != null ? students.size() : 0);
            } catch (Exception e) {
                report.setStudentCount(0);
            }
        }

        try {
            List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
            report.setTotalChapters(chapters != null ? chapters.size() : 0);
        } catch (Exception e) {
            report.setTotalChapters(0);
        }
    }

    private LearningAnalyticsVO safeLoadLearningAnalytics(Long courseId, String range) {
        try {
            return learningAnalyticsService.getLearningAnalytics(courseId, range, null);
        } catch (Exception e) {
            log.warn("Failed to load learning analytics for course {}: {}", courseId, e.getMessage());
            return null;
        }
    }

    private SubmissionStatsVO safeLoadSubmissionStats(Long courseId) {
        try {
            SubmissionStatsVO stats = submissionQueryApi.getCourseSubmissionStats(courseId);
            return stats != null ? stats : new SubmissionStatsVO();
        } catch (Exception e) {
            log.warn("Failed to load submission stats for course {}: {}", courseId, e.getMessage());
            return new SubmissionStatsVO();
        }
    }

    private KnowledgeMasteryVO safeLoadMastery(Long courseId) {
        try {
            return knowledgeMasteryService.getMastery(courseId, null);
        } catch (Exception e) {
            log.warn("Failed to load knowledge mastery for course {}: {}", courseId, e.getMessage());
            return null;
        }
    }

    /**
     * 从学习分析结果中提取真实指标：
     * 大纲推进度 = 周期内有学习行为的章节数 / 总章节数；
     * 及格率 = 已批改学生中均分 ≥ 60 的占比；
     * 活跃趋势 = 真实日活用户数（无记录时保持空列表，不再写死走势）。
     */
    private void fillFromLearningAnalytics(TeachingReportVO report, LearningAnalyticsVO learning) {
        if (learning == null) {
            report.setSyllabusProgress(0);
            report.setPassRate(0);
            return;
        }

        LearningAnalyticsVO.CourseHealthVO health = learning.getCourseHealth();
        report.setSyllabusProgress(health != null && health.getSyllabusCoverage() != null ? health.getSyllabusCoverage() : 0);
        report.setPassRate(health != null && health.getPassRate() != null ? health.getPassRate() : 0);
        report.setDataUpdatedAt(learning.getDataUpdatedAt());

        List<LearningAnalyticsVO.TrendPoint> trendPoints =
                learning.getTrends() != null ? learning.getTrends().getLearning() : null;
        if (trendPoints == null || trendPoints.isEmpty()) {
            report.setWeeklyActivity(new ArrayList<>());
            return;
        }
        List<TeachingReportVO.WeeklyActivityVO> weekly = trendPoints.stream()
                .filter(Objects::nonNull)
                .map(tp -> {
                    TeachingReportVO.WeeklyActivityVO item = new TeachingReportVO.WeeklyActivityVO();
                    item.setDate(tp.getDate());
                    item.setCount(tp.getActiveUsers() != null ? tp.getActiveUsers() : 0);
                    return item;
                })
                .collect(Collectors.toList());
        report.setWeeklyActivity(weekly);
    }

    private int countAiCallsByCourse(Long courseId, String range) {
        int days = resolveRangeDays(range);
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays(days);
        List<com.edumind.statistics.entity.CourseStatisticsEntity> stats =
                courseStatisticsDao.listByCourseAndDateRange(courseId, startDate, endDate);
        if (!stats.isEmpty()) {
            return stats.stream()
                    .mapToInt(s -> s.getAiCallCount() != null ? s.getAiCallCount() : 0)
                    .sum();
        }
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(days);
        long direct = aiAuditQueryApi.countCallsByCourse(courseId, since);
        if (direct > 0) {
            return (int) direct;
        }
        List<Long> kbIds = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId).stream()
                .map(KnowledgeBaseVO::getId)
                .collect(Collectors.toList());
        return (int) aiAuditQueryApi.countCallsByKnowledgeBases(kbIds);
    }

    /** 统计周期天数：7d=7，30d=30，semester/term/90d=90（与 LearningAnalyticsServiceImpl 对齐） */
    private static int resolveRangeDays(String range) {
        if ("30d".equals(range)) {
            return 30;
        }
        if ("semester".equals(range) || "term".equals(range) || "90d".equals(range)) {
            return 90;
        }
        return 7;
    }

    /**
     * 掌握度矩阵中是否存在推算方格（实测方格数 &lt; 总方格数）。
     * 存在推算方格时班级均分并非纯实测结果，需向教师标注口径。
     */
    private static boolean hasEstimatedCells(KnowledgeMasteryVO mastery) {
        if (mastery == null) {
            return false;
        }
        Integer measured = mastery.getMeasuredCellCount();
        Integer total = mastery.getTotalCellCount();
        if (total == null || total <= 0) {
            return false;
        }
        return measured == null || measured < total;
    }

    /**
     * 真实实测掌握度映射：知识点 ID → (班级平均掌握度 0~100, 实测学生数, 累计测评次数)。
     * 数据源 knowledge_mastery，仅包含学生实测记录，不做任何推算。
     *
     * <p>样本量必须成对透出：只有 1~2 名学生有实测记录时，0% 不代表全班都不掌握。
     * 历史实现把「记录条数」当作「人次」并单独展示，语义含混；这里拆成
     * 「有实测记录的学生数」与「累计测评次数（Σ sample_count）」两个字段，
     * 由前端分别表述，教师才能判断这个百分比覆盖了多少人、多少次测评。</p>
     *
     * <p>记录同时收敛到班级名单：非本课程学员的练习数据不得进入班级掌握度。</p>
     */
    private Map<Long, KpMasteryStat> buildKpMasteryMap(Long courseId, List<Long> classStudentIds) {
        Map<Long, KpMasteryStat> result = new LinkedHashMap<>();
        try {
            List<KnowledgeMasteryEntity> entities = restrictMasteryToClass(
                    knowledgeMasteryDao.listByCourse(courseId), classStudentIds);
            if (entities.isEmpty()) {
                return result;
            }
            Map<Long, List<KnowledgeMasteryEntity>> grouped = entities.stream()
                    .filter(m -> m.getKnowledgePointId() != null && m.getMasteryScore() != null)
                    .collect(Collectors.groupingBy(KnowledgeMasteryEntity::getKnowledgePointId,
                            LinkedHashMap::new, Collectors.toList()));
            grouped.forEach((kpId, list) -> {
                double avg = list.stream()
                        .mapToDouble(m -> m.getMasteryScore().doubleValue())
                        .average()
                        .orElse(0.0);
                int assessmentCount = list.stream()
                        .mapToInt(m -> m.getSampleCount() != null && m.getSampleCount() > 0 ? m.getSampleCount() : 1)
                        .sum();
                result.put(kpId, new KpMasteryStat(round1(avg * 100.0), list.size(), assessmentCount));
            });
        } catch (Exception e) {
            log.warn("Failed to build knowledge mastery map for course {}: {}", courseId, e.getMessage());
        }
        return result;
    }

    /** 掌握度记录收敛到班级名单；名单不可用（null / 空）时原样返回，不做收敛 */
    private static List<KnowledgeMasteryEntity> restrictMasteryToClass(List<KnowledgeMasteryEntity> entities,
                                                                      List<Long> classStudentIds) {
        if (entities == null) {
            return new ArrayList<>();
        }
        if (classStudentIds == null || classStudentIds.isEmpty()) {
            return entities;
        }
        Set<Long> allowed = new HashSet<>(classStudentIds);
        return entities.stream()
                .filter(entity -> entity != null && entity.getStudentId() != null && allowed.contains(entity.getStudentId()))
                .collect(Collectors.toList());
    }

    /**
     * 错因聚类：对 wrong_question_record.error_types 做真实计数聚合。
     * 全量聚合（不再用 200 条分页截断，否则百分比会失真），按占比降序返回。
     *
     * @param records 由调用方一次性加载的错题记录，避免同一请求内重复全量查询
     */
    private List<TeachingReportVO.ErrorCategoryVO> buildErrorCategories(List<WrongQuestionRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> counts = new HashMap<>();
        for (WrongQuestionRecordEntity entity : records) {
            if (!StringUtils.hasText(entity.getErrorTypes())) {
                continue;
            }
            for (String type : entity.getErrorTypes().split(",")) {
                String key = type.trim().toUpperCase();
                if (StringUtils.hasText(key)) {
                    counts.merge(key, 1, Integer::sum);
                }
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return List.of();
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(e -> {
                    TeachingReportVO.ErrorCategoryVO vo = new TeachingReportVO.ErrorCategoryVO();
                    vo.setType(e.getKey());
                    vo.setName(ERROR_TYPE_LABELS.getOrDefault(e.getKey(), e.getKey()));
                    vo.setPercent((int) Math.round(e.getValue() * 100.0 / total));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 薄弱考点榜单。
     *
     * <p>优先使用真实错题记录（含题干、错因类型、AI 诊断原文）；
     * 没有错题记录时退化为真实掌握度薄弱考点（knowledge_mastery &lt; 70）。
     * 两者都无数据时返回空列表，由前端展示"暂无薄弱考点数据"。</p>
     *
     * @param records 由调用方一次性加载的错题记录，与错因聚类共用同一份数据
     */
    private List<TeachingReportVO.WeakPointVO> buildWeakPoints(Long courseId,
                                                               List<WrongQuestionRecordEntity> records,
                                                               Map<Long, KpMasteryStat> kpMasteryMap,
                                                               KnowledgeMasteryVO mastery) {
        if (records != null && !records.isEmpty()) {
            Map<Long, String> kpTitleMap = loadKnowledgePointTitles(courseId);
            List<TeachingReportVO.WeakPointVO> aggregated = aggregateByKnowledgePoint(records, kpTitleMap, kpMasteryMap);
            if (!aggregated.isEmpty()) {
                return aggregated;
            }
        }

        if (mastery == null || mastery.getWeakPoints() == null || mastery.getWeakPoints().isEmpty()) {
            return new ArrayList<>();
        }
        return mastery.getWeakPoints().stream()
                .limit(MAX_WEAK_POINTS)
                .map(this::toWeakPointFromMastery)
                .collect(Collectors.toList());
    }

    /**
     * 错题记录 → 考点级榜单（同一考点的多道错题合并为一行）。
     *
     * <p>旧实现每条错题占一行，同一考点的多道题会挤占榜位，
     * 「覆盖 N 个薄弱考点」与实际展示不符。合并后累加答错人次，
     * 取答错人次最多的一条作为「查看原题」的代表，并保留组内最完整的一次 AI 诊断。</p>
     */
    private List<TeachingReportVO.WeakPointVO> aggregateByKnowledgePoint(List<WrongQuestionRecordEntity> records,
                                                                        Map<Long, String> kpTitleMap,
                                                                        Map<Long, KpMasteryStat> kpMasteryMap) {
        Map<String, List<WrongQuestionRecordEntity>> groups = new LinkedHashMap<>();
        for (WrongQuestionRecordEntity record : records) {
            if (record == null) {
                continue;
            }
            // 考点 ID 缺失时退化为按题目分组，至少不把不同题目混成一行
            String key = record.getKnowledgePointId() != null
                    ? "KP:" + record.getKnowledgePointId()
                    : "Q:" + record.getQuestionId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        return groups.values().stream()
                .map(group -> toWeakPointFromGroup(group, kpTitleMap, kpMasteryMap))
                .filter(Objects::nonNull)
                .sorted(WEAK_POINT_ORDER)
                .limit(MAX_WEAK_POINTS)
                .collect(Collectors.toList());
    }

    /** 同一考点的多条错题记录 → 单个榜单条目 */
    private TeachingReportVO.WeakPointVO toWeakPointFromGroup(List<WrongQuestionRecordEntity> group,
                                                             Map<Long, String> kpTitleMap,
                                                             Map<Long, KpMasteryStat> kpMasteryMap) {
        if (group == null || group.isEmpty()) {
            return null;
        }

        // 代表记录：题干、错因标签、AI 诊断正文、查看原题四者必须同源，详见 pickRepresentative
        WrongQuestionRecordEntity primary = pickRepresentative(group);

        TeachingReportVO.WeakPointVO weak = new TeachingReportVO.WeakPointVO();
        weak.setQuestionId(primary.getQuestionId());
        weak.setWrongQuestionCount(group.size());
        weak.setWrongCount(group.stream()
                .mapToInt(r -> r.getWrongCount() != null ? r.getWrongCount() : 1)
                .sum());

        Long knowledgePointId = primary.getKnowledgePointId();
        String knowledgePointName = null;

        QuestionVO question = safeGetQuestion(primary.getQuestionId());
        if (question != null) {
            weak.setQuestionStem(question.getStem());
            // 原题选项与答案必须同源透出：抽屉里只给题干、不给选项，教师无法判断学生到底选错了哪一项
            weak.setQuestionType(question.getType());
            weak.setQuestionOptions(question.getOptions());
            weak.setQuestionAnswer(question.getAnswer());
            if (knowledgePointId == null) {
                knowledgePointId = question.getKnowledgePointId();
            }
            knowledgePointName = question.getKnowledgePointName();
        }
        weak.setKnowledgePointId(knowledgePointId);

        if (!StringUtils.hasText(knowledgePointName) && knowledgePointId != null) {
            knowledgePointName = kpTitleMap.get(knowledgePointId);
        }
        if (!StringUtils.hasText(knowledgePointName)) {
            knowledgePointName = knowledgePointId != null ? "考点 #" + knowledgePointId : "未关联考点";
        }
        weak.setKnowledgePointName(knowledgePointName);
        weak.setTitle(knowledgePointName);

        // 真实掌握度：仅当存在该知识点的实测掌握度记录时才有值，同时透出实测学生数与测评次数
        KpMasteryStat stat = knowledgePointId != null ? kpMasteryMap.get(knowledgePointId) : null;
        Integer masteryRate = stat != null ? (int) Math.round(stat.avgPercent()) : null;
        weak.setMasteryRate(masteryRate);
        weak.setMasterySampleCount(stat != null ? stat.studentCount() : null);
        weak.setMasteryAssessmentCount(stat != null ? stat.assessmentCount() : null);
        applyMasteryStatus(weak, masteryRate);

        // 错因类型、诊断正文与「查看原题」同源：全部取自同一条代表记录
        ErrorTypeResolution resolution = resolveErrorType(primary);
        weak.setErrorType(resolution != null ? resolution.code() : null);
        weak.setErrorTypeName(resolution != null
                ? ERROR_TYPE_LABELS.getOrDefault(resolution.code(), "典型错因")
                : "待归因");
        weak.setErrorTypeInferred(resolution != null && resolution.inferred());
        weak.setErrorReason(cleanDiagnosisText(primary.getDiagnosis()));
        // 建议与诊断同源：优先复用 AI 归因正文中的教学补救结论
        weak.setSuggestion(buildSuggestion(
                resolution != null ? resolution.code() : null,
                knowledgePointName,
                primary.getDiagnosis()));
        return weak;
    }

    /**
     * 组内代表记录：优先取「已完成 AI 归因」的那条，其中再取答错人次最多的；都没有诊断时退回答错人次最多的一条。
     *
     * <p>同一考点被合并为一行后，题干、错因标签、诊断正文、变式题入口必须来自同一条错题记录。
     * 历史实现用「答错人次最多」选题干，却用「组内诊断文本最长」兜底正文，
     * 于是出现「标签说概念错误、正文却在讲另一道题」的错位；更糟的是正文对应的题目
     * 与「查看原题」打开的题目不是同一道，教师无法核对。</p>
     */
    private static WrongQuestionRecordEntity pickRepresentative(List<WrongQuestionRecordEntity> group) {
        return group.stream()
                .filter(record -> StringUtils.hasText(record.getDiagnosis()))
                .max(Comparator.comparingInt(TeachingReportServiceImpl::wrongCountOf))
                .orElseGet(() -> group.stream()
                        .max(Comparator.comparingInt(TeachingReportServiceImpl::wrongCountOf))
                        .orElse(group.get(0)));
    }

    /** 错题记录的累计答错人次，缺省按 1 次计 */
    private static int wrongCountOf(WrongQuestionRecordEntity record) {
        return record.getWrongCount() != null ? record.getWrongCount() : 1;
    }

    /**
     * 诊断原文下发前的清洗。
     *
     * <p>报告侧过去直接透传 {@code record.diagnosis}，会把「主要失分诱因代码：[ ]」
     * 「类型：CONCEPT」这类模型中间产物原样展示给教师。这里与错题本共用同一套清洗规则。</p>
     */
    private static String cleanDiagnosisText(String diagnosis) {
        if (!StringUtils.hasText(diagnosis)) {
            return null;
        }
        String cleaned = WrongErrorType.stripTypeMarker(diagnosis);
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    /**
     * 教学建议生成优先级：
     * <ol>
     *   <li>AI 归因正文里的教学补救结论 —— 与「错题分析」同源于 {@code wrong_question_record.diagnosis}，
     *       保证同一考点在报告与错题分析里看到的是同一口径的结论；</li>
     *   <li>错因类型对应的规则模板 —— AI 结论抽取不到时的稳定兜底，并绑定考点名；</li>
     *   <li>状态说明 —— 既无 AI 结论也无错因类型时，只说明原因，绝不虚构教学建议。</li>
     * </ol>
     */
    private static String buildSuggestion(String errorType, String knowledgePointName, String diagnosis) {
        String aiAdvice = DiagnosisAdviceExtractor.extract(diagnosis);
        if (StringUtils.hasText(aiAdvice)) {
            return StringUtils.hasText(knowledgePointName)
                    ? "围绕「" + knowledgePointName + "」，" + aiAdvice
                    : aiAdvice;
        }
        String template = errorType != null ? SUGGESTION_TEMPLATES.get(errorType) : null;
        if (template == null) {
            return resolvePendingAdvice(diagnosis);
        }
        return StringUtils.hasText(knowledgePointName)
                ? "围绕「" + knowledgePointName + "」，" + template
                : template;
    }

    /** 无归因结论时的状态说明：区分「空白作答」与「尚未归因」，两种情况都不编造建议 */
    private static String resolvePendingAdvice(String diagnosis) {
        if (StringUtils.hasText(diagnosis)
                && NON_ATTRIBUTABLE_MARKERS.stream().anyMatch(diagnosis::contains)) {
            return ADVICE_UNANSWERED;
        }
        return ADVICE_PENDING;
    }

    private TeachingReportVO.WeakPointVO toWeakPointFromMastery(KnowledgeMasteryVO.WeakPointVO wp) {
        TeachingReportVO.WeakPointVO weak = new TeachingReportVO.WeakPointVO();
        weak.setKnowledgePointId(wp.getKnowledgePointId());
        weak.setKnowledgePointName(wp.getTitle());
        weak.setTitle(wp.getTitle());
        weak.setChapterName(wp.getChapterName());
        weak.setWrongCount(wp.getWrongCount() != null ? wp.getWrongCount() : 0);

        // 掌握度可信度：纯推算（ESTIMATED）时不展示数值，
        // 避免把规则推算值当作实测掌握度呈现给教师
        boolean fullyEstimated = "ESTIMATED".equalsIgnoreCase(wp.getDataConfidence());
        if (fullyEstimated || wp.getMastery() == null) {
            weak.setMasteryRate(null);
            weak.setStatus("unknown");
            weak.setStatusLabel(fullyEstimated ? "待测评验证" : "暂无测评数据");
        } else {
            Integer masteryRate = (int) Math.round(wp.getMastery() * 100);
            weak.setMasteryRate(masteryRate);
            applyMasteryStatus(weak, masteryRate);
        }

        // 掌握度样本人次：该考点上真正有实测成绩的学员数，供前端标注口径
        weak.setMasterySampleCount(wp.getMeasuredStudentCount());

        // 该分支说明尚无错题诊断记录，错因类型未知，不做猜测
        weak.setErrorType(null);
        weak.setErrorTypeName("待 AI 归因");
        weak.setErrorTypeInferred(false);
        weak.setErrorReason(null);
        // 掌握度分支没有错题记录可复用，只保留掌握度服务给出的建议；缺失时说明原因而不是留空
        weak.setSuggestion(StringUtils.hasText(wp.getSuggestion()) ? wp.getSuggestion() : ADVICE_PENDING);
        return weak;
    }

    /** 掌握度 → 状态标签；无实测数据时状态为 unknown，前端展示"暂无测评数据" */
    private void applyMasteryStatus(TeachingReportVO.WeakPointVO weak, Integer masteryRate) {
        if (masteryRate == null) {
            weak.setStatus("unknown");
            weak.setStatusLabel("暂无测评数据");
            return;
        }
        if (masteryRate < 50) {
            weak.setStatus("danger");
            weak.setStatusLabel("急需攻坚");
        } else if (masteryRate < 70) {
            weak.setStatus("warning");
            weak.setStatusLabel("待巩固强化");
        } else if (masteryRate < 82) {
            weak.setStatus("normal");
            weak.setStatusLabel("稳步提升中");
        } else {
            weak.setStatus("good");
            weak.setStatusLabel("掌握良好");
        }
    }

    /** 错因类型解析结果：code + 是否来自关键词推断 */
    private record ErrorTypeResolution(String code, boolean inferred) {
    }

    /**
     * 诊断正文中自述「无法归因」的信号词。
     *
     * <p>空白作答、未提交的错题记录没有可归因的作答痕迹，模型已在正文里明确声明不作归因；
     * 这类记录不得再按关键词硬推一个错因，否则教师会看到「正文说不作归因、标签写计算失误」。</p>
     */
    private static final List<String> NON_ATTRIBUTABLE_MARKERS = List.of(
            "无法定位", "无法判断", "无法归因", "不作认知归因", "不作归因", "未作答", "空白", "答案缺失");

    /**
     * 错因类型归类：优先取 wrong_question_record.error_types 的真实标注；
     * 未标注时才按诊断正文的关键词分布推断，并标记为 inferred（无法判断返回 null，不猜测）。
     */
    private ErrorTypeResolution resolveErrorType(WrongQuestionRecordEntity record) {
        if (StringUtils.hasText(record.getErrorTypes())) {
            for (String part : record.getErrorTypes().split(",")) {
                String key = part.trim().toUpperCase();
                if (ERROR_TYPE_LABELS.containsKey(key)) {
                    return new ErrorTypeResolution(key, false);
                }
            }
        }
        String diagnosis = record.getDiagnosis();
        if (!StringUtils.hasText(diagnosis)) {
            return null;
        }
        // 正文自述「无法归因」时不硬推错因：空白作答/未提交本身没有可归因的作答痕迹，
        // 再按关键词推一个类型，就会出现「正文写着不作归因、标签却显示计算失误」的自相矛盾
        if (NON_ATTRIBUTABLE_MARKERS.stream().anyMatch(diagnosis::contains)) {
            return null;
        }
        String inferred = inferErrorTypeByKeywords(diagnosis);
        return inferred != null ? new ErrorTypeResolution(inferred, true) : null;
    }

    /**
     * 关键词命中次数最多者胜出，平票保留 {@link #ERROR_TYPE_KEYWORDS} 中更靠前的类型。
     *
     * <p>旧实现按固定顺序首个命中即返回，且把"概念/定义"排在"逻辑/推理"之前，
     * 使"零点定理量词误读"这类逻辑偏差被误判为概念理解错误，进而套用错误的建议模板。</p>
     */
    private static String inferErrorTypeByKeywords(String diagnosis) {
        String best = null;
        int bestHits = 0;
        for (Map.Entry<String, List<String>> entry : ERROR_TYPE_KEYWORDS) {
            int hits = 0;
            for (String keyword : entry.getValue()) {
                if (diagnosis.contains(keyword)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = entry.getKey();
            }
        }
        return best;
    }

    private Map<Long, String> loadKnowledgePointTitles(Long courseId) {
        Map<Long, String> titles = new HashMap<>();
        try {
            List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
            if (points != null) {
                for (KnowledgePointVO point : points) {
                    if (point.getId() != null && StringUtils.hasText(point.getTitle())) {
                        titles.put(point.getId(), point.getTitle());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load knowledge point titles for course {}: {}", courseId, e.getMessage());
        }
        return titles;
    }

    private QuestionVO safeGetQuestion(Long questionId) {
        if (questionId == null) {
            return null;
        }
        try {
            return questionQueryApi.getQuestionById(questionId);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double round1(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
