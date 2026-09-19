package com.edumind.teaching.vo.submission;

import lombok.Data;

@Data
public class GradingItemVO {
    private Long questionId;
    private String type;
    private String stem;
    private String standardAnswer;
    private String analysis;
    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private String aiComment;
    private String teacherComment;
    private String status;
}
