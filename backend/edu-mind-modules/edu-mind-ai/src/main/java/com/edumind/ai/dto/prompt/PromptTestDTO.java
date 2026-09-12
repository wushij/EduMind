package com.edumind.ai.dto.prompt;

import lombok.Data;

import java.util.Map;

@Data
public class PromptTestDTO {
    private Map<String, String> variables;
    private String modelKey;
}
