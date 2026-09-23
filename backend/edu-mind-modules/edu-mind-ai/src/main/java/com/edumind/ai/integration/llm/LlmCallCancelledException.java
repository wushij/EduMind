package com.edumind.ai.integration.llm;

/**
 * 大模型调用被用户主动中止。
 *
 * <p>该异常表示「用户要求停止」，而不是「模型不可用」，因此网关层必须原样向上抛出：
 * 既不能触发重试，也不能走 fallback 模型 —— 否则用户点了中止，后台反而又发一次请求。</p>
 */
public class LlmCallCancelledException extends RuntimeException {

    public LlmCallCancelledException(String message) {
        super(message);
    }
}
