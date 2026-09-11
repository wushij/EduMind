package com.edumind.ai.integration.llm;

import java.util.Map;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);

    String generateQuestions(String prompt, Map<String, Object> params);

    void streamChat(String systemPrompt, String userPrompt, StreamCallback callback);

    @FunctionalInterface
    interface StreamCallback {
        void onChunk(String content);

        default void onComplete() {
        }

        default void onError(String message) {
        }
    }
}
