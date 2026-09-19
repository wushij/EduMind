package com.edumind.statistics.service.learning.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.learning.AdaptivePathService;
import com.edumind.statistics.service.learning.LearningHomeService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.LearningHomeOverviewVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import com.edumind.teaching.api.StudentAssignmentQueryApi;
import com.edumind.teaching.vo.assignment.StudentAssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningHomeServiceImpl implements LearningHomeService {

    private static final int MAX_WEAK_POINTS = 8;
    private static final int MAX_TODAY_TASKS = 12;

    private final CourseQueryApi courseQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final LearningRecordDao learningRecordDao;
    private final StudentAssignmentQueryApi studentAssignmentQueryApi;
    private final AdaptivePathService adaptivePathService;

    @Override
    public LearningHomeOverviewVO getOverview(Long studentId, Long primaryCourseId) {
        if (studentId == null) {
            throw new BusinessException("未登录，无法加载学习总览");
        }
        LearningHomeOverviewVO overview = new LearningHomeOverviewVO();
        List<Long> courseIds = resolveCourseIds(studentId);
        if (courseIds.isEmpty()) {
            overview.setPrimaryCourseId(null);
            return overview;
        }

        Map<Long, String> courseNames = loadCourseNames(courseIds);
        Long resolvedPrimary = resolvePrimaryCourseId(courseIds, primaryCourseId, studentId);
        overview.setPrimaryCourseId(resolvedPrimary);

        List<LearningHomeOverviewVO.CourseItemVO> courseItems = new ArrayList<>();
        List<LearningHomeOverviewVO.WeakPointItemVO> allWeak = new ArrayList<>();
        int totalStudyMinutes = 0;
        int progressSum = 0;
        int masteryCount = 0;
        double masterySum = 0;

        List<StudentAssignmentVO> allAssignments = studentAssignmentQueryApi.listMine(null);

        for (Long courseId : courseIds) {
            LearningHomeOverviewVO.CourseItemVO item = new LearningHomeOverviewVO.CourseItemVO();
            item.setCourseId(courseId);
            item.setCourseName(courseNames.getOrDefault(courseId, "课程 " + courseId));

            CourseLessonProgressSummaryVO progress = courseQueryApi.getLessonProgressSummary(courseId, studentId);
            int lessonPercent = 0;
            if (progress != null && progress.getTotalLessonCount() != null && progress.getTotalLessonCount() > 0) {
                int completed = progress.getCompletedLessonCount() != null ? progress.getCompletedLessonCount() : 0;
                lessonPercent = (int) Math.round(completed * 100.0 / progress.getTotalLessonCount());
            }
            item.setLessonProgressPercent(lessonPercent);
            progressSum += lessonPercent;

            KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
            double courseMastery = averagePersonalMastery(mastery);
            item.setOverallMastery(Math.round(courseMastery * 10.0) / 10.0);
            if (!mastery.getPersonal().isEmpty()) {
                masterySum += courseMastery;
                masteryCount++;
            }

            int pending = (int) allAssignments.stream()
                    .filter(a -> Objects.equals(a.getCourseId(), courseId))
                    .filter(a -> !isAssignmentDone(a))
                    .count();
            item.setPendingAssignmentCount(pending);

            courseItems.add(item);
            totalStudyMinutes += learningRecordDao.getTotalDuration(courseId, studentId);

            for (KnowledgeMasteryVO.WeakPointVO wp : mastery.getWeakPoints()) {
                LearningHomeOverviewVO.WeakPointItemVO weak = new LearningHomeOverviewVO.WeakPointItemVO();
                weak.setKnowledgePointId(wp.getKnowledgePointId());
                weak.setTitle(wp.getTitle());
                weak.setCourseId(courseId);
                weak.setCourseName(item.getCourseName());
                double pct = wp.getMastery() != null ? wp.getMastery() * 100 : 0;
                weak.setMastery(Math.round(pct * 10.0) / 10.0);
                weak.setSuggestion(wp.getSuggestion());
                weak.setLevel(pct < 50 ? "danger" : "warning");
                allWeak.add(weak);
            }
        }

        overview.setCourses(courseItems);
        allWeak.sort(Comparator.comparing(LearningHomeOverviewVO.WeakPointItemVO::getMastery));
        overview.setWeakPoints(allWeak.stream().limit(MAX_WEAK_POINTS).collect(Collectors.toList()));

        List<LearningHomeOverviewVO.TodayTaskVO> tasks = buildTodayTasks(allAssignments, resolvedPrimary, studentId, courseNames);
        overview.setTodayTasks(tasks.stream().limit(MAX_TODAY_TASKS).collect(Collectors.toList()));

        LearningHomeOverviewVO.SummaryVO summary = overview.getSummary();
        summary.setTotalStudyMinutes(totalStudyMinutes);
        summary.setAvgCourseProgressPercent(courseIds.isEmpty() ? 0 : progressSum / courseIds.size());
        summary.setOverallMasteryPercent(masteryCount == 0 ? 0.0 : Math.round(masterySum / masteryCount * 10.0) / 10.0);
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        summary.setCompletedTasks((int) completed);
        summary.setTotalTasks(tasks.size());

        return overview;
    }

    private List<Long> resolveCourseIds(Long studentId) {
        Set<Long> ids = new LinkedHashSet<>();
        List<CourseBriefVO> recent = courseQueryApi.listRecentCourses(20);
        if (recent != null) {
            for (CourseBriefVO brief : recent) {
                if (brief.getId() != null && courseQueryApi.isCourseMember(brief.getId(), studentId)) {
                    ids.add(brief.getId());
                }
            }
        }
        ids.addAll(courseQueryApi.listCourseIdsByUserId(studentId));
        return new ArrayList<>(ids);
    }

    private Map<Long, String> loadCourseNames(List<Long> courseIds) {
        Map<Long, String> names = new HashMap<>();
        List<CourseVO> courses = courseQueryApi.listCoursesByIds(courseIds);
        if (courses != null) {
            for (CourseVO c : courses) {
                names.put(c.getId(), c.getName());
            }
        }
        return names;
    }

    private Long resolvePrimaryCourseId(List<Long> courseIds, Long requested, Long studentId) {
        if (requested != null && courseIds.contains(requested)
                && courseQueryApi.isCourseMember(requested, studentId)) {
            return requested;
        }
        List<CourseBriefVO> recent = courseQueryApi.listRecentCourses(5);
        if (recent != null) {
            for (CourseBriefVO brief : recent) {
                if (brief.getId() != null && courseIds.contains(brief.getId())) {
                    return brief.getId();
                }
            }
        }
        return courseIds.get(0);
    }

    private double averagePersonalMastery(KnowledgeMasteryVO mastery) {
        if (mastery.getPersonal() == null || mastery.getPersonal().isEmpty()) {
            return 0;
        }
        return mastery.getPersonal().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private boolean isAssignmentDone(StudentAssignmentVO a) {
        String status = a.getMySubmissionStatus();
        return "SUBMITTED".equals(status) || "GRADED".equals(status);
    }

    private List<LearningHomeOverviewVO.TodayTaskVO> buildTodayTasks(
            List<StudentAssignmentVO> assignments,
            Long primaryCourseId,
            Long studentId,
            Map<Long, String> courseNames) {
        List<LearningHomeOverviewVO.TodayTaskVO> tasks = new ArrayList<>();
        LocalDateTime weekLater = LocalDateTime.now().plusDays(7);

        List<StudentAssignmentVO> sorted = assignments.stream()
                .sorted(Comparator.comparing(a -> a.getDeadline() == null ? LocalDateTime.MAX : a.getDeadline()))
                .collect(Collectors.toList());

        for (StudentAssignmentVO a : sorted) {
            if (a.getDeadline() != null && a.getDeadline().isAfter(weekLater) && isAssignmentDone(a)) {
                continue;
            }
            LearningHomeOverviewVO.TodayTaskVO task = new LearningHomeOverviewVO.TodayTaskVO();
            task.setId("assignment-" + a.getId());
            task.setTitle(a.getTitle());
            task.setCourseId(a.getCourseId());
            task.setCourseName(a.getCourseName() != null ? a.getCourseName()
                    : courseNames.getOrDefault(a.getCourseId(), "课程作业"));
            task.setType("ASSIGNMENT");
            task.setEstimatedMinutes(30);
            boolean done = isAssignmentDone(a);
            task.setStatus(done ? "COMPLETED" : "PENDING");
            task.setTargetUrl(done
                    ? "/learning/assignments/" + a.getId() + "/result"
                    : "/learning/assignments/" + a.getId() + "/take");
            tasks.add(task);
        }

        if (primaryCourseId != null) {
            LearningPathVO path = adaptivePathService.buildAdaptivePath(primaryCourseId, studentId);
            if (path.getWeeks() != null && !path.getWeeks().isEmpty()) {
                LearningPathVO.LearningPathWeekVO firstWeek = path.getWeeks().get(0);
                String courseName = courseNames.getOrDefault(primaryCourseId, "当前课程");
                int idx = 0;
                for (LearningPathVO.LearningPathTaskVO pt : firstWeek.getTasks()) {
                    if (!"PENDING".equalsIgnoreCase(pt.getStatus())
                            && !"IN_PROGRESS".equalsIgnoreCase(pt.getStatus())) {
                        continue;
                    }
                    LearningHomeOverviewVO.TodayTaskVO task = new LearningHomeOverviewVO.TodayTaskVO();
                    task.setId("path-" + primaryCourseId + "-" + idx++);
                    task.setTitle(pt.getTitle());
                    task.setCourseId(primaryCourseId);
                    task.setCourseName(courseName);
                    task.setType(pt.getType() != null ? pt.getType() : "PRACTICE");
                    task.setEstimatedMinutes(pt.getEstimatedMinutes() != null ? pt.getEstimatedMinutes() : 15);
                    task.setStatus("PENDING");
                    String url = pt.getTargetUrl();
                    task.setTargetUrl(StringUtils.hasText(url)
                            ? url
                            : "/learning/practice?courseId=" + primaryCourseId);
                    tasks.add(task);
                    break;
                }
            }
        }
        return tasks;
    }
}
