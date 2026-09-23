package com.edumind.ai.integration.llm;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);

    default String chat(String systemPrompt, String userPrompt, LlmChatOptions options) {
        return chat(systemPrompt, userPrompt);
    }

    /**
     * 可取消的对话调用。
     *
     * <p>实现方需在流式读取过程中轮询 {@code cancelled}，一旦置位立即停止读取并关闭响应流，
     * 从而真正断开与上游的连接、停止继续计费；无法支持取消的实现退化为普通调用。</p>
     *
     * @throws LlmCallCancelledException 调用已被中止（不应被当作模型故障重试或 fallback）
     */
    default String chat(String systemPrompt, String userPrompt, LlmChatOptions options,
                        BooleanSupplier cancelled) {
        return chat(systemPrompt, userPrompt, options);
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
