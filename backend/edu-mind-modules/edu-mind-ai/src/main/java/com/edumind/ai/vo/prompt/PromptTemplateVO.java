package com.edumind.ai.vo.prompt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromptTemplateVO {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String description;
    private String systemPrompt;
    private String status;
    private Integer version;
    private String content;
    private String variables;
    private String boundModel;
    private java.math.BigDecimal temperature;
    private Integer maxTokens;
    private LocalDateTime updateTime;
}
