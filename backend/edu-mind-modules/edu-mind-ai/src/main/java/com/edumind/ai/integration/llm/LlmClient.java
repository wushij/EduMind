package com.edumind.ai.integration.llm;

import java.util.Map;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);

    String generateQuestions(String prompt, Map<String, Object> params);

    void streamChat(String systemPrompt, String userPrompt, StreamCallback callback);

    interface StreamCallback {
        /** 正文 token */
        void onChunk(String content);

        /** 思考链 token（DeepSeek reasoning_content） */
        default void onReasoning(String content) {
        }

        /** 阶段状态（reasoning / composing） */
        default void onStatus(String phase, String message) {
        }

        default void onComplete() {
        }

        default void onError(String message) {
        }
    }
}
