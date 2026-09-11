package com.edumind.ai.dto;

import lombok.Data;

@Data
public class SubjectiveGradingDTO {
    private Long questionId;
    private String questionStem;
    private String referenceAnswer;
    private String studentAnswer;
    private Integer maxScore;
}
