package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningPathVO {
    private Long courseId;
    private String title;
    private List<LearningPathWeekVO> weeks = new ArrayList<>();

    @Data
    public static class LearningPathWeekVO {
        private Integer weekNo;
        private String theme;
        private Long knowledgePointId;
        private Double masteryPercent;
        private String focusReason;
        private List<LearningPathTaskVO> tasks = new ArrayList<>();
    }

    @Data
    public static class LearningPathTaskVO {
        private String id;
        private String title;
        private String type;
        private String typeLabel;
        private Long refId;
        private Long knowledgePointId;
        private String status;
        private Integer estimatedMinutes;
        private String targetUrl;
        private String actionLabel;
    }
}
