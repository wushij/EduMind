package com.edumind.question.dto.bank;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionBankUpdateDTO {
    @NotBlank(message = "题库名称不能为空")
    private String name;
    private Long courseId;
    private String description;
    private Integer status;
}
