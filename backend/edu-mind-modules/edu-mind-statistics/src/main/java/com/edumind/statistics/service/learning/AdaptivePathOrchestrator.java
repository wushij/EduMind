package com.edumind.statistics.service.learning;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
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
    /**
     * 每周取题时的候选池大小。
     * 取题需要「同一个知识点优先、跨周不重复」，候选池过小（例如 3）会导致多周只能拿到同一道题，
     * 学员看到的就是一周接一周的相同练习，因此这里保留足够宽度供跨周轮换。
     */
    private static final int QUESTION_CANDIDATE_SIZE = 6;
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
        // 章节标题映射 + 根章节列表：课程知识点尚未录入（kpById 为空）时，任务文案与排期必须退化为「章节维度」，
        // 否则每周都会输出同一句通用占位文案并推同一道练习，学员会误以为系统在重复刷同一套假数据。
        Map<Long, String> chapterTitleById = new HashMap<>();
        List<ChapterTreeVO> rootChapters = loadRootChapters(courseId, chapterTitleById);

        List<FocusWeekPlan> weekPlans =
                planWeeks(courseId, studentId, mastery, masteryByKp, kpById, chapterTitleById, rootChapters);
        if (weekPlans.isEmpty()) {
            LearningPathVO baseline = learningPathService.buildPath(courseId);
            weekPlans = fromBaselineWeeks(baseline, masteryByKp, kpById, chapterTitleById);
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
        // 跨周已选题目：保证同一份学习计划内不会连续两周推同一道练习题
        Set<Long> pickedQuestionIds = new HashSet<>();

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
            // 章节维度的降级计划没有知识点，null 进入高亮集合会让后续图切片构建做无意义的整图展开
            if (plan.knowledgePointId != null) {
                highlightKpIds.add(plan.knowledgePointId);
            }

            List<LearningPathVO.LearningPathTaskVO> tasks = buildTasksForWeek(
                    courseId, studentId, plan, masteryByKp, kpById, questionCache, pickedQuestionIds);
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
                                          Map<Long, Double> masteryByKp, Map<Long, KnowledgePointVO> kpById,
                                          Map<Long, String> chapterTitleById, List<ChapterTreeVO> rootChapters) {
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

        // 课程知识点表尚未录入（kpById 为空）时，题库与错题里残留的知识点 ID 无法反查标题，
        // 若继续按知识点排期，只会产出「强化薄弱考点」这类占位主题 + 同一道练习题的重复计划。
        // 因此显式降级为章节维度排期，让每周主题与章节一一对应、可回溯。
        if (focusIds.isEmpty()) {
            return planWeeksByChapter(rootChapters);
        }

        Map<Long, List<Long>> prereqMap =
                knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(focusIds);

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
                    plans.add(new FocusWeekPlan(prereqId, chapterIdOf(kp), chapterTitleOf(kp, chapterTitleById),
                            "补先修：" + title, m * 100,
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
                plans.add(new FocusWeekPlan(focusId, chapterIdOf(kp), chapterTitleOf(kp, chapterTitleById),
                        theme, m * 100, reason));
                scheduled.add(focusId);
            }
            if (plans.size() >= MAX_WEEKS) {
                break;
            }
        }
        return plans;
    }

    /**
     * 课程没有知识点元数据时的降级排期：按章节（根节点）顺序生成周计划。
     * 每周的 chapterId/chapterTitle 都真实可回溯，任务文案与推荐范围因此收敛到该章节。
     */
    private List<FocusWeekPlan> planWeeksByChapter(List<ChapterTreeVO> rootChapters) {
        List<FocusWeekPlan> plans = new ArrayList<>();
        if (rootChapters == null || rootChapters.isEmpty()) {
            return plans;
        }
        List<ChapterTreeVO> ordered = rootChapters.stream()
                .filter(c -> c != null && c.getId() != null && StringUtils.hasText(c.getTitle()))
                .sorted(Comparator.comparing(c -> c.getSort() != null ? c.getSort() : 0))
                .limit(MAX_WEEKS)
                .collect(Collectors.toList());
        for (ChapterTreeVO chapter : ordered) {
            plans.add(new FocusWeekPlan(null, chapter.getId(), chapter.getTitle(), chapter.getTitle(), null,
                    "该课程尚未录入知识点元数据，已按章节维度编排；补齐知识点后将自动切换为按薄弱考点精准排期。"));
        }
        return plans;
    }

    /**
     * 展开章节树，产出「章节ID → 章节标题」映射，并返回根章节列表（供章节维度降级排期使用）。
     */
    private List<ChapterTreeVO> loadRootChapters(Long courseId, Map<Long, String> chapterTitleById) {
        List<ChapterTreeVO> roots = new ArrayList<>();
        collectChapters(courseQueryApi.listChaptersByCourseId(courseId), chapterTitleById, roots);
        return roots;
    }

    private void collectChapters(List<ChapterTreeVO> chapters, Map<Long, String> chapterTitleById,
                                 List<ChapterTreeVO> roots) {
        if (chapters == null) {
            return;
        }
        for (ChapterTreeVO chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            if (chapter.getId() != null && StringUtils.hasText(chapter.getTitle())) {
                chapterTitleById.put(chapter.getId(), chapter.getTitle());
            }
            if (chapter.getParentId() == null || chapter.getParentId() == 0L) {
                roots.add(chapter);
            }
            collectChapters(chapter.getChildren(), chapterTitleById, roots);
        }
    }

    private static Long chapterIdOf(KnowledgePointVO kp) {
        return kp != null ? kp.getChapterId() : null;
    }

    private static String chapterTitleOf(KnowledgePointVO kp, Map<Long, String> chapterTitleById) {
        Long chapterId = chapterIdOf(kp);
        return chapterId != null ? chapterTitleById.get(chapterId) : null;
    }

    private List<FocusWeekPlan> fromBaselineWeeks(LearningPathVO baseline, Map<Long, Double> masteryByKp,
                                                  Map<Long, KnowledgePointVO> kpById,
                                                  Map<Long, String> chapterTitleById) {
        List<FocusWeekPlan> plans = new ArrayList<>();
        if (baseline.getWeeks() == null) {
            return plans;
        }
        for (LearningPathVO.LearningPathWeekVO week : baseline.getWeeks()) {
            // 只接受能在课程知识点表反查到的 ID：基线任务里的 knowledgePointId 可能来自题库标签，
            // 直接沿用会得到「知识点不存在」的周计划（文案退化、推荐题无法按考点收敛）。
            Long kpId = kpById.containsKey(week.getKnowledgePointId()) ? week.getKnowledgePointId() : null;
            if (kpId == null && week.getTasks() != null) {
                for (LearningPathVO.LearningPathTaskVO t : week.getTasks()) {
                    if (t.getKnowledgePointId() != null && kpById.containsKey(t.getKnowledgePointId())) {
                        kpId = t.getKnowledgePointId();
                        break;
                    }
                }
            }
            Long chapterId = resolveBaselineChapterId(week);
            String chapterTitle = chapterId != null ? chapterTitleById.get(chapterId) : week.getTheme();
            double m = kpId != null ? masteryByKp.getOrDefault(kpId, 0.0) : 0.0;
            plans.add(new FocusWeekPlan(kpId, chapterId, chapterTitle,
                    week.getTheme() != null ? week.getTheme() : "基础巩固",
                    m * 100, "按章节进度推荐的学习计划。"));
        }
        return plans;
    }

    /**
     * 基线的「章节学习」任务 refId 即章节 ID，用它回填周计划的章节维度信息。
     */
    private static Long resolveBaselineChapterId(LearningPathVO.LearningPathWeekVO week) {
        if (week.getTasks() == null) {
            return null;
        }
        for (LearningPathVO.LearningPathTaskVO t : week.getTasks()) {
            if ("READ".equalsIgnoreCase(t.getType()) && t.getRefId() != null) {
                return t.getRefId();
            }
        }
        return null;
    }

    private List<LearningPathVO.LearningPathTaskVO> buildTasksForWeek(
            Long courseId, Long studentId, FocusWeekPlan plan, Map<Long, Double> masteryByKp,
            Map<Long, KnowledgePointVO> kpById, Map<Long, RecommendedQuestionVO> questionCache,
            Set<Long> pickedQuestionIds) {
        List<LearningPathVO.LearningPathTaskVO> tasks = new ArrayList<>();
        Long kpId = plan.knowledgePointId;
        KnowledgePointVO kp = kpId != null ? kpById.get(kpId) : null;
        double mastery = kpId != null ? masteryByKp.getOrDefault(kpId, 0.0) : 0.0;
        boolean mastered = mastery >= MASTERED_THRESHOLD;
        // 知识点缺失（课程尚未录入知识点元数据）时，章节标题是本周唯一可回溯的主题来源，
        // 用它替代通用占位文案，保证「第 N 周」的任务描述各不相同。
        String weekTopic = kp != null && StringUtils.hasText(kp.getTitle()) ? kp.getTitle()
                : (StringUtils.hasText(plan.chapterTitle) ? plan.chapterTitle : null);

        LearningPathVO.LearningPathTaskVO read = new LearningPathVO.LearningPathTaskVO();
        read.setId(taskId(kpId, "read"));
        read.setType("READ");
        read.setTypeLabel("章节学习");
        read.setKnowledgePointId(kpId);
        read.setEstimatedMinutes(20);
        Long chapterId = kp != null ? kp.getChapterId() : plan.chapterId;
        read.setRefId(chapterId);
        read.setTitle(weekTopic != null ? "学习章节：" + weekTopic : "学习课程章节内容");
        read.setTargetUrl("/course/" + courseId + "/chapters");
        read.setActionLabel("去学习");
        read.setStatus(mastered ? "COMPLETED" : "PENDING");
        tasks.add(read);

        RecommendedQuestionVO question =
                pickQuestion(courseId, studentId, kp, chapterId, questionCache, pickedQuestionIds);
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
            wb.setTitle(weekTopic != null ? "错题变式攻坚：" + weekTopic : "错题变式攻坚");
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

    private RecommendedQuestionVO pickQuestion(Long courseId, Long studentId, KnowledgePointVO kp, Long chapterId,
                                               Map<Long, RecommendedQuestionVO> questionCache,
                                               Set<Long> pickedQuestionIds) {
        Long kpKey = kp != null && kp.getId() != null ? kp.getId() : null;
        // 仅对「知识点确定」的周做缓存复用：知识点缺失时缓存 key 会退化到同一个分支，
        // 整份计划就会反复取到同一道题，因此这类情况必须逐周重新挑选。
        if (kpKey != null && questionCache.containsKey(kpKey)) {
            return questionCache.get(kpKey);
        }
        List<RecommendedQuestionVO> list = recommendationService.recommendQuestionsForStudent(
                courseId, chapterId, QUESTION_CANDIDATE_SIZE, studentId);
        if (list == null || list.isEmpty()) {
            if (kpKey != null) {
                questionCache.put(kpKey, null);
            }
            return null;
        }
        RecommendedQuestionVO matched = pickUnusedQuestion(list, kpKey, pickedQuestionIds);
        if (matched != null && matched.getId() != null) {
            pickedQuestionIds.add(matched.getId());
        }
        if (kpKey != null) {
            questionCache.put(kpKey, matched);
        }
        return matched;
    }

    /**
     * 优先返回「同知识点 + 尚未被本周计划占用」的题目，其次退让为任意未占用题，最后才允许重复。
     * 这是同一份学习计划内多周练习不重复的关键。
     */
    private static RecommendedQuestionVO pickUnusedQuestion(List<RecommendedQuestionVO> list, Long kpId,
                                                            Set<Long> pickedQuestionIds) {
        for (RecommendedQuestionVO q : list) {
            if (kpId != null && !Objects.equals(q.getKnowledgePointId(), kpId)) {
                continue;
            }
            if (q.getId() == null || !pickedQuestionIds.contains(q.getId())) {
                return q;
            }
        }
        for (RecommendedQuestionVO q : list) {
            if (q.getId() != null && !pickedQuestionIds.contains(q.getId())) {
                return q;
            }
        }
        return list.get(0);
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

    private record FocusWeekPlan(Long knowledgePointId, Long chapterId, String chapterTitle,
                                 String theme, Double masteryPercent, String focusReason) {
    }
}
