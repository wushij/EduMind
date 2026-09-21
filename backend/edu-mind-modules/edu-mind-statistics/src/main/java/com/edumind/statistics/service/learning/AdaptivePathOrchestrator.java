package com.edumind.statistics.service.learning;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgeGraphQueryApi;
import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import com.edumind.statistics.api.KnowledgeMasteryQueryApi;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.LearningPathDetailVO;
import com.edumind.statistics.vo.learning.LearningPathStudentItemVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

@Component
@RequiredArgsConstructor
public class AdaptivePathOrchestrator {

    private static final double MASTERED_THRESHOLD = 0.7;
    private static final int MAX_WEEKS = 4;
    private static final DateTimeFormatter GENERATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;
    private final RecommendationService recommendationService;
    private final LearningPathService learningPathService;
    private final KnowledgePointRelationCommandApi knowledgePointRelationCommandApi;
    private final KnowledgeGraphQueryApi knowledgeGraphQueryApi;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final UserQueryApi userQueryApi;
    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;

    public LearningPathVO buildAdaptivePath(Long courseId, Long studentId) {
        LearningPathDetailVO detail = buildDetail(courseId, studentId, false);
        return toSummaryPath(detail);
    }

    public LearningPathDetailVO buildDetail(Long courseId, Long studentId) {
        return buildDetail(courseId, studentId, true);
    }

    public LearningPathDetailVO buildDetail(Long courseId, Long studentId, boolean includeGraphSlice) {
        if (courseId == null || studentId == null) {
            throw new BusinessException("课程或学员参数无效");
        }
        if (!courseQueryApi.isCourseMember(courseId, studentId)) {
            throw new BusinessException("该学员未加入本课程，无法生成学习路径");
        }

        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
        Map<Long, Double> masteryByKp = loadFullMasteryMap(courseId, studentId, mastery);
        List<KnowledgePointVO> allKps = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        Map<Long, KnowledgePointVO> kpById = allKps.stream()
                .filter(k -> k.getId() != null)
                .collect(Collectors.toMap(KnowledgePointVO::getId, k -> k, (a, b) -> a));

        List<FocusWeekPlan> weekPlans = planWeeks(courseId, studentId, mastery, masteryByKp, kpById);
        if (weekPlans.isEmpty()) {
            LearningPathVO baseline = learningPathService.buildPath(courseId);
            weekPlans = fromBaselineWeeks(baseline, masteryByKp, kpById);
        }

        LearningPathDetailVO detail = new LearningPathDetailVO();
        detail.setCourseId(courseId);
        detail.setStudentId(studentId);
        detail.setCourseName(resolveCourseName(courseId));
        detail.setTitle("自适应推荐学习路径");
        detail.setGeneratedAt(LocalDateTime.now().format(GENERATED_FMT));

        int weekNo = 1;
        int totalTasks = 0;
        int completedTasks = 0;
        int estimatedMinutes = 0;
        Set<Long> highlightKpIds = new LinkedHashSet<>();
        Map<Long, RecommendedQuestionVO> questionCache = new HashMap<>();

        for (FocusWeekPlan plan : weekPlans) {
            if (weekNo > MAX_WEEKS) {
                break;
            }
            LearningPathVO.LearningPathWeekVO week = new LearningPathVO.LearningPathWeekVO();
            week.setWeekNo(weekNo++);
            week.setTheme(plan.theme);
            week.setKnowledgePointId(plan.knowledgePointId);
            week.setMasteryPercent(plan.masteryPercent);
            week.setFocusReason(plan.focusReason);
            highlightKpIds.add(plan.knowledgePointId);

            List<LearningPathVO.LearningPathTaskVO> tasks = buildTasksForWeek(
                    courseId, studentId, plan, masteryByKp, kpById, questionCache);
            week.setTasks(tasks);
            detail.getWeeks().add(week);

            for (LearningPathVO.LearningPathTaskVO task : tasks) {
                totalTasks++;
                estimatedMinutes += task.getEstimatedMinutes() != null ? task.getEstimatedMinutes() : 0;
                if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                    completedTasks++;
                }
            }
        }

