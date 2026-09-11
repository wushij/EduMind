package com.edumind.question.dto.question;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionUpdateDTO {
    private Long bankId;
    private Long courseId;
    private Long knowledgePointId;

    @NotBlank(message = "题干不能为空")
    private String stem;

    @NotBlank(message = "题目类型不能为空")
    private String type;

    private String options;

    @NotBlank(message = "正确答案不能为空")
    private String answer;

    private String analysis;
    private Integer difficulty;
    private Integer score;
    private Integer status;
}
