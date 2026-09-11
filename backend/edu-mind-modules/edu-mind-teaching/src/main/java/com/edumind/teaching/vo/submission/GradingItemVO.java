package com.edumind.teaching.vo.submission;

import lombok.Data;

@Data
public class GradingItemVO {
    private Long questionId;
    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private String aiComment;
    private String teacherComment;
    private String status;
}
