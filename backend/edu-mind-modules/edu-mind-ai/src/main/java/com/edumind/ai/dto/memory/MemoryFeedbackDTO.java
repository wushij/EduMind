package com.edumind.ai.dto.memory;

import lombok.Data;

@Data
public class MemoryFeedbackDTO {
    private String feedbackAction; // FORGET / MODIFY
    private String correctContent;
    private String reason;
    private Integer relevanceScore;
}
