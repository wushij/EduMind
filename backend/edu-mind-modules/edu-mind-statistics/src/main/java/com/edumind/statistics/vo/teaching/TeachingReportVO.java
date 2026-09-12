package com.edumind.statistics.vo.teaching;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeachingReportVO {
    private Long courseId;
    private String range;
    private Integer totalChapters;
    private Integer recommendedQuestions;
    private Integer recommendedResources;
    private Integer aiCallCount;
    private Double avgSubmissionRate;
    private Double knowledgeMasteryAvg;
    private List<WeakPointVO> weakPoints = new ArrayList<>();
    private List<WeeklyActivityVO> weeklyActivity = new ArrayList<>();
    private List<ErrorCategoryVO> errorCategories = new ArrayList<>();

    @Data
    public static class WeeklyActivityVO {
        private String date;
        private Integer count;
    }

    @Data
    public static class ErrorCategoryVO {
        private String type;
        private String name;
        private Integer percent;
    }

    @Data
    public static class WeakPointVO {
        private String title;
        private Integer wrongCount;
        private String suggestion;
    }
}
