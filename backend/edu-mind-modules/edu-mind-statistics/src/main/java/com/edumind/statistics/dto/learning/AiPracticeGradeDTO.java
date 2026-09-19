package com.edumind.statistics.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiPracticeGradeDTO {
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    private String studentAnswer;
}
