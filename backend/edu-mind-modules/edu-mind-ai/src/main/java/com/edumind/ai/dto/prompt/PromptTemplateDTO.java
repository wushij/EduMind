package com.edumind.ai.dto.prompt;

import lombok.Data;

@Data
public class PromptTemplateDTO {
    private String code;
    private String name;
    private String category;
    private String description;
    private String systemPrompt;
    private String content;
    private String variables;
    private String boundModel;
    private java.math.BigDecimal temperature;
    private Integer maxTokens;
}
