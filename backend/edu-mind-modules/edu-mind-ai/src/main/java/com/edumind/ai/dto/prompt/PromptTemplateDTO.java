package com.edumind.ai.dto.prompt;

import lombok.Data;

@Data
public class PromptTemplateDTO {
    private String code;
    private String name;
    private String category;
    private String content;
    private String variables;
}
