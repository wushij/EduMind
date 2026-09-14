package com.edumind.ai.integration.llm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmChatOptions {
    Double temperature;
    Integer maxTokens;
    Boolean disableThinking;

    public static LlmChatOptions empty() {
        return LlmChatOptions.builder().build();
    }

    public static LlmChatOptions of(Double temperature, Integer maxTokens) {
        return LlmChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    public static LlmChatOptions forQueryRewrite(Double temperature, Integer maxTokens) {
        return LlmChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .disableThinking(true)
                .build();
    }
}
