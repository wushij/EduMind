package com.edumind.ai.integration.llm;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);

    default String chat(String systemPrompt, String userPrompt, LlmChatOptions options) {
        return chat(systemPrompt, userPrompt);
    }

    default String chat(String systemPrompt, List<LlmChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return chat(systemPrompt, "");
        }
        LlmChatMessage last = messages.get(messages.size() - 1);
        if (messages.size() == 1) {
            return chat(systemPrompt, last.getContent() != null ? last.getContent() : "");
        }
        return chatWithHistory(systemPrompt, messages);
    }

    default String chatWithHistory(String systemPrompt, List<LlmChatMessage> messages) {
        LlmChatMessage last = messages.get(messages.size() - 1);
        return chat(systemPrompt, last.getContent() != null ? last.getContent() : "");
    }

    String generateQuestions(String prompt, Map<String, Object> params);

    void streamChat(String systemPrompt, String userPrompt, StreamCallback callback);

    default void streamChat(String systemPrompt, List<LlmChatMessage> messages, StreamCallback callback) {
        if (messages == null || messages.isEmpty()) {
            streamChat(systemPrompt, "", callback);
            return;
        }
        if (messages.size() == 1) {
            LlmChatMessage only = messages.get(0);
            streamChat(systemPrompt, only.getContent() != null ? only.getContent() : "", callback);
            return;
        }
        streamChatWithHistory(systemPrompt, messages, callback);
    }

    void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages, StreamCallback callback);

    default void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages,
                                       BooleanSupplier cancelled, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, messages, callback);
    }

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
