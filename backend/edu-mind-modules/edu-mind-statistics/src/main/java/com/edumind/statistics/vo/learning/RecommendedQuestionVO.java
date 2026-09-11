package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class RecommendedQuestionVO {
    private Long id;
    private String stem;
    private String type;
    private Integer difficulty;
    private Long courseId;
}
