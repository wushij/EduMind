package com.edumind.ai.api;

/**
 * AI 对话跨模块公开 API（供 statistics 等模块轻量调用）
 */
public interface AiChatApi {

    String chat(String scene, String systemPrompt, String userPrompt);
}
