package com.edumind.question.dto.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchCreateDTO {
    private Long courseId;

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    private List<QuestionCreateDTO> questions;
}
