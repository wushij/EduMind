package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemoryFeedbackDTO {
    @NotBlank(message = "操作类型不能为空")
    private String feedbackAction; // FORGET / MODIFY
    private String correctContent;
    private String reason;
}
