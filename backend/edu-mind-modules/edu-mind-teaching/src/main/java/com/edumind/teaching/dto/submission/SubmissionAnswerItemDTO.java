package com.edumind.teaching.dto.submission;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionAnswerItemDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    private String answer;
}
