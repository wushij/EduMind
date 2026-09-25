package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.support.MasteryScoreResolver;
import com.edumind.statistics.service.analytics.support.MasteryScoreResolver.MasteryContext;
import com.edumind.statistics.service.analytics.support.MasteryScoreResolver.MasterySource;
import com.edumind.statistics.service.analytics.support.MasteryScoreResolver.ResolvedScore;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程知识点掌握度画像服务。
 *
 * <p>统计口径约定（修复前该口径在多个接口间是不一致的）：</p>
 * <ol>
 *   <li><b>班级名单</b>只取课程真实选课成员（{@code course_member}）。历史上会把
 *       {@code knowledge_mastery} 表里的退课学员、以及请求里传入的「聚焦学员 ID」一并塞进班级名单，
 *       导致全班视图误传教师 ID 时，班级均分被这名并不存在的「幽灵学员」的推算分拉高；</li>
 *   <li><b>聚焦学员</b>必须属于班级名单，否则视为未聚焦。修复前 Controller 会在全班视图下
 *       隐式用当前登录用户 ID 兜底，教师本人被当成学生，出现「指标条说 3 个薄弱考点、
 *       榜单却说 0 处预警」的自相矛盾；</li>
 *   <li><b>分值来源</b>统一由 {@link MasteryScoreResolver} 产出，雷达图与热力矩阵不再各算一套；</li>
 *   <li><b>薄弱口径</b>跟随作用域：精熟/良好/薄弱计数与薄弱考点榜单使用同一份分值数组，
 *       保证「薄弱待攻坚 N 个」恒等于「N 处预警」。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeMasteryServiceImpl implements KnowledgeMasteryService {

    /** 精熟阈值：考点均分 ≥85% 计为精熟 */
    private static final int MASTERED_THRESHOLD = 85;

    /** 良好阈值：考点均分 ≥70% 计为良好 */
    private static final int GOOD_THRESHOLD = 70;

    /** 薄弱考点入榜阈值：考点分值 <70% */
    private static final double WEAK_POINT_THRESHOLD = 0.70;

    /** 薄弱考点榜单最大条数 */
    private static final int WEAK_POINT_LIMIT = 8;

    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final CourseQueryApi courseQueryApi;
    private final UserQueryApi userQueryApi;
    private final OrganizationQueryApi organizationQueryApi;
    private final SubmissionQueryApi submissionQueryApi;
    private final MasteryScoreResolver masteryScoreResolver;

    // ------------------------------------------------------------------
    // 掌握度画像（雷达图 + 指标条 + 薄弱考点榜单）
    // ------------------------------------------------------------------

    @Override
    public KnowledgeMasteryVO getMastery(Long courseId, Long studentId) {
        return getMastery(courseId, studentId, false);
    }

    @Override
    public KnowledgeMasteryVO getMastery(Long courseId, Long studentId, boolean includeTesting) {
        KnowledgeMasteryVO vo = new KnowledgeMasteryVO();
        vo.setScope(KnowledgeMasteryVO.SCOPE_CLASS);
        if (courseId == null) {
            return vo;
        }

        // 1. 课程考点与章节信息
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, String> chapterNameMap = buildChapterNameMap(courseId);
        if (points.isEmpty()) {
            return vo;
        }
        vo.setTotalKnowledgePoints(points.size());
        for (KnowledgePointVO point : points) {
            vo.getDimensions().add(point.getTitle());
        }

        // 2. 班级名单：仅课程真实选课成员，不再混入历史退课学员
        List<Long> enrolledStudentIds = listEnrolledStudentIds(courseId);
        Map<Long, UserBriefVO> userMap = safeMapUserBriefs(enrolledStudentIds);

        // 3. 测试账号过滤：与热力矩阵共用同一批学员，指标条人数与矩阵人数从此一致
        List<Long> statStudentIds = filterTestingAccounts(enrolledStudentIds, userMap, includeTesting);
        vo.setStudentCount(statStudentIds.size());
        vo.setClassStudentCount(enrolledStudentIds.size());

        // 4. 聚焦学员必须属于班级名单，教师/管理员/占位 ID 一律视为未聚焦
        Long focusStudentId = statStudentIds.contains(studentId) ? studentId : null;
        if (focusStudentId != null) {
            vo.setScope(KnowledgeMasteryVO.SCOPE_STUDENT);
            vo.setFocusStudentId(focusStudentId);
        }

        Map<Long, MemberOrgBriefVO> orgMap = safeMapPrimaryClasses(statStudentIds);

        // 5. 原始学情数据 → 掌握度分值（全模块唯一换算入口）
        MasteryContext context = buildContext(courseId, statStudentIds);

        // 6. 逐学员逐考点解析，同步累计考点维度、学员维度与数据来源计数
        Map<Long, List<Double>> kpScores = new LinkedHashMap<>();
        Map<Long, List<Double>> studentScores = new LinkedHashMap<>();
        Map<Long, Integer> kpMeasuredCountMap = new LinkedHashMap<>();
        Map<Long, Integer> kpBelowThresholdMap = new LinkedHashMap<>();
        int measuredCellCount = 0;

        for (Long sid : statStudentIds) {
            for (KnowledgePointVO point : points) {
                ResolvedScore resolved = context.resolve(sid, point);
                kpScores.computeIfAbsent(point.getId(), k -> new ArrayList<>()).add(resolved.score());
                studentScores.computeIfAbsent(sid, k -> new ArrayList<>()).add(resolved.score());
                if (resolved.isMeasured()) {
                    measuredCellCount++;
                    kpMeasuredCountMap.merge(point.getId(), 1, Integer::sum);
                }
                if (resolved.score() < WEAK_POINT_THRESHOLD) {
                    kpBelowThresholdMap.merge(point.getId(), 1, Integer::sum);
                }
            }
        }
        vo.setMeasuredCellCount(measuredCellCount);
        vo.setTotalCellCount(statStudentIds.size() * points.size());

        // 7. 班级均分雷达线：恒定基于班级名单，与聚焦学员无关
        List<Integer> classAvg = new ArrayList<>();
        for (KnowledgePointVO point : points) {
            classAvg.add(toPercent(kpScores.get(point.getId())));
        }
        vo.setClassAvg(classAvg);
        vo.setClassAvgMastery(averageOf(classAvg));

        // 8. 聚焦学员个人雷达线：未聚焦时保持为空，避免前端把空数组回退成「全班考点全薄弱」
        if (focusStudentId != null) {
            for (Double score : studentScores.getOrDefault(focusStudentId, Collections.emptyList())) {
                vo.getPersonal().add((int) Math.round(score * 100.0));
            }
            vo.setFocusAvgMastery(averageOf(vo.getPersonal()));
        }

        // 9. 统计口径与薄弱榜单同源：聚焦学员时看个人，否则看班级
        List<Integer> scopeScores = focusStudentId != null ? vo.getPersonal() : classAvg;
        int masteredCount = 0;
        int goodCount = 0;
        int warningCount = 0;
        for (Integer score : scopeScores) {
            int value = score != null ? score : 0;
            if (value >= MASTERED_THRESHOLD) {
                masteredCount++;
            } else if (value >= GOOD_THRESHOLD) {
                goodCount++;
            } else {
                warningCount++;
            }
        }
        vo.setMasteredCount(masteredCount);
        vo.setGoodCount(goodCount);
        vo.setWarningCount(warningCount);

        // 10. 薄弱考点榜单
        vo.setWeakPoints(buildWeakPoints(points, chapterNameMap, scopeScores, context,
                kpMeasuredCountMap, kpBelowThresholdMap, statStudentIds.size()));

        // 11. 学员花名册（附带实测覆盖度，便于前端标注数据可信度）
        vo.setStudents(buildStudentRoster(statStudentIds, userMap, orgMap, points, context));
        return vo;
    }

    // ------------------------------------------------------------------
    // 热力矩阵
    // ------------------------------------------------------------------

    @Override
    public Map<String, Object> getHeatmap(Long courseId, String range) {
        return getHeatmap(courseId, range, false);
    }

    @Override
    public Map<String, Object> getHeatmap(Long courseId, String range, boolean includeTesting) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("range", range);
        result.put("scope", KnowledgeMasteryVO.SCOPE_CLASS);

        if (courseId == null) {
            return fillEmptyHeatmap(result);
        }

        // 1. 考点与章节
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, String> chapterNameMap = buildChapterNameMap(courseId);

        List<Map<String, Object>> kpList = new ArrayList<>();
        for (KnowledgePointVO p : points) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getTitle() != null ? p.getTitle() : "知识点" + p.getId());
            item.put("chapterId", p.getChapterId());
            item.put("chapterName", chapterNameMap.getOrDefault(p.getChapterId(), "通用章节"));
            kpList.add(item);
        }
        result.put("knowledgePoints", kpList);

        if (points.isEmpty()) {
            return fillEmptyHeatmap(result);
        }

        // 2. 班级名单 + 测试账号过滤（与掌握度画像同一批学员）
        List<Long> enrolledStudentIds = listEnrolledStudentIds(courseId);
        Map<Long, UserBriefVO> userMap = safeMapUserBriefs(enrolledStudentIds);
        List<Long> statStudentIds = filterTestingAccounts(enrolledStudentIds, userMap, includeTesting);
        result.put("studentCount", statStudentIds.size());
        result.put("classStudentCount", enrolledStudentIds.size());

        Map<Long, MemberOrgBriefVO> orgMap = safeMapPrimaryClasses(statStudentIds);
        MasteryContext context = buildContext(courseId, statStudentIds);

        // 3. 生成学员行与方格
        List<Map<String, Object>> studentList = new ArrayList<>();
        List<Map<String, Object>> cells = new ArrayList<>();
        Map<Long, List<Double>> kpScores = new LinkedHashMap<>();
        int measuredCellCount = 0;
        int estimatedCellCount = 0;

        for (Long sid : statStudentIds) {
            UserBriefVO user = userMap.get(sid);
            MemberOrgBriefVO orgBrief = orgMap.get(sid);

            Map<String, Object> stuObj = new LinkedHashMap<>();
            stuObj.put("id", sid);
            stuObj.put("name", user != null && user.getRealName() != null ? user.getRealName() : "学员 " + sid);
            stuObj.put("username", user != null ? user.getUsername() : null);
            stuObj.put("studentNo", resolveStudentNo(orgBrief, sid));
            stuObj.put("avatar", user != null ? user.getAvatar() : null);
            stuObj.put("className", orgBrief != null && orgBrief.getName() != null ? orgBrief.getName() : "选课班级");
            stuObj.put("measuredKpCount", 0);
            stuObj.put("estimatedKpCount", 0);
            List<Map<String, Object>> studentCells = new ArrayList<>();

            int measuredKpCount = 0;
            double scoreSum = 0.0;
            for (KnowledgePointVO point : points) {
                ResolvedScore resolved = context.resolve(sid, point);
                kpScores.computeIfAbsent(point.getId(), k -> new ArrayList<>()).add(resolved.score());
                scoreSum += resolved.score();
                if (resolved.isMeasured()) {
                    measuredCellCount++;
                    measuredKpCount++;
                } else {
                    estimatedCellCount++;
                }

                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("studentId", sid);
                cell.put("knowledgePointId", point.getId());
                cell.put("mastery", Math.round(resolved.score() * 1000.0) / 1000.0);
                // 数据来源：MEASURED 实测 / ESTIMATED 规则推算，前端据此打标
                cell.put("source", resolved.source().name());
                cell.put("sampleCount", resolved.sampleCount());
                cells.add(cell);
                studentCells.add(cell);
            }

            stuObj.put("measuredKpCount", measuredKpCount);
            stuObj.put("estimatedKpCount", Math.max(0, points.size() - measuredKpCount));
            stuObj.put("avgScore", points.isEmpty()
                    ? 0.0
                    : Math.round(scoreSum / points.size() * 1000.0) / 10.0);
            studentList.add(stuObj);
        }

        // 4. 后端权威班级均分：前端不再自行重算，杜绝「列头均分」与「指标条均分」两套数字
        Map<String, Object> classAvgScores = new LinkedHashMap<>();
        for (KnowledgePointVO point : points) {
            List<Double> scores = kpScores.getOrDefault(point.getId(), Collections.emptyList());
            double avg = scores.isEmpty()
                    ? 0.0
                    : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0;
            classAvgScores.put(String.valueOf(point.getId()), Math.round(avg * 10.0) / 10.0);
        }

        result.put("students", studentList);
        result.put("cells", cells);
        result.put("classAvgScores", classAvgScores);
        result.put("measuredCellCount", measuredCellCount);
        result.put("estimatedCellCount", estimatedCellCount);
        return result;
    }

    @Override
    public Map<String, Object> getHeatmapCell(Long courseId, Long studentId, Long knowledgePointId) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("courseId", courseId);
        cell.put("studentId", studentId);
        cell.put("knowledgePointId", knowledgePointId);

        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        KnowledgePointVO target = points.stream()
                .filter(p -> p.getId() != null && p.getId().equals(knowledgePointId))
                .findFirst()
                .orElse(null);

        // 与矩阵方格使用同一解析器，保证下钻弹窗的掌握度与格子里的数字完全一致
        MasteryContext context = buildContext(courseId, listEnrolledStudentIds(courseId));
        ResolvedScore resolved = target != null
                ? context.resolve(studentId, target)
                : new ResolvedScore(MasteryScoreResolver.PRIOR_BASE_RATIO, MasterySource.ESTIMATED, 0);

        cell.put("mastery", resolved.score());
        cell.put("source", resolved.source().name());
        cell.put("sampleCount", resolved.sampleCount());

        if (target != null) {
            cell.put("knowledgePointTitle", target.getTitle());
            cell.put("description", target.getDescription());
        }

        WrongQuestionRecordEntity wrong = wrongQuestionRecordDao.findByStudentCourseAndKp(studentId, courseId, knowledgePointId);
        int wrongCount = wrong != null && wrong.getWrongCount() != null
                ? wrong.getWrongCount()
                : context.wrongCountOfPoint(knowledgePointId);
        cell.put("wrongCount", wrongCount);
        if (wrong != null) {
            cell.put("diagnosis", wrong.getDiagnosis());
            cell.put("recommendedQuestionIds", wrong.getVariantQuestionIds());
        } else {
            cell.put("diagnosis", buildEstimatingDiagnosis(resolved, wrongCount));
        }
        return cell;
    }

    // ------------------------------------------------------------------
    // 实测掌握度写回
    // ------------------------------------------------------------------

    @Override
    public void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio) {
        if (studentId == null || knowledgePointId == null) {
            return;
        }
        KnowledgeMasteryEntity existing = knowledgeMasteryDao.findByStudentAndKp(studentId, knowledgePointId);
        BigDecimal newScore = BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, scoreRatio)))
                .setScale(4, RoundingMode.HALF_UP);
        if (existing == null) {
            KnowledgeMasteryEntity entity = new KnowledgeMasteryEntity();
            entity.setStudentId(studentId);
            entity.setCourseId(courseId);
            entity.setKnowledgePointId(knowledgePointId);
            entity.setMasteryScore(newScore);
            entity.setSampleCount(1);
            entity.setLastAssessedAt(LocalDateTime.now());
            knowledgeMasteryDao.insert(entity);
            return;
        }
        int count = (existing.getSampleCount() != null ? existing.getSampleCount() : 0) + 1;
        double history = existing.getMasteryScore() != null
                ? existing.getMasteryScore().doubleValue() * (count - 1)
                : 0.0;
        double weighted = (history + newScore.doubleValue()) / count;
        existing.setMasteryScore(BigDecimal.valueOf(weighted).setScale(4, RoundingMode.HALF_UP));
        existing.setSampleCount(count);
        existing.setLastAssessedAt(LocalDateTime.now());
        knowledgeMasteryDao.updateById(existing);
    }

    // ------------------------------------------------------------------
    // 内部构件
    // ------------------------------------------------------------------

    /** 课程真实选课成员 ID 列表（去重且保持选课顺序） */
    private List<Long> listEnrolledStudentIds(Long courseId) {
        List<Long> raw;
        try {
            raw = courseQueryApi.listStudentUserIdsByCourseId(courseId);
        } catch (Exception e) {
            log.warn("Failed to load enrolled students for course {}: {}", courseId, e.getMessage());
            return new ArrayList<>();
        }
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(raw.stream().filter(Objects::nonNull).toList()));
    }

    private Map<Long, UserBriefVO> safeMapUserBriefs(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, UserBriefVO> map = userQueryApi.mapUserBriefsByIds(studentIds);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to load user briefs: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, MemberOrgBriefVO> safeMapPrimaryClasses(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, MemberOrgBriefVO> map = organizationQueryApi.mapPrimaryClassesByUserIds(null, studentIds);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to retrieve primary classes: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 构建统计上下文，并把原始记录收敛到班级名单范围内。
     *
     * <p>库中存在历史遗留的孤儿数据：管理员账号（如 admin）并非课程选课成员，
     * 却可能在 {@code knowledge_mastery} 与 {@code wrong_question_record} 里留有该课程的数据。
     * 若不收敛，就会出现「班级只有 2 名学员，却显示失分累积 12 次」这类自相矛盾的数字。</p>
     *
     * @param allowedStudentIds 允许进入统计的学员 ID 集合（班级名单）
     */
    private MasteryContext buildContext(Long courseId, List<Long> allowedStudentIds) {
        Set<Long> allowed = new HashSet<>(allowedStudentIds);
        List<KnowledgeMasteryEntity> masteries = knowledgeMasteryDao.listByCourse(courseId).stream()
                .filter(entity -> entity.getStudentId() != null && allowed.contains(entity.getStudentId()))
                .collect(Collectors.toList());
        List<WrongQuestionRecordEntity> wrongRecords = wrongQuestionRecordDao.listByCourse(courseId).stream()
                .filter(record -> record.getStudentId() != null && allowed.contains(record.getStudentId()))
                .collect(Collectors.toList());
        return masteryScoreResolver.buildContext(
                submissionQueryApi.getCourseSubmissionStats(courseId), masteries, wrongRecords);
    }

    /**
     * 过滤管理员/测试账号。判定规则与前端 {@code KnowledgeHeatmap.vue#isTestingAccount} 保持一致，
     * 避免出现「前端隐藏 2 人、后端仍按 4 人算均分」的口径分裂。
     */
    private List<Long> filterTestingAccounts(List<Long> studentIds,
                                             Map<Long, UserBriefVO> userMap,
                                             boolean includeTesting) {
        if (includeTesting || studentIds.isEmpty()) {
            return studentIds;
        }
        List<Long> filtered = studentIds.stream()
                .filter(id -> !isTestingAccount(userMap.get(id)))
                .collect(Collectors.toList());
        // 若整门课都是管理员/测试账号，回退为全量，避免整页空数据
        return filtered.isEmpty() ? studentIds : filtered;
    }

    private static boolean isTestingAccount(UserBriefVO user) {
        if (user == null) {
            return false;
        }
        String realName = user.getRealName() != null ? user.getRealName() : "";
        String username = user.getUsername() != null ? user.getUsername() : "";
        String lowerName = realName.toLowerCase();
        String lowerUsername = username.toLowerCase();
        return realName.contains("管理员")
                || lowerName.contains("admin")
                || lowerUsername.contains("admin");
    }

    /** 构造薄弱考点榜单：口径与指标条的统计完全同源 */
    private List<KnowledgeMasteryVO.WeakPointVO> buildWeakPoints(List<KnowledgePointVO> points,
                                                                Map<Long, String> chapterNameMap,
                                                                List<Integer> scopeScores,
                                                                MasteryContext context,
                                                                Map<Long, Integer> kpMeasuredCountMap,
                                                                Map<Long, Integer> kpBelowThresholdMap,
                                                                int classStudentCount) {
        List<KnowledgeMasteryVO.WeakPointVO> weakPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            KnowledgePointVO point = points.get(i);
            int percent = i < scopeScores.size() && scopeScores.get(i) != null ? scopeScores.get(i) : 0;
            double pointScore = percent / 100.0;
            if (pointScore >= WEAK_POINT_THRESHOLD) {
                continue;
            }

            KnowledgeMasteryVO.WeakPointVO weak = new KnowledgeMasteryVO.WeakPointVO();
            weak.setKnowledgePointId(point.getId());
            weak.setTitle(point.getTitle());
            weak.setChapterName(chapterNameMap.getOrDefault(point.getChapterId(), "核心章节"));
            weak.setMastery(Math.round(pointScore * 1000.0) / 1000.0);
            weak.setWrongCount(context.wrongCountOfPoint(point.getId()));
            // 受影响人数改为真实统计「该考点分值低于 70% 的学员数」，不再按比例反推
            weak.setAffectedStudentCount(kpBelowThresholdMap.getOrDefault(point.getId(), 0));
            weak.setMeasuredStudentCount(kpMeasuredCountMap.getOrDefault(point.getId(), 0));
            weak.setClassStudentCount(classStudentCount);

            int measured = weak.getMeasuredStudentCount();
            if (measured == 0) {
                weak.setDataConfidence("ESTIMATED");
            } else if (measured * 2 >= classStudentCount) {
                weak.setDataConfidence("MEASURED");
            } else {
                weak.setDataConfidence("MIXED");
            }

            weak.setSuggestion(suggestionFor(pointScore));
            weakPoints.add(weak);
        }
        weakPoints.sort(Comparator.comparing(KnowledgeMasteryVO.WeakPointVO::getMastery));
        return weakPoints.stream().limit(WEAK_POINT_LIMIT).collect(Collectors.toList());
    }

    private List<KnowledgeMasteryVO.StudentItemVO> buildStudentRoster(List<Long> studentIds,
                                                                     Map<Long, UserBriefVO> userMap,
                                                                     Map<Long, MemberOrgBriefVO> orgMap,
                                                                     List<KnowledgePointVO> points,
                                                                     MasteryContext context) {
        List<KnowledgeMasteryVO.StudentItemVO> roster = new ArrayList<>();
        for (Long sid : studentIds) {
            UserBriefVO user = userMap.get(sid);
            MemberOrgBriefVO orgBrief = orgMap.get(sid);

            double sum = 0.0;
            int measured = 0;
            for (KnowledgePointVO point : points) {
                ResolvedScore resolved = context.resolve(sid, point);
                sum += resolved.score();
                if (resolved.isMeasured()) {
                    measured++;
                }
            }

            KnowledgeMasteryVO.StudentItemVO item = new KnowledgeMasteryVO.StudentItemVO();
            item.setId(sid);
            item.setName(user != null && user.getRealName() != null ? user.getRealName() : "学员 " + sid);
            item.setUsername(user != null && user.getUsername() != null ? user.getUsername() : "student_" + sid);
            item.setStudentNo(resolveStudentNo(orgBrief, sid));
            item.setAvatar(user != null ? user.getAvatar() : null);
            item.setClassName(orgBrief != null && orgBrief.getName() != null ? orgBrief.getName() : "选课班级");
            item.setMasteryAvg(points.isEmpty() ? 0.0 : Math.round(sum / points.size() * 1000.0) / 10.0);
            item.setMeasuredKpCount(measured);
            item.setEstimatedKpCount(Math.max(0, points.size() - measured));
            roster.add(item);
        }
        roster.sort((a, b) -> Double.compare(b.getMasteryAvg(), a.getMasteryAvg()));
        return roster;
    }

    private String resolveStudentNo(MemberOrgBriefVO orgBrief, Long studentId) {
        if (orgBrief != null && orgBrief.getMemberNo() != null && !orgBrief.getMemberNo().isBlank()) {
            return orgBrief.getMemberNo();
        }
        return "STU-" + String.format("%04d", studentId);
    }

    private String suggestionFor(double pointScore) {
        if (pointScore < 0.50) {
            return "认知盲区严重，建议优先指派前驱概念重温课件并开展 3~5 道变式专项攻坚";
        }
        if (pointScore < 0.60) {
            return "公式推演与边界条件易错，建议派发靶向诊断习题集并安排随堂答疑";
        }
        return "中等偏弱，建议在下周随堂测验中设置变式考题进行达标核验";
    }

    private String buildEstimatingDiagnosis(ResolvedScore resolved, int wrongCount) {
        if (resolved.source() == MasterySource.ESTIMATED) {
            return wrongCount > 0
                    ? "该考点暂无独立测评采样，当前掌握度由作业均分与 " + wrongCount
                    + " 次错题失分推算得出，建议安排一次课内小测以获得真实测评数据"
                    : "该考点暂无独立测评采样，当前掌握度由作业表现推算得出，建议安排一次课内小测以获得真实测评数据";
        }
        if (resolved.score() < 0.55) {
            return "该考点存在较严重的认知断层，建议由浅入深重构核心定义定理，并进行变式训练";
        }
        if (resolved.score() < 0.70) {
            return "解题方法熟练度不足，复杂边界条件与逆向应用易出错，建议进行针对性强化训练";
        }
        return "基础掌握良好，建议提供拓展综合题挑战更高阶应用";
    }

    /** 把某考点全班分值列表折算为百分制整数 */
    private static int toPercent(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0;
        }
        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0;
        return (int) Math.round(avg);
    }

    private static double averageOf(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        int count = 0;
        for (Integer score : scores) {
            if (score != null) {
                sum += score;
                count++;
            }
        }
        return count == 0 ? 0.0 : Math.round((sum * 10.0) / count) / 10.0;
    }

    private Map<String, Object> fillEmptyHeatmap(Map<String, Object> result) {
        result.put("knowledgePoints", Collections.emptyList());
        result.put("students", Collections.emptyList());
        result.put("cells", Collections.emptyList());
        result.put("classAvgScores", Collections.emptyMap());
        result.put("studentCount", 0);
        result.put("classStudentCount", 0);
        result.put("measuredCellCount", 0);
        result.put("estimatedCellCount", 0);
        return result;
    }

    private Map<Long, String> buildChapterNameMap(Long courseId) {
        Map<Long, String> map = new HashMap<>();
        if (courseId == null) {
            return map;
        }
        try {
            List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
            if (chapters != null) {
                flattenChapters(chapters, map);
            }
        } catch (Exception e) {
            log.warn("Failed to load chapters for course {}: {}", courseId, e.getMessage());
        }
        return map;
    }

    private void flattenChapters(List<ChapterTreeVO> chapters, Map<Long, String> map) {
        for (ChapterTreeVO c : chapters) {
            if (c.getId() != null) {
                map.put(c.getId(), c.getTitle());
            }
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                flattenChapters(c.getChildren(), map);
            }
        }
    }
}
