package com.edumind.statistics.service.dashboard.impl;

import com.edumind.ai.api.AiQueryApi;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.infrastructure.redis.cache.DashboardCacheService;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.statistics.service.dashboard.DashboardService;
import com.edumind.statistics.vo.dashboard.DashboardSummaryVO;
import com.edumind.statistics.vo.dashboard.RecentCourseVO;
import com.edumind.teaching.api.ExamQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CourseQueryApi courseQueryApi;
    private final QuestionQueryApi questionQueryApi;
    private final ExamQueryApi examQueryApi;
    private final AiQueryApi aiQueryApi;
    private final DashboardCacheService dashboardCacheService;

    @Override
    public DashboardSummaryVO getSummary() {
        Long userId = UserContext.getUserId();
        return dashboardCacheService.getOrLoad(userId, DashboardSummaryVO.class, this::loadSummary);
    }

    private DashboardSummaryVO loadSummary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setCourseCount(courseQueryApi.countCourses());
        vo.setQuestionCount(questionQueryApi.countQuestions());
        vo.setExamCount(examQueryApi.countExams());
        vo.setAiConversationCount(aiQueryApi.countConversations());
        vo.setAssignmentCount(examQueryApi.countAssignments());
        vo.setPendingGradingCount(examQueryApi.countPendingGrading());
        vo.setPendingAssignmentCount(examQueryApi.countPendingAssignments());
        vo.setRecentCourses(mapRecentCourses(courseQueryApi.listRecentCourses(5)));
        return vo;
    }

    private List<RecentCourseVO> mapRecentCourses(List<CourseBriefVO> courses) {
        return courses.stream().map(course -> {
            RecentCourseVO recent = new RecentCourseVO();
            recent.setId(course.getId());
            recent.setName(course.getTitle());
            recent.setLastVisitAt(course.getLastVisitAt());
            return recent;
        }).collect(Collectors.toList());
    }
}
