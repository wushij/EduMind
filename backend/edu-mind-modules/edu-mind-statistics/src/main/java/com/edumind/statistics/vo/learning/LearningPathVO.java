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
        private List<LearningPathTaskVO> tasks = new ArrayList<>();
    }

    @Data
    public static class LearningPathTaskVO {
        private String title;
        private String type;
        private Long refId;
        private String status;
    }
}
