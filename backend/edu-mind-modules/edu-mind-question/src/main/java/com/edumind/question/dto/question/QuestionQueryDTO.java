package com.edumind.question.dto.question;

import lombok.Data;

@Data
public class QuestionQueryDTO {
    private Long courseId;
    private String type;
    private Integer difficulty;
    private String keyword;
    private Long page = 1L;
    private Long pageSize = 10L;
}
