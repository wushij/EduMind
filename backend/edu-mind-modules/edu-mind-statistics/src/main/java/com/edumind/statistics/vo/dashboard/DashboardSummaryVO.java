package com.edumind.statistics.vo.dashboard;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSummaryVO {
    private Long courseCount;
    private Long questionCount;
    private Long examCount;
    private Long aiConversationCount;
    private Long assignmentCount;
    private Long pendingGradingCount;
    private Long pendingAssignmentCount;
    private List<RecentCourseVO> recentCourses;
}
