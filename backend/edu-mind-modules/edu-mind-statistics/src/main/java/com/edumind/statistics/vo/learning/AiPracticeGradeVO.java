package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class AiPracticeGradeVO {
    private Long questionId;
    private Boolean correct;
    private Integer score;
    private Integer maxScore;
    private String referenceAnswer;
    private String analysis;
    private String knowledgePointName;
}
