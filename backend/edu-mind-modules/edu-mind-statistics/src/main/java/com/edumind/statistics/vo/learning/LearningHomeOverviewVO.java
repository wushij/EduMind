package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningHomeOverviewVO {

    private Long primaryCourseId;
    private SummaryVO summary = new SummaryVO();
    private List<CourseItemVO> courses = new ArrayList<>();
    private List<WeakPointItemVO> weakPoints = new ArrayList<>();
    private List<TodayTaskVO> todayTasks = new ArrayList<>();

    @Data
    public static class SummaryVO {
        private Integer avgCourseProgressPercent;
        private Integer totalStudyMinutes;
        private Integer completedTasks;
        private Integer totalTasks;
        private Double overallMasteryPercent;
    }

    @Data
    public static class CourseItemVO {
        private Long courseId;
        private String courseName;
        private Integer lessonProgressPercent;
        private Double overallMastery;
        private Integer pendingAssignmentCount;
    }

    @Data
    public static class WeakPointItemVO {
        private Long knowledgePointId;
        private String title;
        private Long courseId;
        private String courseName;
        private Double mastery;
        private String suggestion;
        private String level;
    }

    @Data
    public static class TodayTaskVO {
        private String id;
        private String title;
        private Long courseId;
        private String courseName;
        private String type;
        private Integer estimatedMinutes;
        private String status;
        private String targetUrl;
    }
}
