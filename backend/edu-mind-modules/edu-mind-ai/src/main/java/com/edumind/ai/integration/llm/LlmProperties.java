package com.edumind.ai.integration.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {
    private String provider = "mock";
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private Integer timeoutMs = 180000;
    private Boolean mockEnabled = true;
    private Boolean streamEnabled = true;
    private Double temperature = 0.7;
    private String reasoningEffort = "low";
    private Integer maxTokens = 8192;
}
