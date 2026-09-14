package com.edumind.ai.integration.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 兼容多轮对话消息（role: system 由 LlmClient 单独传入，此处仅 user/assistant）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatMessage {

    private String role;
    private String content;

    public static LlmChatMessage user(String content) {
        return new LlmChatMessage("user", content);
    }

    public static LlmChatMessage assistant(String content) {
        return new LlmChatMessage("assistant", content);
    }
}
