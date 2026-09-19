package com.edumind.question.vo.question;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionVO {
    private Long id;
    private Long bankId;
    private Long courseId;
    private Long knowledgePointId;
    private String stem;
    private String type;
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer score;
    private Integer status;
    private String knowledgePointName;
    private String cognitiveLevel;
    private String distractorAnalysis;
    private LocalDateTime createTime;
}
