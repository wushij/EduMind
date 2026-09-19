package com.edumind.statistics.vo.learning;

import com.edumind.statistics.vo.analytics.StudentPortraitVO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class LearningReportVO implements Serializable {

    private Long courseId;
    private String courseName;
    private StudentPortraitVO portrait;
    private LearningReportTrendsVO trends = new LearningReportTrendsVO();
    private List<EnrolledCourseItemVO> enrolledCourses = new ArrayList<>();

    @Data
    public static class EnrolledCourseItemVO implements Serializable {
        private Long courseId;
        private String courseName;
    }

    @Data
    public static class LearningReportTrendsVO implements Serializable {
        private List<DailyStudyMinutesVO> studyMinutesByDate = new ArrayList<>();
        private List<DailyScoreVO> scoreByDate = new ArrayList<>();
    }

    @Data
    public static class DailyStudyMinutesVO implements Serializable {
        private String date;
        private Integer minutes;
    }

    @Data
    public static class DailyScoreVO implements Serializable {
        private String date;
        private Double avgScore;
    }
}