        detail.setWeakPointCount(mastery.getWeakPoints() != null ? mastery.getWeakPoints().size() : 0);
        detail.setGraphGapCount(countGraphGaps(masteryByKp, weekPlans));
        detail.setEstimatedTotalMinutes(estimatedMinutes);
        detail.setOverallProgressPercent(totalTasks == 0 ? 0
                : (int) Math.round(completedTasks * 100.0 / totalTasks));

        detail.setWeakPointsBrief(buildWeakBriefs(mastery));
        if (includeGraphSlice && !highlightKpIds.isEmpty()) {
            detail.setGraphSlice(buildGraphSlice(courseId, highlightKpIds, masteryByKp, kpById));
        } else {
            detail.setGraphSlice(new LearningPathDetailVO.GraphSliceVO());
        }
        detail.setInterpretHint("路径基于薄弱考点、先修关系与推荐练习自动生成，完成任务将同步更新掌握度。");
        return detail;
    }

    public List<LearningPathStudentItemVO> listCourseStudents(Long courseId) {
        if (courseId == null) {
            return List.of();
        }
        List<Long> ids = courseQueryApi.listStudentUserIdsByCourseId(courseId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // 批量补全学生用户信息，避免逐学生跨模块查询（每人 3 次 DB）造成 N+1
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(ids);
        List<LearningPathStudentItemVO> list = new ArrayList<>();
        for (Long sid : ids) {
            LearningPathStudentItemVO item = new LearningPathStudentItemVO();
            item.setStudentId(sid);
            UserBriefVO user = userMap.get(sid);
            if (user != null) {
                item.setUsername(user.getUsername());
                item.setRealName(user.getRealName());
            }
            list.add(item);
        }
        return list;
    }

    private List<FocusWeekPlan> planWeeks(Long courseId, Long studentId, KnowledgeMasteryVO mastery,
                                          Map<Long, Double> masteryByKp, Map<Long, KnowledgePointVO> kpById) {
        List<KnowledgeMasteryVO.WeakPointVO> weak = new ArrayList<>(mastery.getWeakPoints());
        weak.sort(Comparator.comparing(wp -> wp.getMastery() != null ? wp.getMastery() : 1.0));

        // 该课程尚无任何掌握度数据（还没有作业/测评批改记录）：
        // 此时并不是“识别到了薄弱点”，而是回退到按章节顺序生成入门计划。
        // 文案必须如实说明，否则学生会误以为系统已经做过归因、进而怀疑数据是假的。
        boolean noMasteryData = weak.isEmpty();

        List<Long> focusIds = new ArrayList<>();
        for (KnowledgeMasteryVO.WeakPointVO wp : weak) {
            if (wp.getKnowledgePointId() != null && focusIds.size() < MAX_WEEKS) {
                focusIds.add(wp.getKnowledgePointId());
            }
        }
        if (focusIds.isEmpty() && !kpById.isEmpty()) {
            kpById.values().stream()
                    .sorted(Comparator.comparing(k -> k.getSort() != null ? k.getSort() : 0))
                    .limit(2)
                    .map(KnowledgePointVO::getId)
                    .forEach(focusIds::add);
        }

        Map<Long, List<Long>> prereqMap = focusIds.isEmpty() ? Map.of()
                : knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(focusIds);

        List<FocusWeekPlan> plans = new ArrayList<>();
        Set<Long> scheduled = new HashSet<>();

        for (Long focusId : focusIds) {
            List<Long> prereqs = prereqMap.getOrDefault(focusId, List.of());
            for (Long prereqId : prereqs) {
                if (scheduled.contains(prereqId)) {
                    continue;
                }
                double m = masteryByKp.getOrDefault(prereqId, 0.0);
                if (m < MASTERED_THRESHOLD) {
                    KnowledgePointVO kp = kpById.get(prereqId);
                    String title = kp != null ? kp.getTitle() : "先修知识点";
                    plans.add(new FocusWeekPlan(prereqId, "补先修：" + title, m * 100,
                            "先修掌握不足，建议先完成本周任务再进入后续考点。"));
                    scheduled.add(prereqId);
                }
            }
            if (!scheduled.contains(focusId)) {
                KnowledgePointVO kp = kpById.get(focusId);
                double m = masteryByKp.getOrDefault(focusId, 0.0);
                String theme;
                String reason;
                if (noMasteryData) {
                    // 无掌握度数据：如实标注为入门计划，不再冒充“薄弱点归因”结论
                    theme = kp != null ? "起步：" + kp.getTitle() : "课程入门";
                    reason = "该课程暂无掌握度数据，先按课程章节顺序安排入门任务；完成作业或测评后将自动改为按薄弱考点精准排期。";
                } else {
                    theme = kp != null ? "强化：" + kp.getTitle() : "强化薄弱考点";
                    reason = weak.stream()
                            .filter(w -> Objects.equals(w.getKnowledgePointId(), focusId))
                            .map(KnowledgeMasteryVO.WeakPointVO::getSuggestion)
                            .filter(StringUtils::hasText)
                            .findFirst()
                            .orElse("根据测评与错题数据识别的薄弱项。");
                }
                plans.add(new FocusWeekPlan(focusId, theme, m * 100, reason));
                scheduled.add(focusId);
            }
            if (plans.size() >= MAX_WEEKS) {
                break;
            }
        }
        return plans;
    }

    private List<FocusWeekPlan> fromBaselineWeeks(LearningPathVO baseline, Map<Long, Double> masteryByKp,
                                                  Map<Long, KnowledgePointVO> kpById) {
        List<FocusWeekPlan> plans = new ArrayList<>();
        if (baseline.getWeeks() == null) {
            return plans;
        }
        for (LearningPathVO.LearningPathWeekVO week : baseline.getWeeks()) {
            Long kpId = week.getKnowledgePointId();
            if (kpId == null && week.getTasks() != null) {
                for (LearningPathVO.LearningPathTaskVO t : week.getTasks()) {
                    if (t.getKnowledgePointId() != null) {
                        kpId = t.getKnowledgePointId();
                        break;
                    }
                }
            }
            if (kpId == null && !kpById.isEmpty()) {
                kpId = kpById.values().iterator().next().getId();
            }
            double m = kpId != null ? masteryByKp.getOrDefault(kpId, 0.0) : 0.0;
            plans.add(new FocusWeekPlan(kpId, week.getTheme() != null ? week.getTheme() : "基础巩固",
                    m * 100, "按章节进度推荐的学习计划。"));
        }
        return plans;
    }

    private List<LearningPathVO.LearningPathTaskVO> buildTasksForWeek(
            Long courseId, Long studentId, FocusWeekPlan plan, Map<Long, Double> masteryByKp,
            Map<Long, KnowledgePointVO> kpById, Map<Long, RecommendedQuestionVO> questionCache) {
        List<LearningPathVO.LearningPathTaskVO> tasks = new ArrayList<>();
        Long kpId = plan.knowledgePointId;
        KnowledgePointVO kp = kpId != null ? kpById.get(kpId) : null;
        double mastery = kpId != null ? masteryByKp.getOrDefault(kpId, 0.0) : 0.0;
        boolean mastered = mastery >= MASTERED_THRESHOLD;

        LearningPathVO.LearningPathTaskVO read = new LearningPathVO.LearningPathTaskVO();
        read.setId(taskId(kpId, "read"));
        read.setType("READ");
        read.setTypeLabel("章节学习");
        read.setKnowledgePointId(kpId);
        read.setEstimatedMinutes(20);
        Long chapterId = kp != null ? kp.getChapterId() : null;
        read.setRefId(chapterId);
        read.setTitle(kp != null ? "学习章节：" + kp.getTitle() : "学习课程章节内容");
        read.setTargetUrl("/course/" + courseId + "/chapters");
        read.setActionLabel("去学习");
        read.setStatus(mastered ? "COMPLETED" : "PENDING");
        tasks.add(read);

        RecommendedQuestionVO question = pickQuestion(courseId, studentId, kp, questionCache);
        if (question != null) {
            LearningPathVO.LearningPathTaskVO practice = new LearningPathVO.LearningPathTaskVO();
            practice.setId(taskId(kpId, "practice"));
            practice.setType("PRACTICE");
            practice.setTypeLabel("巩固练习");
            practice.setKnowledgePointId(kpId);
            practice.setRefId(question.getId());
            practice.setEstimatedMinutes(15);
            practice.setTitle("巩固练习：" + truncate(question.getStem(), 48));
            practice.setTargetUrl("/learning/practice?courseId=" + courseId
                    + (kpId != null ? "&knowledgePointId=" + kpId : ""));
            practice.setActionLabel("开始练习");
            practice.setStatus(mastered ? "COMPLETED" : "PENDING");
            tasks.add(practice);
        }

        WrongQuestionRecordEntity wrong = kpId != null
                ? wrongQuestionRecordDao.findByStudentCourseAndKp(studentId, courseId, kpId) : null;
        if (wrong != null && (wrong.getStatus() == null || wrong.getStatus() == 0)) {
            LearningPathVO.LearningPathTaskVO wb = new LearningPathVO.LearningPathTaskVO();
            wb.setId(taskId(kpId, "wrong"));
            wb.setType("WRONG_BOOK");
            wb.setTypeLabel("错题攻坚");
            wb.setKnowledgePointId(kpId);
            wb.setRefId(wrong.getId());
            wb.setEstimatedMinutes(12);
            wb.setTitle("错题变式攻坚");
            wb.setTargetUrl("/learning/wrong-questions?courseId=" + courseId
                    + (kpId != null ? "&knowledgePointId=" + kpId : ""));
            wb.setActionLabel("去错题本");
            wb.setStatus("PENDING");
            tasks.add(wb);
        }

        if (kp != null && StringUtils.hasText(kp.getTitle())) {
            LearningPathVO.LearningPathTaskVO ai = new LearningPathVO.LearningPathTaskVO();
            ai.setId(taskId(kpId, "ai"));
            ai.setType("AI_CHAT");
            ai.setTypeLabel("AI 讲解");
            ai.setKnowledgePointId(kpId);
            ai.setEstimatedMinutes(10);
            ai.setTitle("AI 讲解巩固：" + kp.getTitle());
            String encoded = URLEncoder.encode(kp.getTitle(), StandardCharsets.UTF_8);
            ai.setTargetUrl("/course/" + courseId + "/ai?knowledgePoint=" + encoded);
            ai.setActionLabel("问 AI");
            ai.setStatus(mastered ? "COMPLETED" : "PENDING");
            tasks.add(ai);
        }

        return tasks;
    }

    private RecommendedQuestionVO pickQuestion(Long courseId, Long studentId, KnowledgePointVO kp,
                                               Map<Long, RecommendedQuestionVO> questionCache) {
        Long cacheKey = kp != null && kp.getId() != null ? kp.getId() : -1L;
        if (questionCache.containsKey(cacheKey)) {
            return questionCache.get(cacheKey);
        }
        Long chapterId = kp != null ? kp.getChapterId() : null;
        List<RecommendedQuestionVO> list = recommendationService.recommendQuestionsForStudent(
                courseId, chapterId, 3, studentId);
        if (list == null || list.isEmpty()) {
            questionCache.put(cacheKey, null);
            return null;
        }
        RecommendedQuestionVO matched = list.get(0);
        if (kp != null && kp.getId() != null) {
            for (RecommendedQuestionVO q : list) {
                if (Objects.equals(q.getKnowledgePointId(), kp.getId())) {
                    matched = q;
                    break;
                }
            }
        }
        questionCache.put(cacheKey, matched);
        return matched;
    }

    private LearningPathDetailVO.GraphSliceVO buildGraphSlice(
            Long courseId, Set<Long> highlightKpIds, Map<Long, Double> masteryByKp,
            Map<Long, KnowledgePointVO> kpById) {
        LearningPathDetailVO.GraphSliceVO slice = new LearningPathDetailVO.GraphSliceVO();
        CourseDetailVO course = courseQueryApi.getCourseById(courseId);
        Long kbId = course != null ? course.getKnowledgeBaseId() : null;
        if (kbId == null) {
            for (Long kpId : highlightKpIds) {
                if (kpId == null) {
                    continue;
                }
                KnowledgePointVO kp = kpById.get(kpId);
                LearningPathDetailVO.GraphNodeVO node = new LearningPathDetailVO.GraphNodeVO();
                node.setId("kp-" + kpId);
                node.setType("KNOWLEDGE_POINT");
                node.setRefId(kpId);
                node.setLabel(kp != null ? kp.getTitle() : "考点 " + kpId);
                node.setMasteryPercent(masteryByKp.getOrDefault(kpId, 0.0) * 100);
                node.setStatus(nodeStatus(masteryByKp.getOrDefault(kpId, 0.0)));
                slice.getNodes().add(node);
                slice.getHighlightNodeIds().add(node.getId());
            }
            return slice;
        }

        KnowledgeGraphVO graph = knowledgeGraphQueryApi.buildGraph(kbId, 2, List.of("prerequisite", "related"));
        if (graph == null || graph.getNodes() == null) {
            return slice;
        }

        Set<String> includeNodeIds = new HashSet<>();
        for (KnowledgeGraphVO.GraphNodeVO n : graph.getNodes()) {
            if (n.getRefId() != null && highlightKpIds.contains(n.getRefId())) {
                includeNodeIds.add(n.getId());
            }
        }
        if (graph.getEdges() != null) {
            boolean expanded = true;
            while (expanded) {
                expanded = false;
                for (KnowledgeGraphVO.GraphEdgeVO e : graph.getEdges()) {
                    if (includeNodeIds.contains(e.getSource()) && !includeNodeIds.contains(e.getTarget())) {
                        includeNodeIds.add(e.getTarget());
                        expanded = true;
                    }
                    if (includeNodeIds.contains(e.getTarget()) && !includeNodeIds.contains(e.getSource())) {
                        includeNodeIds.add(e.getSource());
                        expanded = true;
                    }
                }
            }
        }

        Map<String, LearningPathDetailVO.GraphNodeVO> nodeMap = new LinkedHashMap<>();
        for (KnowledgeGraphVO.GraphNodeVO n : graph.getNodes()) {
            if (!includeNodeIds.contains(n.getId())) {
                continue;
            }
            LearningPathDetailVO.GraphNodeVO out = new LearningPathDetailVO.GraphNodeVO();
            out.setId(n.getId());
            out.setLabel(n.getLabel());
            out.setType(n.getType());
            out.setRefId(n.getRefId());
            if (n.getRefId() != null) {
                double m = masteryByKp.getOrDefault(n.getRefId(), 0.0);
                out.setMasteryPercent(m * 100);
                out.setStatus(nodeStatus(m));
            }
            nodeMap.put(n.getId(), out);
            if (n.getRefId() != null && highlightKpIds.contains(n.getRefId())) {
                slice.getHighlightNodeIds().add(n.getId());
            }
        }
        slice.getNodes().addAll(nodeMap.values());

        int edgeIdx = 0;
        if (graph.getEdges() != null) {
            for (KnowledgeGraphVO.GraphEdgeVO e : graph.getEdges()) {
                if (!includeNodeIds.contains(e.getSource()) || !includeNodeIds.contains(e.getTarget())) {
                    continue;
                }
                LearningPathDetailVO.GraphEdgeVO edge = new LearningPathDetailVO.GraphEdgeVO();
                edge.setId("e-" + edgeIdx++);
                edge.setSource(e.getSource());
                edge.setTarget(e.getTarget());
                edge.setRelation(e.getRelation());
                slice.getEdges().add(edge);
                if (slice.getHighlightNodeIds().contains(e.getSource())
                        && slice.getHighlightNodeIds().contains(e.getTarget())) {
                    slice.getPathEdgeIds().add(edge.getId());
                }
            }
        }
        return slice;
    }

    private List<LearningPathDetailVO.WeakPointBriefVO> buildWeakBriefs(KnowledgeMasteryVO mastery) {
        List<LearningPathDetailVO.WeakPointBriefVO> list = new ArrayList<>();
        if (mastery.getWeakPoints() == null) {
            return list;
        }
        for (KnowledgeMasteryVO.WeakPointVO wp : mastery.getWeakPoints()) {
            LearningPathDetailVO.WeakPointBriefVO b = new LearningPathDetailVO.WeakPointBriefVO();
            b.setKnowledgePointId(wp.getKnowledgePointId());
            b.setTitle(wp.getTitle());
            b.setMasteryPercent(wp.getMastery() != null ? wp.getMastery() * 100 : 0);
            b.setSuggestion(wp.getSuggestion());
            list.add(b);
        }
        return list;
    }

    private LearningPathVO toSummaryPath(LearningPathDetailVO detail) {
        LearningPathVO path = new LearningPathVO();
        path.setCourseId(detail.getCourseId());
        path.setTitle(detail.getTitle());
        path.setWeeks(detail.getWeeks());
        return path;
    }

    private Map<Long, Double> buildMasteryMap(KnowledgeMasteryVO mastery) {
        Map<Long, Double> map = new HashMap<>();
        if (mastery.getWeakPoints() != null) {
            for (KnowledgeMasteryVO.WeakPointVO wp : mastery.getWeakPoints()) {
                if (wp.getKnowledgePointId() != null) {
                    map.put(wp.getKnowledgePointId(), wp.getMastery() != null ? wp.getMastery() : 0.0);
                }
            }
        }
        return map;
    }

    private Map<Long, Double> loadFullMasteryMap(Long courseId, Long studentId, KnowledgeMasteryVO mastery) {
        Map<Long, Double> map = new HashMap<>(buildMasteryMap(mastery));
        Map<Long, Double> full = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId);
        if (full != null) {
            map.putAll(full);
        }
        return map;
    }

    private int countGraphGaps(Map<Long, Double> masteryByKp, List<FocusWeekPlan> plans) {
        int gaps = 0;
        for (FocusWeekPlan p : plans) {
            if (p.knowledgePointId != null && masteryByKp.getOrDefault(p.knowledgePointId, 0.0) < MASTERED_THRESHOLD) {
                gaps++;
            }
        }
        return gaps;
    }

    private String resolveCourseName(Long courseId) {
        var list = courseQueryApi.listCoursesByIds(List.of(courseId));
        if (list != null && !list.isEmpty() && list.get(0).getName() != null) {
            return list.get(0).getName();
        }
        return "当前课程";
    }

    private static String taskId(Long kpId, String suffix) {
        return "kp-" + (kpId != null ? kpId : "0") + "-" + suffix;
    }

    private static String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return "推荐练习";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private static String nodeStatus(double mastery) {
        if (mastery >= MASTERED_THRESHOLD) {
            return "MASTERED";
        }
        if (mastery >= 0.4) {
            return "LEARNING";
        }
        return "WEAK";
    }

    private record FocusWeekPlan(Long knowledgePointId, String theme, Double masteryPercent, String focusReason) {
    }
}
