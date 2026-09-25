package com.edumind.ai.api;

/**
 * AI 对话跨模块公开 API（供 statistics 等模块轻量调用）
 */
public interface AiChatApi {

    String chat(String scene, String systemPrompt, String userPrompt);

    /**
     * 带课程归属的对话调用。
     *
     * <p>AI 审计流水（ai_call_log）按课程维度汇总与展示，只有把「发起这次调用的课程」带下来，
     * 这条记录才会归属到该课程、出现在对应课程的 AI 消耗视图里。
     * 课程级调用（学情诊断、教学干预、错题归因等）必须使用本重载；
     * 确实没有课程上下文的平台级调用才允许传 {@code null}，此时记录不归属任何课程。</p>
     *
     * @param courseId 发起调用的课程 ID，可为 null（表示无课程归属）
     */
    String chat(String scene, Long courseId, String systemPrompt, String userPrompt);
}
