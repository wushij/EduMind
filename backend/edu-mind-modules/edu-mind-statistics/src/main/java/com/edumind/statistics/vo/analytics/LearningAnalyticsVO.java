package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningAnalyticsVO {
    private Long courseId;
    private Integer studentCount;
    private Double completionRate;
    private Double avgScore;
    private Integer avgStudyMinutes;
    private Double knowledgeMasteryAvg;
    private Integer aiUsageCount;
    private Boolean aggregated;
    private TrendData trends = new TrendData();

    @Data
    public static class TrendData {
        private List<TrendPoint> learning = new ArrayList<>();
        private List<ScoreTrendPoint> score = new ArrayList<>();
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer activeUsers;
    }

    @Data
    public static class ScoreTrendPoint {
        private String date;
        private Double avgScore;
    }
}
