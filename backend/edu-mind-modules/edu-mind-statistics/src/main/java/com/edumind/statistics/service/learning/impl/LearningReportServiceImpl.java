package com.edumind.statistics.service.learning.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.learning.LearningHomeService;
import com.edumind.statistics.service.learning.LearningReportService;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;
import com.edumind.statistics.vo.learning.LearningHomeOverviewVO;
import com.edumind.statistics.vo.learning.LearningReportVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LearningReportServiceImpl implements LearningReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LearningAnalyticsService learningAnalyticsService;
    private final LearningHomeService learningHomeService;
    private final CourseQueryApi courseQueryApi;
    private final LearningRecordDao learningRecordDao;
    private final SubmissionQueryApi submissionQueryApi;

    @Override
    public LearningReportVO getReport(Long studentId, Long courseId, String range) {
        if (studentId == null) {
            throw new BusinessException("未登录，无法加载学习报告");
        }
        String resolvedRange = range != null && !range.isBlank() ? range : "30d";
        List<Long> enrolledIds = resolveEnrolledCourseIds(studentId);
        if (enrolledIds.isEmpty()) {
            LearningReportVO empty = new LearningReportVO();
            empty.setEnrolledCourses(List.of());
            return empty;
        }

        Long resolvedCourseId = resolveCourseId(enrolledIds, courseId, studentId);
        if (!courseQueryApi.isCourseMember(resolvedCourseId, studentId)) {
            throw new BusinessException("您尚未加入该课程，无法查看学情报告");
        }

        LearningReportVO vo = new LearningReportVO();
        vo.setCourseId(resolvedCourseId);
        vo.setCourseName(loadCourseName(resolvedCourseId));
        vo.setEnrolledCourses(buildEnrolledCourseItems(enrolledIds));

        StudentPortraitVO portrait = learningAnalyticsService.getStudentPortrait(resolvedCourseId, studentId, resolvedRange);
        vo.setPortrait(portrait);
        vo.setTrends(buildPersonalTrends(resolvedCourseId, studentId, resolvedRange));
        return vo;
    }

    private List<Long> resolveEnrolledCourseIds(Long studentId) {
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

    private Long resolveCourseId(List<Long> enrolledIds, Long requested, Long studentId) {
        if (requested != null && enrolledIds.contains(requested)) {
            return requested;
        }
        LearningHomeOverviewVO overview = learningHomeService.getOverview(studentId, requested);
        if (overview.getPrimaryCourseId() != null && enrolledIds.contains(overview.getPrimaryCourseId())) {
            return overview.getPrimaryCourseId();
        }
        return enrolledIds.get(0);
    }

    private List<LearningReportVO.EnrolledCourseItemVO> buildEnrolledCourseItems(List<Long> courseIds) {
        Map<Long, String> names = loadCourseNames(courseIds);
        List<LearningReportVO.EnrolledCourseItemVO> items = new ArrayList<>();
        for (Long id : courseIds) {
            LearningReportVO.EnrolledCourseItemVO item = new LearningReportVO.EnrolledCourseItemVO();
            item.setCourseId(id);
            item.setCourseName(names.getOrDefault(id, "课程 " + id));
            items.add(item);
        }
        return items;
    }

    private String loadCourseName(Long courseId) {
        return loadCourseNames(List.of(courseId)).getOrDefault(courseId, "课程 " + courseId);
    }

    private Map<Long, String> loadCourseNames(List<Long> courseIds) {
        Map<Long, String> names = new HashMap<>();
        List<CourseVO> courses = courseQueryApi.listCoursesByIds(courseIds);
        if (courses != null) {
            for (CourseVO c : courses) {
                if (c.getId() != null) {
                    names.put(c.getId(), c.getName());
                }
            }
        }
        return names;
    }

    private LearningReportVO.LearningReportTrendsVO buildPersonalTrends(Long courseId, Long studentId, String range) {
        LocalDateTime since = resolveSince(range);
        LocalDate start = since != null ? since.toLocalDate() : LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        List<LearningRecordEntity> records = learningRecordDao.listByCourseAndStudentSince(courseId, studentId, since);
        Map<LocalDate, Integer> minutesByDay = new HashMap<>();
        for (LearningRecordEntity record : records) {
            if (record.getCreateTime() == null) {
                continue;
            }
            LocalDate day = record.getCreateTime().toLocalDate();
            int minutes = record.getDurationMinutes() != null ? record.getDurationMinutes() : 0;
            minutesByDay.merge(day, minutes, Integer::sum);
        }

        LearningReportVO.LearningReportTrendsVO trends = new LearningReportVO.LearningReportTrendsVO();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LearningReportVO.DailyStudyMinutesVO point = new LearningReportVO.DailyStudyMinutesVO();
            point.setDate(d.format(DATE_FMT));
            point.setMinutes(minutesByDay.getOrDefault(d, 0));
            trends.getStudyMinutesByDate().add(point);
        }

        SubmissionStatsVO stats = submissionQueryApi.getCourseSubmissionStats(courseId);
        double flatScore = 0;
        if (stats.getStudentScores() != null) {
            flatScore = stats.getStudentScores().stream()
                    .filter(s -> studentId.equals(s.getStudentId()))
                    .map(SubmissionStatsVO.StudentScoreVO::getAvgScore)
                    .filter(score -> score != null)
                    .findFirst()
                    .orElse(stats.getAvgScore() != null ? stats.getAvgScore() : 0.0);
        } else if (stats.getAvgScore() != null) {
            flatScore = stats.getAvgScore();
        }
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LearningReportVO.DailyScoreVO sp = new LearningReportVO.DailyScoreVO();
            sp.setDate(d.format(DATE_FMT));
            sp.setAvgScore(flatScore);
            trends.getScoreByDate().add(sp);
        }
        return trends;
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
